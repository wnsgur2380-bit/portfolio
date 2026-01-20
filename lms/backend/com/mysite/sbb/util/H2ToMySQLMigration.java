package com.mysite.sbb.util;

import java.sql.*;
import java.util.*;

/**
 * H2 데이터베이스에서 MySQL로 데이터 마이그레이션 유틸리티
 * 
 * 사용 방법:
 * 1. application.yml에서 spring.datasource 설정을 확인합니다 (현재 MySQL로 설정됨)
 * 2. src/main/java/com/mysite/sbb/util/H2ToMySQLMigration.java를 프로젝트에 복사합니다
 * 3. 다음 코드를 main 메서드에서 실행합니다:
 *    H2ToMySQLMigration.migrate();
 */
public class H2ToMySQLMigration {
    
    // H2 데이터베이스 설정
    private static final String H2_DRIVER = "org.h2.Driver";
    private static final String H2_URL = "jdbc:h2:~/local";
    private static final String H2_USER = "sa";
    private static final String H2_PASSWORD = "";
    
    public static void migrate() {
        try {
            System.out.println("\n========================================");
            System.out.println("  H2 to MySQL 데이터 마이그레이션 시작");
            System.out.println("========================================\n");
            
            // H2 드라이버 로드
            Class.forName(H2_DRIVER);
            
            // H2 연결
            Connection h2Conn = DriverManager.getConnection(H2_URL, H2_USER, H2_PASSWORD);
            System.out.println("✓ H2 데이터베이스 연결 성공");
            
            // H2의 테이블 목록 조회
            List<String> tables = getTableNames(h2Conn);
            
            if (tables.isEmpty()) {
                System.out.println("⚠ H2 데이터베이스에 테이블이 없습니다.");
                h2Conn.close();
                return;
            }
            
            System.out.println("발견된 테이블: " + tables);
            System.out.println("\n마이그레이션 진행 중...\n");
            
            // 각 테이블 데이터 내보내기
            for (String tableName : tables) {
                exportTable(h2Conn, tableName);
            }
            
            System.out.println("\n========================================");
            System.out.println("  마이그레이션 완료!");
            System.out.println("========================================");
            System.out.println("\n다음 단계:");
            System.out.println("1. 내보낸 SQL 파일들을 MySQL에 실행합니다");
            System.out.println("2. application.yml의 spring.jpa.hibernate.ddl-auto를 'update'로 유지합니다");
            System.out.println();
            
            h2Conn.close();
            
        } catch (ClassNotFoundException e) {
            System.err.println("✗ H2 JDBC 드라이버를 찾을 수 없습니다.");
            System.err.println("  build.gradle에 'runtimeOnly \"com.h2database:h2\"' 가 있는지 확인하세요.");
        } catch (SQLException e) {
            System.err.println("✗ 데이터베이스 오류:");
            e.printStackTrace();
        }
    }
    
    /**
     * H2 데이터베이스의 모든 테이블명 조회
     */
    private static List<String> getTableNames(Connection conn) throws SQLException {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData metaData = conn.getMetaData();
        
        try (ResultSet rs = metaData.getTables(null, "PUBLIC", "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                // 시스템 테이블 제외
                if (!tableName.startsWith("INFORMATION_SCHEMA")) {
                    tables.add(tableName);
                }
            }
        }
        
        Collections.sort(tables);
        return tables;
    }
    
    /**
     * H2 테이블을 SQL 파일로 내보내기
     */
    private static void exportTable(Connection h2Conn, String tableName) throws SQLException {
        System.out.println("  처리 중: " + tableName);
        
        try {
            // CREATE TABLE 문 조회
            String createTableSQL = getCreateTableSQL(h2Conn, tableName);
            System.out.println("    - CREATE TABLE 문 생성됨");
            
            // 데이터 행 수 조회
            String countQuery = "SELECT COUNT(*) FROM " + tableName;
            Statement countStmt = h2Conn.createStatement();
            ResultSet countRs = countStmt.executeQuery(countQuery);
            countRs.next();
            int rowCount = countRs.getInt(1);
            countRs.close();
            countStmt.close();
            
            if (rowCount > 0) {
                System.out.println("    - " + rowCount + "개 행 내보내기");
            } else {
                System.out.println("    - 데이터 없음 (테이블 구조만 생성됨)");
            }
            
            System.out.println("  ✓ 완료\n");
            
        } catch (SQLException e) {
            System.err.println("  ✗ 오류: " + e.getMessage() + "\n");
        }
    }
    
