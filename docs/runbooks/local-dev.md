# 로컬 개발 가이드

## 사전 요구사항

### 필수 소프트웨어
- **Docker Desktop**: 최신 버전
- **Java 17+**: OpenJDK 또는 Oracle JDK
- **Gradle 8+**: 빌드 도구 (wrapper 포함)
- **Git**: 버전 관리

### 선택 도구
- **IntelliJ IDEA**: 권장 IDE
- **Postman/Insomnia**: API 테스팅
- **DBeaver**: 데이터베이스 GUI 클라이언트

## 빠른 시작

### 1. 저장소 복제
```bash
git clone <repository-url>
cd GgUd
```

### 2. 환경 설정
프로젝트 루트에 `.env` 파일 생성:
```bash
DB_PASSWORD=your_secure_password
KAKAO_CLIENT_ID=your_kakao_app_id
KAKAO_CLIENT_SECRET=your_kakao_secret
JWT_SECRET=your_jwt_secret_key_min_32_chars
```

### 3. 서비스 시작
```bash
# PostgreSQL과 Redis 시작
docker-compose up -d

# 서비스 실행 확인
docker-compose ps

# 로그 확인
docker-compose logs -f
```

### 4. 애플리케이션 빌드 및 실행
```bash
# 프로젝트 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# 애플리케이션 접속: http://localhost:8080
```

### 5. 설정 확인
```bash
# 헬스 체크
curl http://localhost:8080/actuator/health

# Swagger UI
open http://localhost:8080/swagger-ui.html
```

---

## Docker 운영

### 서비스 관리

**모든 서비스 시작**:
```bash
docker-compose up -d
```

**모든 서비스 중지**:
```bash
docker-compose down
```

**특정 서비스 재시작**:
```bash
docker-compose restart db
docker-compose restart redis
```

**서비스 상태 확인**:
```bash
docker-compose ps
```

**모든 컨테이너 및 볼륨 제거**:
```bash
docker-compose down -v
```

### 데이터베이스 접근

**PostgreSQL 접속**:
```bash
# psql 명령어 사용
docker exec -it ggud-postgres psql -U postgres -d ggud_db

# 일반적인 쿼리
\dt                      # 테이블 목록
\d table_name           # 테이블 구조 확인
SELECT * FROM users;    # 데이터 조회
```

**데이터베이스 GUI 연결**:
- **Host**: localhost
- **Port**: 5432
- **Database**: ggud_db
- **Username**: postgres
- **Password**: (.env 파일에서 확인)

**Redis 접속**:
```bash
# Redis CLI
docker exec -it ggud-redis redis-cli

# 일반적인 명령어
KEYS *                  # 모든 키 목록
GET key_name           # 값 가져오기
FLUSHALL               # 모든 데이터 삭제 (주의!)
```

### 로그 및 모니터링

**애플리케이션 로그 확인**:
```bash
# 로그 실시간 확인
docker-compose logs -f app

# 최근 100줄
docker-compose logs --tail=100 app

# 특정 시간 범위
docker-compose logs --since=10m app
```

**모든 서비스 로그 확인**:
```bash
docker-compose logs -f
```

**데이터베이스 로그**:
```bash
docker-compose logs -f db
```

**Redis 로그**:
```bash
docker-compose logs -f redis
```

---

## 문제 해결

### 포트 충돌

**증상**: "Port already in use" 오류

**해결방법**:
```bash
# 포트를 사용 중인 프로세스 확인
lsof -i :8080   # 애플리케이션 포트
lsof -i :5432   # PostgreSQL 포트
lsof -i :6379   # Redis 포트

# 해당 포트를 사용하는 프로세스 종료
kill -9 <PID>

# 또는 application.yml에서 포트 변경
server:
  port: 8081
```

### 데이터베이스 연결 실패

**증상**: "Connection refused" 또는 "Authentication failed"

**확인사항**:
```bash
# PostgreSQL 실행 확인
docker-compose ps db

# 오류 로그 확인
docker-compose logs db

# 연결 테스트
docker exec -it ggud-postgres pg_isready

# 인증 정보 확인
docker exec -it ggud-postgres psql -U postgres -c "SELECT 1"
```

**해결방법**:
1. .env 파일에 올바른 DB_PASSWORD가 있는지 확인
2. PostgreSQL 재시작: `docker-compose restart db`
3. application.yml datasource 설정 확인
4. 컨테이너 재생성: `docker-compose down && docker-compose up -d`

### 빌드 실패

**증상**: Gradle 빌드 실패

**일반적인 문제**:

**1. Java 버전 불일치**:
```bash
# Java 버전 확인
java -version  # 17 이상이어야 함

# 필요시 JAVA_HOME 설정
export JAVA_HOME=/path/to/java17
```

**2. 의존성 해결 문제**:
```bash
# Gradle 캐시 정리
./gradlew clean --refresh-dependencies

# .gradle 폴더 삭제
rm -rf .gradle/
./gradlew build
```

**3. Lombok 작동 안함**:
- IDE에서 annotation processing 활성화
- Lombok 플러그인 설치 (IntelliJ/Eclipse)
- 프로젝트 재빌드

