import java.sql.*;
import java.util.*;

/**
 * H2 데이터베이스에서 MySQL로 데이터 마이그레이션 유틸리티
 * 사용법: java H2ToMySQLMigration
 * 
 * H2 드라이버: org.h2.Driver
 * MySQL 드라이버: com.mysql.cj.jdbc.Driver
 */
public class H2ToMySQLMigration {
    
    // H2 데이터베이스 설정
    private static final String H2_URL = "jdbc:h2:~/local";
    private static final String H2_DRIVER = "org.h2.Driver";
    private static final String H2_USER = "sa";
    private static final String H2_PASSWORD = "";
    
    // MySQL 설정
    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/testdb?serverTimezone=Asia/Seoul&characterEncoding=UTF-8";
    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String MYSQL_USER = "root";
    private static final String MYSQL_PASSWORD = "1234";
    
    public static void main(String[] args) {
        try {
            System.out.println("=== H2 to MySQL 데이터 마이그레이션 시작 ===\n");
            
            // JDBC 드라이버 로드
            Class.forName(H2_DRIVER);
            Class.forName(MYSQL_DRIVER);
            
            // H2와 MySQL 연결
            Connection h2Conn = DriverManager.getConnection(H2_URL, H2_USER, H2_PASSWORD);
            Connection mysqlConn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
            
            System.out.println("✓ H2 데이터베이스 연결 성공");
            System.out.println("✓ MySQL 데이터베이스 연결 성공\n");
            
            // H2의 모든 테이블 조회
            List<String> tables = getTableNames(h2Conn);
            
            if (tables.isEmpty()) {
                System.out.println("H2 데이터베이스에 테이블이 없습니다.");
                h2Conn.close();
                mysqlConn.close();
                return;
            }
            
            System.out.println("발견된 테이블: " + tables);
            System.out.println();
            
            // 각 테이블별 데이터 마이그레이션
            for (String tableName : tables) {
                migrateTable(h2Conn, mysqlConn, tableName);
            }
            
            System.out.println("\n=== 마이그레이션 완료! ===");
            
            h2Conn.close();
            mysqlConn.close();
            
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC 드라이버를 찾을 수 없습니다:");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("데이터베이스 오류:");
            e.printStackTrace();
        }
    }
    
    /**
     * H2 데이터베이스의 모든 테이블명 조회
     */
    private static List<String> getTableNames(Connection conn) throws SQLException {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData metaData = conn.getMetaData();
        
        try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                tables.add(tableName);
            }
        }
        
        return tables;
    }
    
    /**
     * 특정 테이블을 H2에서 MySQL로 마이그레이션
     */
    private static void migrateTable(Connection h2Conn, Connection mysqlConn, String tableName) 
            throws SQLException {
        
        System.out.println("마이그레이션 중: " + tableName);
        
        try {
            // H2에서 데이터 조회
            String selectQuery = "SELECT * FROM " + tableName;
            Statement h2Stmt = h2Conn.createStatement();
            ResultSet rs = h2Stmt.executeQuery(selectQuery);
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnName(i));
            }
            
            // MySQL에 데이터 삽입
            int count = 0;
            while (rs.next()) {
                insertRow(mysqlConn, tableName, columnNames, rs, metaData);
                count++;
                
                if (count % 100 == 0) {
                    System.out.println("  → " + count + "개 행 처리됨");
                }
            }
            
            System.out.println("  ✓ " + count + "개 행 마이그레이션 완료\n");
            
            rs.close();
            h2Stmt.close();
            
        } catch (SQLException e) {
            System.err.println("  ✗ 테이블 마이그레이션 실패: " + e.getMessage());
        }
    }
    
    /**
     * 한 행의 데이터를 MySQL에 삽입
     */
    private static void insertRow(Connection mysqlConn, String tableName, List<String> columnNames, 
                                   ResultSet sourceRs, ResultSetMetaData metaData) throws SQLException {
        
        StringBuilder columnList = new StringBuilder();
        StringBuilder values = new StringBuilder();
        List<Object> valueList = new ArrayList<>();
        
        for (int i = 0; i < columnNames.size(); i++) {
            if (i > 0) {
                columnList.append(", ");
                values.append(", ");
            }
            
            columnList.append("`").append(columnNames.get(i)).append("`");
            values.append("?");
            
            Object value = sourceRs.getObject(i + 1);
            valueList.add(value);
        }
        
        String insertQuery = "INSERT INTO `" + tableName + "` (" + columnList + ") VALUES (" + values + ")";
        
        try (PreparedStatement pstmt = mysqlConn.prepareStatement(insertQuery)) {
            for (int i = 0; i < valueList.size(); i++) {
                pstmt.setObject(i + 1, valueList.get(i));
            }
            pstmt.executeUpdate();
        }
    }
}
