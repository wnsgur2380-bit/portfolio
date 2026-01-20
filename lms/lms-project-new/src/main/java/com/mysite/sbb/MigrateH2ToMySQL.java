package com.mysite.sbb;

import java.sql.*;
import java.util.*;

public class MigrateH2ToMySQL {
    
    private static final String H2_DRIVER = "org.h2.Driver";
    private static final String H2_URL = "jdbc:h2:~/local";
    private static final String H2_USER = "sa";
    private static final String H2_PASSWORD = "";
    
    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/testdb?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowMultiQueries=true";
    private static final String MYSQL_USER = "root";
    private static final String MYSQL_PASSWORD = "1234";
    
    public static void main(String[] args) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("   H2 → MySQL 데이터 마이그레이션 도구");
        System.out.println("=".repeat(60) + "\n");
        
        Connection h2Conn = null;
        Connection mysqlConn = null;
        
        try {
            System.out.println("[1/6] JDBC 드라이버 로드 중...");
            Class.forName(H2_DRIVER);
            Class.forName(MYSQL_DRIVER);
            System.out.println("    ✓ H2 드라이버 로드 성공");
            System.out.println("    ✓ MySQL 드라이버 로드 성공\n");
            
            System.out.println("[2/6] 데이터베이스 연결 중...");
            h2Conn = DriverManager.getConnection(H2_URL, H2_USER, H2_PASSWORD);
            System.out.println("    ✓ H2 연결 성공");
            
            mysqlConn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
            System.out.println("    ✓ MySQL 연결 성공\n");
            
            System.out.println("[3/6] H2 테이블 목록 조회 중...");
            List<String> tables = getTableNames(h2Conn);
            
            if (tables.isEmpty()) {
                System.out.println("    ⚠ 테이블이 없습니다.\n");
                return;
            }
            
            System.out.println("    발견된 테이블 (" + tables.size() + "개): ");
            for (String table : tables) {
                System.out.println("      - " + table);
            }
            System.out.println();
            
            System.out.println("[4/6] MySQL 기존 테이블 삭제 중...\n");
            dropTablesInMySQL(mysqlConn, tables);
            System.out.println();
            
            System.out.println("[5/6] MySQL에 테이블 구조 생성 중...\n");
            for (String tableName : tables) {
                createTableInMySQL(h2Conn, mysqlConn, tableName);
            }
            System.out.println();
            
            System.out.println("[6/6] 데이터 마이그레이션 중...\n");
            int totalRows = 0;
            
            for (String tableName : tables) {
                int rowCount = migrateTable(h2Conn, mysqlConn, tableName);
                totalRows += rowCount;
            }
            
            System.out.println();
            System.out.println("=".repeat(60));
            System.out.println("결과: " + tables.size() + "개 테이블, " + totalRows + "개 행 마이그레이션됨");
            System.out.println("=".repeat(60) + "\n");
            
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("✗ 오류: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (h2Conn != null) h2Conn.close();
                if (mysqlConn != null) mysqlConn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    private static List<String> getTableNames(Connection conn) throws SQLException {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData metaData = conn.getMetaData();
        
        try (ResultSet rs = metaData.getTables(null, "PUBLIC", "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                if (!tableName.startsWith("INFORMATION_SCHEMA") && !tableName.startsWith("PG_")) {
                    tables.add(tableName);
                }
            }
        }
        
        Collections.sort(tables);
        return tables;
    }
    
    private static void dropTablesInMySQL(Connection mysqlConn, List<String> tables) throws SQLException {
        for (String tableName : tables) {
            System.out.print("  " + tableName + " 삭제 중... ");
            try (Statement stmt = mysqlConn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS `" + tableName + "`");
                System.out.println("✓");
            } catch (SQLException e) {
                System.out.println("(무시됨)");
            }
        }
    }
    
    private static void createTableInMySQL(Connection h2Conn, Connection mysqlConn, String tableName) 
            throws SQLException {
        
        System.out.print("  " + tableName + "... ");
        
        try {
            DatabaseMetaData h2Meta = h2Conn.getMetaData();
            
            StringBuilder sql = new StringBuilder("CREATE TABLE `" + tableName + "` (\n");
            
            try (ResultSet columns = h2Meta.getColumns(null, null, tableName, null)) {
                List<String> colDefs = new ArrayList<>();
                while (columns.next()) {
                    String colName = columns.getString("COLUMN_NAME");
                    String type = columns.getString("TYPE_NAME");
                    int size = columns.getInt("COLUMN_SIZE");
                    int decimals = columns.getInt("DECIMAL_DIGITS");
                    String nullable = columns.getString("IS_NULLABLE");
                    String autoInc = columns.getString("IS_AUTOINCREMENT");
                    
                    StringBuilder def = new StringBuilder("`" + colName + "` ");
                    def.append(convertType(type, size, decimals));
                    
                    if ("NO".equals(nullable)) def.append(" NOT NULL");
                    if ("YES".equals(autoInc)) def.append(" AUTO_INCREMENT");
                    
                    colDefs.add(def.toString());
                }
                
                for (int i = 0; i < colDefs.size(); i++) {
                    sql.append("  ").append(colDefs.get(i));
                    if (i < colDefs.size() - 1) sql.append(",");
                    sql.append("\n");
                }
            }
            
            try (ResultSet pk = h2Meta.getPrimaryKeys(null, null, tableName)) {
                List<String> pkCols = new ArrayList<>();
                while (pk.next()) {
                    pkCols.add(pk.getString("COLUMN_NAME"));
                }
                if (!pkCols.isEmpty()) {
                    sql.append(",\n  PRIMARY KEY (");
                    for (int i = 0; i < pkCols.size(); i++) {
                        if (i > 0) sql.append(", ");
                        sql.append("`").append(pkCols.get(i)).append("`");
                    }
                    sql.append(")\n");
                }
            }
            
            sql.append(")");
            
            try (Statement stmt = mysqlConn.createStatement()) {
                stmt.execute(sql.toString());
                System.out.println("✓");
            }
            
        } catch (SQLException e) {
            System.out.println("✗ (" + e.getMessage() + ")");
        }
    }
    
    private static String convertType(String h2Type, int size, int decimals) {
        String type = h2Type.toUpperCase().trim();
        
        if (type.contains("CHARACTER VARYING") || type.contains("VARCHAR")) {
            // 큰 VARCHAR는 TEXT로 변환 (행 크기 문제 피하기)
            if (size <= 0 || size > 5000) return "LONGTEXT";
            if (size > 1000) return "TEXT";
            return "VARCHAR(" + size + ")";
        }
        if (type.contains("CHAR")) return "CHAR(" + (size > 0 && size <= 255 ? size : 255) + ")";
        if (type.contains("TEXT") || type.contains("CLOB")) return "LONGTEXT";
        if (type.contains("BLOB")) return "LONGBLOB";
        if (type.contains("DECIMAL") || type.contains("NUMERIC")) return "DECIMAL(" + (size > 0 ? size : 10) + "," + (decimals > 0 ? decimals : 0) + ")";
        if (type.contains("BIGINT") || type.contains("LONG")) return "BIGINT";
        if (type.contains("INTEGER")) return "INT";
        if (type.contains("SMALLINT")) return "SMALLINT";
        if (type.contains("TINYINT")) return "TINYINT";
        if (type.contains("DOUBLE")) return "DOUBLE";
        if (type.contains("FLOAT")) return "FLOAT";
        if (type.contains("BOOLEAN") || type.contains("BIT")) return "BOOLEAN";
        if (type.contains("DATE")) return "DATE";
        if (type.contains("TIME")) return "DATETIME";
        
        return "LONGTEXT";
    }
    
    private static int migrateTable(Connection h2Conn, Connection mysqlConn, String tableName) 
            throws SQLException {
        
        System.out.print("  " + tableName + "... ");
        
        int rowCount = 0;
        try (Statement stmt = h2Conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName);
            if (rs.next()) rowCount = rs.getInt(1);
            rs.close();
        }
        
        if (rowCount == 0) {
            System.out.println("(행 없음)");
            return 0;
        }
        
        try {
            String query = "SELECT * FROM " + tableName;
            Statement h2Stmt = h2Conn.createStatement();
            ResultSet rs = h2Stmt.executeQuery(query);
            
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            List<String> colNames = new ArrayList<>();
            for (int i = 1; i <= cols; i++) {
                colNames.add(meta.getColumnName(i));
            }
            
            int inserted = 0;
            while (rs.next()) {
                inserted += insertRow(mysqlConn, tableName, colNames, rs) ? 1 : 0;
            }
            
            rs.close();
            h2Stmt.close();
            
            System.out.println("✓ (" + inserted + "개 행)");
            return inserted;
            
        } catch (SQLException e) {
            System.out.println("✗");
            return 0;
        }
    }
    
    private static boolean insertRow(Connection mysqlConn, String tableName, List<String> colNames, 
                                      ResultSet rs) throws SQLException {
        StringBuilder cols = new StringBuilder();
        StringBuilder vals = new StringBuilder();
        List<Object> values = new ArrayList<>();
        
        for (int i = 0; i < colNames.size(); i++) {
            if (i > 0) {
                cols.append(", ");
                vals.append(", ");
            }
            cols.append("`").append(colNames.get(i)).append("`");
            vals.append("?");
            values.add(rs.getObject(i + 1));
        }
        
        String sql = "INSERT INTO `" + tableName + "` (" + cols + ") VALUES (" + vals + ")";
        
        try (PreparedStatement pstmt = mysqlConn.prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) {
                pstmt.setObject(i + 1, values.get(i));
            }
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (!e.getMessage().contains("Duplicate")) throw e;
            return false;
        }
    }
}