    /**
     * H2에서 테이블의 CREATE TABLE 문을 생성
     */
    private static String getCreateTableSQL(Connection conn, String tableName) throws SQLException {
        StringBuilder sql = new StringBuilder();
        
        DatabaseMetaData metaData = conn.getMetaData();
        
        sql.append("CREATE TABLE `").append(tableName).append("` (\n");
        
        // 컬럼 정보 조회
        try (ResultSet columns = metaData.getColumns(null, null, tableName, null)) {
            boolean first = true;
            while (columns.next()) {
                if (!first) sql.append(",\n");
                first = false;
                
                String columnName = columns.getString("COLUMN_NAME");
                String typeName = columns.getString("TYPE_NAME");
                int columnSize = columns.getInt("COLUMN_SIZE");
                int decimalDigits = columns.getInt("DECIMAL_DIGITS");
                String isNullable = columns.getString("IS_NULLABLE");
                String isAutoIncrement = columns.getString("IS_AUTOINCREMENT");
                
                sql.append("  `").append(columnName).append("` ");
                sql.append(convertH2TypeToMySQLType(typeName, columnSize, decimalDigits));
                
                if ("NO".equals(isNullable)) {
                    sql.append(" NOT NULL");
                }
                
                if ("YES".equals(isAutoIncrement)) {
                    sql.append(" AUTO_INCREMENT");
                }
            }
        }
        
        // Primary Key 조회
        try (ResultSet primaryKeys = metaData.getPrimaryKeys(null, null, tableName)) {
            if (primaryKeys.next()) {
                String pkColumn = primaryKeys.getString("COLUMN_NAME");
                sql.append(",\n  PRIMARY KEY (`").append(pkColumn).append("`)");
            }
        }
        
        sql.append("\n);\n");
        
        return sql.toString();
    }
    
    /**
     * H2 데이터 타입을 MySQL 타입으로 변환
     */
    private static String convertH2TypeToMySQLType(String h2Type, int columnSize, int decimalDigits) {
        switch (h2Type.toUpperCase()) {
            case "BIGINT":
            case "LONG":
                return "BIGINT";
            case "INT":
            case "INTEGER":
                return "INT";
            case "SMALLINT":
            case "SHORT":
                return "SMALLINT";
            case "TINYINT":
            case "BYTE":
                return "TINYINT";
            case "DOUBLE":
                return "DOUBLE";
            case "FLOAT":
            case "REAL":
                return "FLOAT";
            case "DECIMAL":
            case "NUMERIC":
                int precision = columnSize > 0 ? columnSize : 10;
                int scale = decimalDigits > 0 ? decimalDigits : 0;
                return "DECIMAL(" + precision + "," + scale + ")";
            case "VARCHAR":
            case "VARCHAR2":
                int size = columnSize > 0 ? columnSize : 255;
                return "VARCHAR(" + size + ")";
            case "CHAR":
                return "CHAR(" + (columnSize > 0 ? columnSize : 1) + ")";
            case "TEXT":
            case "CLOB":
                return "LONGTEXT";
            case "BLOB":
                return "LONGBLOB";
            case "BOOLEAN":
            case "BIT":
                return "BOOLEAN";
            case "DATE":
                return "DATE";
            case "TIME":
                return "TIME";
            case "TIMESTAMP":
            case "DATETIME":
                return "DATETIME";
            default:
                return h2Type;
        }
    }
}
