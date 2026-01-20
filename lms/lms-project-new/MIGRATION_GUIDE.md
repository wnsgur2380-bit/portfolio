# H2에서 MySQL로 데이터 마이그레이션 가이드

## 📋 준비 사항

1. **MySQL 서버 실행 확인**
   ```bash
   mysql -u root -p
   # 또는 MySQL이 설치되어 있으면 자동으로 실행됨
   ```

2. **MySQL 데이터베이스 확인**
   ```sql
   -- MySQL에서 실행
   CREATE DATABASE testdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   -- 또는 이미 존재하면 확인만 하기
   SHOW DATABASES;
   ```

3. **H2 데이터 파일 확인**
   ```bash
   # 다음 파일이 사용자 홈 디렉토리에 있는지 확인
   ls -la ~/local*
   # 출력: local.mv.db, local.trace.db 등
   ```

## 🚀 마이그레이션 실행 방법

### 방법 1: IDE에서 실행 (권장)

1. Spring Tool Suite 4에서 프로젝트를 열기
2. 좌측 Package Explorer에서 `src/main/java/com/mysite/sbb/MigrateH2ToMySQL.java` 찾기
3. 파일을 우클릭 → **Run As** → **Java Application** 선택
4. Console 창에서 마이그레이션 진행 상황 확인

### 방법 2: 터미널에서 실행

```bash
cd /home/hkit/Documents/workspace-spring-tool-suite-4-4.21.0.RELEASE/lms-project

# 1. 프로젝트 빌드
./gradlew build

# 2. 마이그레이션 프로그램 실행
java -cp build/classes/java/main:build/resources/main \
     -Dloader.path=build/libs \
     com.mysite.sbb.MigrateH2ToMySQL
```

### 방법 3: Gradle 태스크로 실행

```bash
cd /home/hkit/Documents/workspace-spring-tool-suite-4-4.21.0.RELEASE/lms-project

# build.gradle에 다음을 추가한 후:
gradle run -PmainClass=com.mysite.sbb.MigrateH2ToMySQL
```

## 📊 마이그레이션 결과 확인

### MySQL에서 데이터 확인

```bash
# MySQL 접속
mysql -u root -p testdb

# 테이블 목록 확인
SHOW TABLES;

# 특정 테이블의 행 수 확인
SELECT COUNT(*) FROM table_name;

# 특정 테이블의 데이터 확인
SELECT * FROM table_name LIMIT 5;
```

### Spring Boot 애플리케이션에서 확인

1. IDE에서 LmsProjectApplication.java를 우클릭
2. **Run As** → **Spring Boot App** 선택
3. 애플리케이션이 시작되고 MySQL 데이터가 로드됨

## ⚙️ 설정 파일 확인

**application.yml** 에서 다음이 설정되어 있는지 확인:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/testdb?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: 1234  # 자신의 MySQL 비밀번호로 변경
  
  jpa:
    hibernate:
      ddl-auto: update  # 데이터 스키마 자동 생성
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
```

## 🔧 문제 해결

### "드라이버를 찾을 수 없습니다" 오류
```
원인: JDBC 드라이버가 build되지 않음
해결: ./gradlew clean build --refresh-dependencies
```

### "MySQL 연결 실패" 오류
```
원인: MySQL 서버가 실행 중이지 않거나 설정이 잘못됨
확인사항:
- MySQL이 실행 중인지 확인: mysql -u root -p
- testdb 데이터베이스 존재 확인
- 포트 3306이 사용 중인지 확인: netstat -an | grep 3306
```

### "H2 데이터베이스에 테이블이 없습니다" 오류
```
원인: local.mv.db 파일이 없거나 데이터가 없음
확인사항:
- ~/local.mv.db 파일이 존재하는지 확인: ls -la ~/local*
- H2 콘솔에서 데이터 확인
```

## 📝 추가 정보

- **H2 파일 위치**: `~/local.mv.db` (사용자 홈 디렉토리)
- **MySQL 기본 포트**: 3306
- **MySQL 기본 사용자**: root
- **데이터베이스명**: testdb

## ✅ 마이그레이션 완료 후

1. 기존 H2 데이터 백업 (선택사항)
   ```bash
   cp ~/local.mv.db ~/local.mv.db.backup
   ```

2. application.yml에서 H2 관련 설정 제거 (이미 제거됨)

3. 애플리케이션 재시작
   ```bash
   ./gradlew bootRun
   ```

4. MySQL에서 데이터 확인

---

질문이나 문제가 발생하면 오류 메시지와 함께 문의해주세요.
