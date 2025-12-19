# Docker 설정 가이드

## 빠른 시작

### 1. 환경 변수 설정

`.env` 파일을 생성하고 필요한 환경 변수를 설정하세요:

```bash
# .env.example을 복사하여 .env 파일 생성
cp .env.example .env

# .env 파일을 편집하여 실제 값 입력
# 특히 다음 항목들은 반드시 변경하세요:
# - JWT_SECRET (보안 키)
# - KAKAO_CLIENT_ID, KAKAO_CLIENT_SECRET
# - NAVER_MAPS_CLIENT_ID, NAVER_MAPS_CLIENT_SECRET
```

### 2. Docker 컨테이너 실행

```bash
# 모든 서비스 시작 (빌드 포함)
docker-compose up -d --build

# 로그 확인
docker-compose logs -f app

# 특정 서비스 로그만 확인
docker-compose logs -f db
docker-compose logs -f cache
```

### 3. 애플리케이션 확인

```bash
# 헬스체크
curl http://localhost:8080/actuator/health

# Swagger UI
open http://localhost:8080/swagger-ui.html
```

## Docker 명령어

### 서비스 관리

```bash
# 모든 서비스 시작
docker-compose up -d

# 특정 서비스만 시작
docker-compose up -d app
docker-compose up -d db

# 서비스 중지
docker-compose stop

# 서비스 재시작
docker-compose restart app

# 서비스 중지 및 컨테이너 삭제
docker-compose down

# 볼륨까지 삭제 (데이터 초기화)
docker-compose down -v
```

### 빌드 및 재배포

```bash
# 이미지 재빌드 (코드 변경 시)
docker-compose build app

# 재빌드 후 시작
docker-compose up -d --build app

# 캐시 없이 재빌드 (완전히 새로 빌드)
docker-compose build --no-cache app
```

### 로그 확인

```bash
# 전체 로그 실시간 확인
docker-compose logs -f

# 특정 서비스 로그
docker-compose logs -f app

# 최근 100줄만 확인
docker-compose logs --tail=100 app

# 타임스탬프 포함
docker-compose logs -f -t app
```

### 컨테이너 접속

```bash
# PostgreSQL 접속
docker exec -it ggud-postgres psql -U postgres -d ggud_db

# Redis 접속
docker exec -it ggud-redis redis-cli

# 애플리케이션 컨테이너 쉘 접속
docker exec -it ggud-app sh
```

### 데이터베이스 관리

```bash
# PostgreSQL 백업
docker exec ggud-postgres pg_dump -U postgres ggud_db > backup.sql

# PostgreSQL 복원
docker exec -i ggud-postgres psql -U postgres ggud_db < backup.sql

# 데이터베이스 초기화
docker-compose down -v
docker-compose up -d
```

## 멀티스테이지 빌드 최적화

### Dockerfile 구조

1. **Stage 1: Build Stage**
   - Gradle 공식 이미지 사용
   - 의존성 캐싱으로 빌드 속도 향상
   - bootJar 생성

2. **Stage 2: Runtime Stage**
   - 경량 JRE 이미지 (alpine)
   - 비root 사용자로 실행 (보안)
   - 헬스체크 포함

### 이미지 크기 비교

```bash
# 멀티스테이지 빌드 전: ~800MB
# 멀티스테이지 빌드 후: ~250MB
# 약 70% 크기 감소
```

## 환경별 설정

### 로컬 개발 환경

```bash
# .env 파일 설정
SPRING_PROFILES_ACTIVE=local
JPA_DDL_AUTO=update
JPA_SHOW_SQL=true

# 실행
docker-compose up -d
```

### 프로덕션 환경

```bash
# .env 파일 설정
SPRING_PROFILES_ACTIVE=prod
JPA_DDL_AUTO=validate
JPA_SHOW_SQL=false

# JWT 시크릿 생성
openssl rand -base64 32

# 실행
docker-compose up -d
```

## 성능 최적화

### PostgreSQL 튜닝

compose.yaml에 이미 다음 최적화가 적용되어 있습니다:

- `max_connections=200`: 최대 동시 연결 수
- `shared_buffers=256MB`: 공유 버퍼 크기
- `effective_cache_size=1GB`: 캐시 크기
- `work_mem=2MB`: 작업 메모리
- `maintenance_work_mem=64MB`: 유지보수 작업 메모리

### Redis 설정

- `maxmemory 512mb`: 최대 메모리
- `maxmemory-policy allkeys-lru`: LRU 정책
- `appendonly yes`: AOF 지속성
- 자동 스냅샷: 900초마다 1개 변경, 300초마다 10개 변경, 60초마다 10000개 변경

## 트러블슈팅

### 포트 충돌

```bash
# 사용 중인 포트 확인
lsof -i :8080
lsof -i :5432
lsof -i :6379

# .env에서 포트 변경
APP_PORT=8081
DB_PORT=5433
REDIS_PORT=6380
```

### 볼륨 권한 문제

```bash
# 로그 디렉토리 권한 설정
mkdir -p logs
chmod 777 logs

# Docker 볼륨 재생성
docker-compose down -v
docker-compose up -d
```

### 빌드 실패

```bash
# Gradle 캐시 삭제
rm -rf .gradle build

# Docker 빌드 캐시 삭제
docker-compose build --no-cache

# 전체 재시작
docker-compose down -v
docker-compose up -d --build
```

### 데이터베이스 연결 실패

```bash
# 데이터베이스 상태 확인
docker-compose ps db

# 헬스체크 확인
docker inspect ggud-postgres | grep -A 10 Health

# 로그 확인
docker-compose logs db

# 수동 연결 테스트
docker exec -it ggud-postgres pg_isready -U postgres
```

## 모니터링

### 컨테이너 상태 확인

```bash
# 모든 컨테이너 상태
docker-compose ps

# 리소스 사용량
docker stats

# 특정 컨테이너 상세 정보
docker inspect ggud-app
```

### 헬스체크

```bash
# 애플리케이션 헬스
curl http://localhost:8080/actuator/health

# 데이터베이스 헬스
docker exec ggud-postgres pg_isready -U postgres

# Redis 헬스
docker exec ggud-redis redis-cli ping
```

## 개발 팁

### 코드 변경 시 빠른 재배포

```bash
# 1. 애플리케이션만 재빌드
docker-compose build app

# 2. 재시작
docker-compose up -d app

# 또는 한 번에
docker-compose up -d --build app
```

### 로컬 IDE와 함께 사용

```bash
# DB와 Redis만 Docker로 실행
docker-compose up -d db cache

# IDE에서 Spring Boot 애플리케이션 직접 실행
./gradlew bootRun
```

### 디버깅

```bash
# 디버그 모드로 실행 (포트 5005)
# Dockerfile에 다음 추가:
# ENV JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# compose.yaml에 포트 추가:
# ports:
#   - "5005:5005"
```