### 애플리케이션 시작 실패

**확인**:
```bash
# 애플리케이션 로그 확인
./gradlew bootRun --stacktrace

# Docker 사용 시
docker-compose logs -f app
```

**일반적인 원인**:
1. **포트 이미 사용 중**: server.port 변경
2. **데이터베이스 준비 안됨**: db 시작 대기 또는 healthcheck 추가
3. **환경 변수 누락**: .env 파일 확인
4. **스키마 검증 실패**: Flyway 마이그레이션 실행 또는 스키마 수정

### 테스트 실패

**상세 정보와 함께 테스트 실행**:
```bash
# 모든 테스트
./gradlew test --info

# 특정 테스트 클래스
./gradlew test --tests "MeetingServiceTest"

# 스택트레이스 포함
./gradlew test --stacktrace
```

**테스트 상태 초기화**:
```bash
# 테스트 캐시 정리
./gradlew cleanTest test
```

---

## 개발 워크플로우

### 코드 변경

**1. 소스 코드 수정**

**2. 핫 리로드** (Spring DevTools 사용 시):
- Java 파일 변경 시 자동 재시작
- 정적 리소스 (HTML/CSS/JS) 변경 시 재시작 없이 리로드

**3. 수동 재시작**:
```bash
# 실행 중인 애플리케이션 중지 (Ctrl+C)
# 재빌드 및 실행
./gradlew bootRun
```

**4. Docker 사용 시**:
```bash
# 애플리케이션 이미지 재빌드
docker-compose build app

# 새 이미지로 재시작
docker-compose up -d --no-deps app
```

### 테스트 실행

**단위 테스트**:
```bash
./gradlew test
```

**통합 테스트**:
```bash
./gradlew integrationTest
```

**특정 테스트**:
```bash
./gradlew test --tests "MeetingServiceTest.createMeeting_shouldSucceed"
```

**커버리지 리포트**:
```bash
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

### 데이터베이스 마이그레이션

**Flyway 사용** (권장):
```bash
# src/main/resources/db/migration/에 마이그레이션 파일 배치
# V1__initial_schema.sql
# V2__add_midpoint_columns.sql

# 마이그레이션 실행
./gradlew flywayMigrate

# 마이그레이션 상태 확인
./gradlew flywayInfo
```

**수동 스키마 변경**:
```bash
# 데이터베이스 접속
docker exec -it ggud-postgres psql -U postgres -d ggud_db

# SQL 실행
ALTER TABLE meetings ADD COLUMN new_field VARCHAR(100);
```

### 디버깅

**IntelliJ IDEA**:
1. 코드에 브레이크포인트 설정
2. Debug 모드로 실행 (Shift+F9)
3. 또는 실행 중인 프로세스에 연결:
   - Run → Attach to Process
   - `GgUdApplication` 선택

**원격 디버깅** (Docker):
```yaml
# docker-compose.yml
services:
  app:
    environment:
      JAVA_OPTS: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
    ports:
      - "5005:5005"  # 디버그 포트
```

그런 다음 localhost:5005에 디버거 연결

---

## 환경 설정

### 개발 환경 (로컬)
```yaml
# application-local.yml
spring:
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: validate
  logging:
    level:
      dev.promise4.GgUd: DEBUG
      org.hibernate.SQL: DEBUG
```

### 테스트 환경
```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
  jpa:
    hibernate:
      ddl-auto: create-drop
```

### 프로덕션 환경
```yaml
# application-prod.yml
spring:
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate
  logging:
    level:
      dev.promise4.GgUd: INFO
```

---

## 성능 튜닝

### 데이터베이스 커넥션 풀
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
```

### JVM 옵션
```bash
# JAVA_OPTS 또는 docker-compose.yml에 설정
JAVA_OPTS="-Xms512m -Xmx2048m -XX:+UseG1GC"
```

### Redis 설정
```yaml
spring:
  data:
    redis:
      timeout: 60000
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

---

## 유용한 명령어

### Gradle
```bash
./gradlew tasks                  # 모든 태스크 목록
./gradlew dependencies          # 의존성 트리 표시
./gradlew build -x test         # 테스트 제외 빌드
./gradlew clean build           # 클린 빌드
```

### Docker
```bash
docker system prune -a          # 사용하지 않는 모든 Docker 데이터 정리
docker volume ls                # 볼륨 목록
docker network ls               # 네트워크 목록
```

### 데이터베이스
```bash
# 데이터베이스 백업
docker exec ggud-postgres pg_dump -U postgres ggud_db > backup.sql

# 데이터베이스 복원
docker exec -i ggud-postgres psql -U postgres -d ggud_db < backup.sql
```

---

## 관련 문서
- **프로젝트 개요**: [../project_brief.md](../project_brief.md)
- **API 명세**: [../api/backend-api.md](../api/backend-api.md)
- **현재 단계**: [../_memory/current_state.md](../_memory/current_state.md)
- **아키텍처 결정**: [../decisions/adr-index.md](../decisions/adr-index.md)
