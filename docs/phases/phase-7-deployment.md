# Phase 7: AWS EC2 배포 및 CI/CD 파이프라인 구축

## 목표 (Goals)
- AWS EC2 t2.medium 인스턴스에 Spring Boot 애플리케이션 배포
- GitHub Actions를 통한 자동화된 CI/CD 파이프라인 구축
- 프로덕션 환경 설정 및 보안 강화
- 모니터링 및 로깅 시스템 구축
- 무중단 배포 환경 구성

## 예상 소요 시간
- **전체**: 5-7일
- **인프라 구축**: 2-3일
- **CI/CD 구성**: 2-3일
- **모니터링 및 최적화**: 1-2일

---

## Step 7.1: AWS 인프라 설정 및 준비

### 목표
AWS EC2 인스턴스 및 관련 리소스 생성, 네트워크 및 보안 설정

### 작업 항목

#### 7.1.1 AWS 계정 및 IAM 설정
**수행 작업:**
1. AWS 계정 생성 및 결제 정보 등록
2. IAM 사용자 생성:
   - 배포용 IAM 사용자 (`ggud-deploy-user`)
   - 필요 권한: EC2, RDS, S3, CloudWatch
3. Access Key 생성 및 안전하게 저장
4. GitHub Secrets에 등록:
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`
   - `AWS_REGION`

**보안 고려사항:**
- Root 계정 직접 사용 금지
- MFA(Multi-Factor Authentication) 활성화
- Access Key는 절대 코드에 포함하지 않음
- IAM 사용자는 최소 권한 원칙(Least Privilege) 적용

#### 7.1.2 EC2 인스턴스 생성
**인스턴스 사양:**
- **인스턴스 타입**: t2.medium (2 vCPU, 4GB RAM)
- **AMI**: Ubuntu Server 22.04 LTS
- **스토리지**: 30GB gp3 (General Purpose SSD)
- **리전**: ap-northeast-2 (서울)

**설정 단계:**
1. EC2 콘솔에서 "인스턴스 시작" 선택
2. 이름: `ggud-backend-server`
3. AMI 선택: Ubuntu Server 22.04 LTS
4. 인스턴스 유형: t2.medium
5. 키 페어 생성: `ggud-server-key.pem` (안전하게 보관)
6. 네트워크 설정:
   - VPC: Default VPC 또는 새로 생성
   - 서브넷: Public Subnet
   - 퍼블릭 IP 자동 할당: 활성화
7. 스토리지 구성: 30GB gp3
8. 태그 추가:
   - Name: `ggud-backend-server`
   - Environment: `production`
   - Project: `GgUd`

#### 7.1.3 보안 그룹 설정
**보안 그룹 이름**: `ggud-backend-sg`

**인바운드 규칙:**
| 타입 | 프로토콜 | 포트 | 소스 | 설명 |
|------|----------|------|------|------|
| SSH | TCP | 22 | My IP | 개발자 접근용 |
| HTTP | TCP | 80 | 0.0.0.0/0 | HTTP 트래픽 |
| HTTPS | TCP | 443 | 0.0.0.0/0 | HTTPS 트래픽 |
| Custom TCP | TCP | 8080 | 0.0.0.0/0 | Spring Boot (임시) |
| PostgreSQL | TCP | 5432 | Security Group ID | 내부 DB 접근 |
| Redis | TCP | 6379 | Security Group ID | 내부 Redis 접근 |

**아웃바운드 규칙:**
- 모든 트래픽 허용 (0.0.0.0/0)

**보안 강화:**
- SSH 포트는 개발자 IP만 허용
- 프로덕션 배포 후 8080 포트는 로드밸런서에서만 접근 가능하도록 변경
- 정기적인 보안 그룹 감사 실시

#### 7.1.4 Elastic IP 할당
**목적**: 인스턴스 재시작 시에도 고정 IP 유지

**설정 단계:**
1. EC2 콘솔에서 "탄력적 IP" 메뉴 선택
2. "탄력적 IP 주소 할당" 클릭
3. 할당된 IP를 EC2 인스턴스에 연결
4. DNS 레코드 업데이트 (도메인이 있는 경우)

**결과물:**
- 고정 퍼블릭 IP 주소 확보
- 도메인 연결 준비 완료

---

## Step 7.2: EC2 서버 초기 설정

### 목표
서버 환경 구성, 필수 소프트웨어 설치, 보안 설정

### 작업 항목

#### 7.2.1 SSH 접속 및 기본 설정
**접속 방법:**
```bash
# 키 페어 권한 설정
chmod 400 ggud-server-key.pem

# SSH 접속
ssh -i ggud-server-key.pem ubuntu@<ELASTIC_IP>
```

**기본 설정:**
```bash
# 시스템 업데이트
sudo apt update && sudo apt upgrade -y

# 타임존 설정 (서울)
sudo timedatectl set-timezone Asia/Seoul

# 호스트네임 설정
sudo hostnamectl set-hostname ggud-backend-server

# 재부팅
sudo reboot
```

#### 7.2.2 필수 소프트웨어 설치
**Java 17 설치:**
```bash
# OpenJDK 17 설치
sudo apt install openjdk-17-jdk -y

# 설치 확인
java -version
```

**Docker 및 Docker Compose 설치:**
```bash
# Docker 설치
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 현재 사용자를 docker 그룹에 추가
sudo usermod -aG docker $USER

# Docker Compose 설치
sudo apt install docker-compose-plugin -y

# 설치 확인
docker --version
docker compose version
```

**Nginx 설치 (리버스 프록시):**
```bash
# Nginx 설치
sudo apt install nginx -y

# 서비스 활성화
sudo systemctl enable nginx
sudo systemctl start nginx

# 상태 확인
sudo systemctl status nginx
```

**추가 유틸리티 설치:**
```bash
# 모니터링 및 디버깅 도구
sudo apt install htop curl wget git vim -y
```

#### 7.2.3 애플리케이션 디렉토리 구조 생성
**디렉토리 생성:**
```bash
# 애플리케이션 디렉토리
sudo mkdir -p /opt/ggud
sudo chown ubuntu:ubuntu /opt/ggud

# 로그 디렉토리
sudo mkdir -p /var/log/ggud
sudo chown ubuntu:ubuntu /var/log/ggud

# 환경 설정 디렉토리
mkdir -p /opt/ggud/config

# 백업 디렉토리
mkdir -p /opt/ggud/backups
```

**디렉토리 구조:**
```
/opt/ggud/
├── app/                    # JAR 파일
├── config/                 # 환경 설정 파일
│   ├── application-prod.yml
│   └── .env
├── backups/                # 백업 파일
└── logs/                   # 애플리케이션 로그
```

#### 7.2.4 방화벽 설정 (UFW)
**UFW 활성화 및 규칙 설정:**
```bash
# UFW 설치
sudo apt install ufw -y

# 기본 정책 설정
sudo ufw default deny incoming
sudo ufw default allow outgoing

# 필요한 포트 열기
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw allow 8080/tcp  # Spring Boot (임시)

# 방화벽 활성화
sudo ufw enable

# 상태 확인
sudo ufw status verbose
```

---

## Step 7.3: 데이터베이스 및 Redis 설정

### 목표
프로덕션용 PostgreSQL 및 Redis 설치 및 구성

### 작업 항목

#### 7.3.1 PostgreSQL 15 설치 및 설정
**PostgreSQL 설치:**
```bash
# PostgreSQL 리포지토리 추가
sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -

# 패키지 업데이트 및 설치
sudo apt update
sudo apt install postgresql-15 postgresql-contrib-15 -y

# 서비스 활성화
sudo systemctl enable postgresql
sudo systemctl start postgresql
```

**데이터베이스 및 사용자 생성:**
```bash
# PostgreSQL 접속
sudo -u postgres psql

# 데이터베이스 생성
CREATE DATABASE ggud_prod;

# 사용자 생성 및 권한 부여
CREATE USER ggud_user WITH ENCRYPTED PASSWORD '<STRONG_PASSWORD>';
GRANT ALL PRIVILEGES ON DATABASE ggud_prod TO ggud_user;

# 연결 확인
\c ggud_prod
\q
```

**PostgreSQL 설정 파일 수정:**
```bash
# postgresql.conf 편집
sudo vim /etc/postgresql/15/main/postgresql.conf

# 수정 내용:
# listen_addresses = 'localhost'  # 외부 접근 차단
# max_connections = 100
# shared_buffers = 256MB
# effective_cache_size = 1GB
# work_mem = 8MB

# pg_hba.conf 편집
sudo vim /etc/postgresql/15/main/pg_hba.conf

# 수정 내용:
# local   all             all                                     peer
# host    all             all             127.0.0.1/32            scram-sha-256
# host    all             all             ::1/128                 scram-sha-256

# PostgreSQL 재시작
sudo systemctl restart postgresql
```

**백업 스크립트 생성:**
```bash
# 백업 스크립트
cat > /opt/ggud/backups/backup-db.sh << 'EOF'
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/opt/ggud/backups"
DB_NAME="ggud_prod"
DB_USER="ggud_user"

pg_dump -U $DB_USER $DB_NAME | gzip > $BACKUP_DIR/ggud_backup_$DATE.sql.gz

# 7일 이상 된 백업 삭제
find $BACKUP_DIR -name "ggud_backup_*.sql.gz" -mtime +7 -delete
EOF

# 실행 권한 부여
chmod +x /opt/ggud/backups/backup-db.sh

# Cron 등록 (매일 새벽 3시)
(crontab -l 2>/dev/null; echo "0 3 * * * /opt/ggud/backups/backup-db.sh") | crontab -
```

#### 7.3.2 Redis 7 설치 및 설정
**Redis 설치:**
```bash
# Redis 설치
sudo apt install redis-server -y

# 서비스 활성화
sudo systemctl enable redis-server
sudo systemctl start redis-server
```

**Redis 설정:**
```bash
# redis.conf 편집
sudo vim /etc/redis/redis.conf

# 수정 내용:
# bind 127.0.0.1 ::1           # 로컬에서만 접근
# protected-mode yes            # 보호 모드 활성화
# requirepass <STRONG_PASSWORD> # 비밀번호 설정
# maxmemory 512mb               # 최대 메모리 설정
# maxmemory-policy allkeys-lru  # 메모리 정책

# Redis 재시작
sudo systemctl restart redis-server

# 연결 테스트
redis-cli
AUTH <STRONG_PASSWORD>
PING
# 응답: PONG
```

---

## Step 7.4: Spring Boot 애플리케이션 설정

### 목표
프로덕션 환경 설정 파일 작성, 환경 변수 관리

### 작업 항목

#### 7.4.1 프로덕션 설정 파일 생성
**application-prod.yml 작성:**
```yaml
# /opt/ggud/config/application-prod.yml
spring:
  application:
    name: ggud-backend

  datasource:
    url: jdbc:postgresql://localhost:5432/ggud_prod
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: false
        show_sql: false
    open-in-view: false

  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD}
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 10
          max-idle: 5
          min-idle: 2

  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

server:
  port: 8080
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain
  tomcat:
    threads:
      max: 200
      min-spare: 10
    accept-count: 100
    max-connections: 10000

logging:
  level:
    root: INFO
    dev.promise4.ggud: INFO
    org.springframework.web: INFO
    org.hibernate: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: /var/log/ggud/application.log
    max-size: 10MB
    max-history: 30

jwt:
  secret: ${JWT_SECRET}
  access-token-expiration: 3600000    # 1시간
  refresh-token-expiration: 604800000 # 7일

kakao:
  rest-api-key: ${KAKAO_REST_API_KEY}
  javascript-key: ${KAKAO_JAVASCRIPT_KEY}
  redirect-uri: ${KAKAO_REDIRECT_URI}

ai:
  server:
    url: ${AI_SERVER_URL}
    timeout: 5000

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
```

#### 7.4.2 환경 변수 파일 생성
**환경 변수 파일 (.env):**
```bash
# /opt/ggud/config/.env 생성
cat > /opt/ggud/config/.env << 'EOF'
# Database
DB_USERNAME=ggud_user
DB_PASSWORD=<STRONG_DB_PASSWORD>

# Redis
REDIS_PASSWORD=<STRONG_REDIS_PASSWORD>

# JWT
JWT_SECRET=<RANDOM_JWT_SECRET_256BIT>

# Kakao API
KAKAO_REST_API_KEY=<YOUR_KAKAO_REST_API_KEY>
KAKAO_JAVASCRIPT_KEY=<YOUR_KAKAO_JS_KEY>
KAKAO_REDIRECT_URI=https://api.ggud.com/api/v1/auth/kakao/callback

# AI Server
AI_SERVER_URL=<YOUR_AI_SERVER_URL>

# Profile
SPRING_PROFILES_ACTIVE=prod
EOF

# 파일 권한 설정 (소유자만 읽기 가능)
chmod 600 /opt/ggud/config/.env
```

**보안 주의사항:**
- 모든 비밀번호는 강력한 임의의 문자열 사용
- JWT_SECRET은 최소 256비트 길이의 랜덤 문자열
- .env 파일은 절대 Git에 커밋하지 않음
- GitHub Secrets에도 동일한 값 등록

#### 7.4.3 systemd 서비스 파일 생성
**서비스 파일 작성:**
```bash
# /etc/systemd/system/ggud-backend.service
sudo vim /etc/systemd/system/ggud-backend.service
```

**서비스 파일 내용:**
```ini
[Unit]
Description=GgUd Backend Spring Boot Application
After=syslog.target network.target postgresql.service redis-server.service

[Service]
Type=simple
User=ubuntu
Group=ubuntu
WorkingDirectory=/opt/ggud/app
EnvironmentFile=/opt/ggud/config/.env

ExecStart=/usr/bin/java \
  -Xms512m \
  -Xmx2g \
  -Dspring.profiles.active=prod \
  -Dspring.config.location=file:/opt/ggud/config/application-prod.yml \
  -jar /opt/ggud/app/ggud-backend.jar

SuccessExitStatus=143
TimeoutStopSec=10
Restart=on-failure
RestartSec=5

StandardOutput=append:/var/log/ggud/application.log
StandardError=append:/var/log/ggud/error.log

[Install]
WantedBy=multi-user.target
```

**서비스 활성화:**
```bash
# systemd 데몬 리로드
sudo systemctl daemon-reload

# 서비스 활성화
sudo systemctl enable ggud-backend.service

# 서비스 상태 확인
sudo systemctl status ggud-backend.service
```

---

## Step 7.5: Nginx 리버스 프록시 설정

### 목표
Nginx를 통한 리버스 프록시 구성, SSL/TLS 설정

### 작업 항목

#### 7.5.1 Nginx 설정 파일 작성
**사이트 설정 파일:**
```bash
# /etc/nginx/sites-available/ggud-backend
sudo vim /etc/nginx/sites-available/ggud-backend
```

**Nginx 설정 내용:**
```nginx
# HTTP 서버 블록 (HTTPS로 리다이렉트)
server {
    listen 80;
    server_name api.ggud.com;  # 실제 도메인으로 변경

    # Let's Encrypt 인증서 갱신을 위한 경로
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    # 모든 요청을 HTTPS로 리다이렉트
    location / {
        return 301 https://$server_name$request_uri;
    }
}

# HTTPS 서버 블록
server {
    listen 443 ssl http2;
    server_name api.ggud.com;  # 실제 도메인으로 변경

    # SSL 인증서 설정 (Let's Encrypt)
    ssl_certificate /etc/letsencrypt/live/api.ggud.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.ggud.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # 로그 설정
    access_log /var/log/nginx/ggud-access.log;
    error_log /var/log/nginx/ggud-error.log;

    # Gzip 압축
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;

    # 클라이언트 최대 업로드 크기
    client_max_body_size 10M;

    # Spring Boot 애플리케이션으로 프록시
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 타임아웃 설정
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # WebSocket 지원
    location /ws {
        proxy_pass http://localhost:8080/ws;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket 타임아웃 설정
        proxy_connect_timeout 7d;
        proxy_send_timeout 7d;
        proxy_read_timeout 7d;
    }

    # Health Check 엔드포인트
    location /actuator/health {
        proxy_pass http://localhost:8080/actuator/health;
        access_log off;
    }
}
```

**설정 활성화:**
```bash
# 심볼릭 링크 생성
sudo ln -s /etc/nginx/sites-available/ggud-backend /etc/nginx/sites-enabled/

# 기본 설정 비활성화
sudo rm /etc/nginx/sites-enabled/default

# 설정 테스트
sudo nginx -t

# Nginx 재시작
sudo systemctl restart nginx
```

#### 7.5.2 SSL/TLS 인증서 설정 (Let's Encrypt)
**Certbot 설치:**
```bash
# Certbot 설치
sudo apt install certbot python3-certbot-nginx -y

# SSL 인증서 발급
sudo certbot --nginx -d api.ggud.com

# 자동 갱신 설정 확인
sudo systemctl status certbot.timer

# 갱신 테스트
sudo certbot renew --dry-run
```

**인증서 자동 갱신:**
- Certbot은 자동으로 systemd timer를 통해 인증서를 갱신
- 만료 30일 전에 자동 갱신 시도
- 갱신 후 Nginx 자동 재시작

---

## Step 7.6: GitHub Actions CI/CD 파이프라인 구축

### 목표
GitHub Actions를 통한 자동화된 빌드, 테스트, 배포 파이프라인 구성

### 작업 항목

#### 7.6.1 GitHub Secrets 설정
**필요한 Secrets:**
1. **AWS 자격 증명:**
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`
   - `AWS_REGION`: `ap-northeast-2`

2. **EC2 SSH 접속:**
   - `EC2_HOST`: EC2 퍼블릭 IP 또는 도메인
   - `EC2_USER`: `ubuntu`
   - `EC2_SSH_KEY`: SSH private key (ggud-server-key.pem 내용)

3. **환경 변수:**
   - `DB_USERNAME`
   - `DB_PASSWORD`
   - `REDIS_PASSWORD`
   - `JWT_SECRET`
   - `KAKAO_REST_API_KEY`
   - `KAKAO_JAVASCRIPT_KEY`
   - `KAKAO_REDIRECT_URI`
   - `AI_SERVER_URL`

**Secrets 등록 방법:**
```
GitHub Repository → Settings → Secrets and variables → Actions → New repository secret
```

#### 7.6.2 GitHub Actions Workflow 파일 구조
**워크플로우 파일 생성 위치:**
```
.github/
└── workflows/
    ├── ci.yml           # PR 및 브랜치 푸시 시 CI 테스트
    ├── cd-prod.yml      # main 브랜치 푸시 시 프로덕션 배포
    └── health-check.yml # 배포 후 헬스체크
```

#### 7.6.3 CI 워크플로우 작성
**파일: `.github/workflows/ci.yml`**

**목적:**
- PR 생성 및 브랜치 푸시 시 자동 빌드 및 테스트
- 코드 품질 검증
- 테스트 커버리지 확인

**주요 단계:**
1. 코드 체크아웃
2. Java 17 환경 설정
3. Gradle 캐싱
4. 애플리케이션 빌드
5. 단위 테스트 실행
6. 통합 테스트 실행 (Testcontainers)
7. 테스트 리포트 생성
8. 빌드 아티팩트 업로드

**트리거:**
- `pull_request`: 모든 브랜치
- `push`: `develop`, `feature/*` 브랜치

#### 7.6.4 CD 워크플로우 작성 (프로덕션 배포)
**파일: `.github/workflows/cd-prod.yml`**

**목적:**
- main 브랜치에 푸시 시 자동 프로덕션 배포
- 무중단 배포 전략
- 배포 후 헬스체크
- 배포 실패 시 롤백

**주요 단계:**
1. **빌드 단계:**
   - 코드 체크아웃
   - Java 17 환경 설정
   - Gradle 빌드 (테스트 포함)
   - JAR 파일 생성

2. **배포 단계:**
   - EC2 SSH 접속
   - 기존 애플리케이션 백업
   - 새 JAR 파일 전송
   - 환경 변수 업데이트
   - 애플리케이션 재시작
   - 헬스체크 수행

3. **알림 단계:**
   - 배포 성공/실패 알림
   - Slack 또는 이메일 통지

**배포 전략:**
- Rolling Deployment (단일 인스턴스이므로 간단한 재시작)
- 향후 Blue-Green 또는 Canary 배포로 확장 가능

**트리거:**
- `push`: `main` 브랜치

#### 7.6.5 헬스체크 워크플로우 작성
**파일: `.github/workflows/health-check.yml`**

**목적:**
- 배포 후 애플리케이션 정상 동작 확인
- API 엔드포인트 응답 검증
- 데이터베이스 연결 확인

**주요 검증 항목:**
- `/actuator/health` 엔드포인트 응답 (200 OK)
- 데이터베이스 연결 상태
- Redis 연결 상태
- 주요 API 엔드포인트 응답 시간

**실행 조건:**
- CD 워크플로우 완료 후 자동 실행
- 수동 트리거 가능 (`workflow_dispatch`)

---

## Step 7.7: 배포 스크립트 작성

### 목표
서버에서 실행될 배포 자동화 스크립트 작성

### 작업 항목

#### 7.7.1 배포 스크립트 작성
**파일: `/opt/ggud/scripts/deploy.sh`**

**스크립트 구조:**
```bash
#!/bin/bash

# GgUd Backend 배포 스크립트
# 사용법: ./deploy.sh <jar-file-path>

set -e  # 에러 발생 시 스크립트 종료

# 변수 설정
APP_NAME="ggud-backend"
APP_DIR="/opt/ggud/app"
BACKUP_DIR="/opt/ggud/backups"
LOG_DIR="/var/log/ggud"
SERVICE_NAME="ggud-backend.service"

# 색상 출력
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 로그 함수
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# JAR 파일 경로 확인
if [ -z "$1" ]; then
    log_error "Usage: $0 <jar-file-path>"
    exit 1
fi

NEW_JAR=$1

if [ ! -f "$NEW_JAR" ]; then
    log_error "JAR file not found: $NEW_JAR"
    exit 1
fi

# 1. 현재 실행 중인 애플리케이션 백업
log_info "Backing up current application..."
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
if [ -f "$APP_DIR/$APP_NAME.jar" ]; then
    cp "$APP_DIR/$APP_NAME.jar" "$BACKUP_DIR/${APP_NAME}_${TIMESTAMP}.jar"
    log_info "Backup created: ${APP_NAME}_${TIMESTAMP}.jar"
fi

# 2. 애플리케이션 중지
log_info "Stopping application..."
sudo systemctl stop $SERVICE_NAME
sleep 5

# 3. 새 JAR 파일 복사
log_info "Copying new JAR file..."
cp "$NEW_JAR" "$APP_DIR/$APP_NAME.jar"
chown ubuntu:ubuntu "$APP_DIR/$APP_NAME.jar"
chmod 755 "$APP_DIR/$APP_NAME.jar"

# 4. 애플리케이션 시작
log_info "Starting application..."
sudo systemctl start $SERVICE_NAME
sleep 10

# 5. 헬스체크
log_info "Performing health check..."
MAX_RETRY=30
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRY ]; do
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health)

    if [ "$HTTP_STATUS" == "200" ]; then
        log_info "Application is healthy (HTTP $HTTP_STATUS)"
        log_info "Deployment successful!"
        exit 0
    fi

    RETRY_COUNT=$((RETRY_COUNT+1))
    log_warn "Health check failed (HTTP $HTTP_STATUS). Retry $RETRY_COUNT/$MAX_RETRY..."
    sleep 5
done

# 6. 헬스체크 실패 시 롤백
log_error "Health check failed after $MAX_RETRY attempts. Rolling back..."

# 최신 백업 파일 찾기
LATEST_BACKUP=$(ls -t $BACKUP_DIR/${APP_NAME}_*.jar | head -n 1)

if [ -z "$LATEST_BACKUP" ]; then
    log_error "No backup found for rollback"
    exit 1
fi

log_warn "Rolling back to: $LATEST_BACKUP"

# 애플리케이션 중지
sudo systemctl stop $SERVICE_NAME
sleep 5

# 백업 파일 복구
cp "$LATEST_BACKUP" "$APP_DIR/$APP_NAME.jar"

# 애플리케이션 시작
sudo systemctl start $SERVICE_NAME
sleep 10

# 롤백 후 헬스체크
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health)

if [ "$HTTP_STATUS" == "200" ]; then
    log_info "Rollback successful. Application is running."
    exit 1
else
    log_error "Rollback failed. Manual intervention required."
    exit 1
fi
```

**스크립트 권한 설정:**
```bash
# 스크립트 디렉토리 생성
mkdir -p /opt/ggud/scripts

# 실행 권한 부여
chmod +x /opt/ggud/scripts/deploy.sh
```

#### 7.7.2 로그 로테이션 스크립트
**파일: `/opt/ggud/scripts/rotate-logs.sh`**

**스크립트 내용:**
```bash
#!/bin/bash

# 로그 로테이션 스크립트
LOG_DIR="/var/log/ggud"
BACKUP_DIR="/opt/ggud/backups/logs"
DAYS_TO_KEEP=30

# 백업 디렉토리 생성
mkdir -p $BACKUP_DIR

# 현재 로그 압축
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
tar -czf $BACKUP_DIR/logs_${TIMESTAMP}.tar.gz -C $LOG_DIR .

# 로그 파일 초기화
> $LOG_DIR/application.log
> $LOG_DIR/error.log

# 오래된 백업 삭제
find $BACKUP_DIR -name "logs_*.tar.gz" -mtime +$DAYS_TO_KEEP -delete

echo "Log rotation completed: logs_${TIMESTAMP}.tar.gz"
```

**Cron 등록:**
```bash
# 매주 일요일 새벽 2시 로그 로테이션
(crontab -l 2>/dev/null; echo "0 2 * * 0 /opt/ggud/scripts/rotate-logs.sh") | crontab -
```

---

## Step 7.8: 모니터링 및 로깅 설정

### 목표
애플리케이션 및 인프라 모니터링, 로그 집계 및 분석

### 작업 항목

#### 7.8.1 Spring Boot Actuator 활성화
**설정 확인:**
- `application-prod.yml`에 Actuator 엔드포인트 설정 확인
- `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus` 활성화

**보안 설정:**
- Actuator 엔드포인트는 내부 IP에서만 접근 가능하도록 제한
- 또는 Basic Authentication 적용

#### 7.8.2 CloudWatch 로그 그룹 설정
**CloudWatch Logs Agent 설치:**
```bash
# CloudWatch Logs Agent 설치
wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
sudo dpkg -i -E ./amazon-cloudwatch-agent.deb

# 설정 파일 생성
sudo vim /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
```

**설정 파일 내용:**
```json
{
  "logs": {
    "logs_collected": {
      "files": {
        "collect_list": [
          {
            "file_path": "/var/log/ggud/application.log",
            "log_group_name": "/aws/ec2/ggud-backend",
            "log_stream_name": "application-{instance_id}",
            "timestamp_format": "%Y-%m-%d %H:%M:%S"
          },
          {
            "file_path": "/var/log/ggud/error.log",
            "log_group_name": "/aws/ec2/ggud-backend",
            "log_stream_name": "error-{instance_id}",
            "timestamp_format": "%Y-%m-%d %H:%M:%S"
          },
          {
            "file_path": "/var/log/nginx/ggud-access.log",
            "log_group_name": "/aws/ec2/ggud-backend",
            "log_stream_name": "nginx-access-{instance_id}"
          },
          {
            "file_path": "/var/log/nginx/ggud-error.log",
            "log_group_name": "/aws/ec2/ggud-backend",
            "log_stream_name": "nginx-error-{instance_id}"
          }
        ]
      }
    }
  },
  "metrics": {
    "namespace": "GgUd/Backend",
    "metrics_collected": {
      "cpu": {
        "measurement": [
          {
            "name": "cpu_usage_idle",
            "rename": "CPU_IDLE",
            "unit": "Percent"
          }
        ],
        "totalcpu": false
      },
      "disk": {
        "measurement": [
          {
            "name": "used_percent",
            "rename": "DISK_USED",
            "unit": "Percent"
          }
        ],
        "resources": [
          "*"
        ]
      },
      "mem": {
        "measurement": [
          {
            "name": "mem_used_percent",
            "rename": "MEM_USED",
            "unit": "Percent"
          }
        ]
      }
    }
  }
}
```

**Agent 시작:**
```bash
# Agent 시작
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
  -a fetch-config \
  -m ec2 \
  -s \
  -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json

# 상태 확인
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
  -m ec2 \
  -a query
```

#### 7.8.3 알람 설정 (CloudWatch Alarms)
**중요 알람:**
1. **CPU 사용률**: 80% 이상 5분 지속
2. **메모리 사용률**: 85% 이상 5분 지속
3. **디스크 사용률**: 80% 이상
4. **애플리케이션 에러 로그**: 에러 로그 급증
5. **HTTP 5xx 에러**: 5xx 응답 비율 5% 이상

**알람 생성 방법:**
- AWS CloudWatch Console에서 수동 생성
- 또는 Terraform/CloudFormation을 통한 IaC 관리

#### 7.8.4 로그 분석 대시보드
**CloudWatch Insights 쿼리 예시:**
```sql
-- 최근 1시간 에러 로그 분석
fields @timestamp, @message
| filter @message like /ERROR/
| sort @timestamp desc
| limit 100

-- API 응답 시간 분석
fields @timestamp, @message
| parse @message /response_time=(?<response_time>\d+)ms/
| stats avg(response_time), max(response_time), min(response_time) by bin(5m)

-- 가장 많이 호출된 API 엔드포인트
fields @timestamp, @message
| parse @message /path=(?<path>\/[^\s]+)/
| stats count() by path
| sort count desc
```

---

## Step 7.9: 보안 강화 및 최적화

### 목표
프로덕션 환경 보안 강화, 성능 최적화

### 작업 항목

#### 7.9.1 보안 체크리스트
**필수 보안 조치:**
- [ ] SSH 포트를 기본 22에서 변경 (예: 2222)
- [ ] SSH 비밀번호 인증 비활성화 (키 기반 인증만 허용)
- [ ] Root 로그인 비활성화
- [ ] Fail2Ban 설치 및 설정 (무차별 대입 공격 방어)
- [ ] 정기적인 보안 패치 자동화
- [ ] 애플리케이션 레벨 방화벽 (WAF) 고려
- [ ] 데이터베이스 암호화 (at-rest 및 in-transit)
- [ ] 환경 변수 암호화 (AWS Secrets Manager 활용)

**Fail2Ban 설정:**
```bash
# Fail2Ban 설치
sudo apt install fail2ban -y

# SSH 보호 설정
sudo vim /etc/fail2ban/jail.local

# 내용:
[sshd]
enabled = true
port = 22
filter = sshd
logpath = /var/log/auth.log
maxretry = 3
bantime = 3600

# Fail2Ban 시작
sudo systemctl enable fail2ban
sudo systemctl start fail2ban
```

#### 7.9.2 성능 최적화
**JVM 튜닝:**
- Heap 메모리: `-Xms512m -Xmx2g`
- GC 설정: G1GC 사용 (Java 17 기본)
- JVM 옵션 최적화

**데이터베이스 연결 풀 튜닝:**
- HikariCP 설정 최적화
- 최대 연결 수: 20
- 최소 유휴 연결: 5

**Redis 캐싱 전략:**
- 자주 조회되는 데이터 캐싱
- TTL 설정 (예: 사용자 세션 1시간)
- 캐시 무효화 전략

**Nginx 최적화:**
- Gzip 압축 활성화
- 정적 파일 캐싱
- 연결 최대 개수 조정

#### 7.9.3 정기 백업 전략
**백업 계획:**
1. **데이터베이스 백업:**
   - 일일 전체 백업 (새벽 3시)
   - 7일간 백업 보관
   - S3로 백업 업로드 (선택 사항)

2. **애플리케이션 백업:**
   - 배포 시마다 이전 버전 백업
   - 최근 5개 버전 보관

3. **로그 백업:**
   - 주간 로그 압축 및 백업
   - 30일간 보관

**S3 백업 스크립트 (선택 사항):**
```bash
#!/bin/bash
# S3로 백업 업로드
AWS_PROFILE=default
S3_BUCKET=ggud-backups

# 데이터베이스 백업 업로드
aws s3 cp /opt/ggud/backups/ggud_backup_$(date +%Y%m%d).sql.gz \
  s3://$S3_BUCKET/database/ \
  --profile $AWS_PROFILE

# 애플리케이션 백업 업로드
aws s3 sync /opt/ggud/backups/ \
  s3://$S3_BUCKET/application/ \
  --exclude "*.sql.gz" \
  --profile $AWS_PROFILE
```

---

## Step 7.10: 배포 테스트 및 검증

### 목표
실제 배포 전 전체 프로세스 테스트 및 검증

### 작업 항목

#### 7.10.1 로컬에서 프로덕션 빌드 테스트
**빌드 테스트:**
```bash
# 프로젝트 루트에서 실행
./gradlew clean build -Pprod

# JAR 파일 확인
ls -lh build/libs/

# JAR 파일 실행 테스트 (로컬)
java -jar build/libs/ggud-backend-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod
```

#### 7.10.2 수동 배포 테스트
**첫 배포는 수동으로 수행:**
1. JAR 파일을 EC2로 복사
2. 배포 스크립트 실행
3. 헬스체크 확인
4. API 테스트 수행

**배포 명령:**
```bash
# JAR 파일 EC2로 전송
scp -i ggud-server-key.pem \
  build/libs/ggud-backend-0.0.1-SNAPSHOT.jar \
  ubuntu@<ELASTIC_IP>:/tmp/

# EC2에 접속
ssh -i ggud-server-key.pem ubuntu@<ELASTIC_IP>

# 배포 스크립트 실행
/opt/ggud/scripts/deploy.sh /tmp/ggud-backend-0.0.1-SNAPSHOT.jar
```

#### 7.10.3 GitHub Actions 워크플로우 테스트
**단계별 테스트:**
1. **CI 워크플로우 테스트:**
   - feature 브랜치 생성
   - 코드 변경 후 푸시
   - GitHub Actions 로그 확인

2. **CD 워크플로우 테스트 (스테이징):**
   - develop 브랜치에 머지
   - 배포 로그 확인
   - 헬스체크 결과 확인

3. **프로덕션 배포 테스트:**
   - main 브랜치에 머지
   - 자동 배포 확인
   - 프로덕션 API 테스트

#### 7.10.4 롤백 테스트
**롤백 시나리오 테스트:**
1. 의도적으로 실패하는 버전 배포
2. 자동 롤백 동작 확인
3. 이전 버전으로 복구 확인

#### 7.10.5 부하 테스트 (선택 사항)
**간단한 부하 테스트:**
```bash
# Apache Bench를 사용한 부하 테스트
ab -n 1000 -c 10 https://api.ggud.com/actuator/health

# 또는 wrk 사용
wrk -t4 -c100 -d30s https://api.ggud.com/actuator/health
```

---

## Step 7.11: 문서화 및 운영 가이드

### 목표
운영 매뉴얼 작성, 트러블슈팅 가이드 정리

### 작업 항목

#### 7.11.1 배포 매뉴얼 작성
**내용:**
- 배포 프로세스 설명
- GitHub Actions 워크플로우 사용법
- 수동 배포 방법
- 롤백 절차

#### 7.11.2 트러블슈팅 가이드 작성
**주요 문제 및 해결 방법:**
1. **애플리케이션 시작 실패:**
   - 로그 확인: `journalctl -u ggud-backend.service -n 100`
   - 환경 변수 확인: `cat /opt/ggud/config/.env`
   - 데이터베이스 연결 확인

2. **메모리 부족:**
   - JVM 힙 메모리 조정
   - 불필요한 프로세스 종료
   - 인스턴스 업그레이드 고려

3. **데이터베이스 연결 실패:**
   - PostgreSQL 상태 확인: `sudo systemctl status postgresql`
   - 연결 설정 확인: `/etc/postgresql/15/main/postgresql.conf`

4. **배포 실패:**
   - GitHub Actions 로그 확인
   - EC2 SSH 접속 확인
   - 디스크 공간 확인: `df -h`

#### 7.11.3 모니터링 대시보드 사용 가이드
**CloudWatch 대시보드:**
- CPU, 메모리, 디스크 사용률 모니터링
- 애플리케이션 로그 조회 방법
- 알람 설정 및 관리

**로그 조회 명령:**
```bash
# 애플리케이션 로그 실시간 조회
tail -f /var/log/ggud/application.log

# 에러 로그만 필터링
grep ERROR /var/log/ggud/application.log | tail -n 50

# 특정 날짜 로그 조회
grep "2024-01-10" /var/log/ggud/application.log
```

---

## 체크리스트

### 인프라 설정
- [ ] AWS 계정 및 IAM 사용자 생성
- [ ] EC2 인스턴스 생성 (t2.medium, Ubuntu 22.04)
- [ ] 보안 그룹 설정
- [ ] Elastic IP 할당
- [ ] SSH 키 페어 생성 및 보관

### 서버 환경 구성
- [ ] 서버 초기 설정 (업데이트, 타임존, 호스트네임)
- [ ] Java 17 설치
- [ ] Docker 및 Docker Compose 설치
- [ ] Nginx 설치
- [ ] PostgreSQL 15 설치 및 설정
- [ ] Redis 7 설치 및 설정

### 애플리케이션 설정
- [ ] 프로덕션 설정 파일 작성 (application-prod.yml)
- [ ] 환경 변수 파일 작성 (.env)
- [ ] systemd 서비스 파일 작성
- [ ] Nginx 리버스 프록시 설정
- [ ] SSL/TLS 인증서 발급 (Let's Encrypt)

### CI/CD 파이프라인
- [ ] GitHub Secrets 등록
- [ ] CI 워크플로우 작성 (.github/workflows/ci.yml)
- [ ] CD 워크플로우 작성 (.github/workflows/cd-prod.yml)
- [ ] 헬스체크 워크플로우 작성
- [ ] 배포 스크립트 작성 및 테스트

### 모니터링 및 보안
- [ ] Spring Boot Actuator 활성화
- [ ] CloudWatch Logs Agent 설치 및 설정
- [ ] CloudWatch Alarms 생성
- [ ] Fail2Ban 설치 및 설정
- [ ] 정기 백업 설정 (데이터베이스, 애플리케이션, 로그)

### 테스트 및 검증
- [ ] 로컬에서 프로덕션 빌드 테스트
- [ ] 수동 배포 테스트
- [ ] GitHub Actions 워크플로우 테스트
- [ ] 롤백 시나리오 테스트
- [ ] 부하 테스트 (선택 사항)

### 문서화
- [ ] 배포 매뉴얼 작성
- [ ] 트러블슈팅 가이드 작성
- [ ] 모니터링 대시보드 사용 가이드 작성

---

## 다음 단계

Phase 7 완료 후:
1. **프로덕션 환경 안정화**: 초기 운영 기간 동안 모니터링 강화
2. **성능 튜닝**: 실제 트래픽 기반 최적화
3. **보안 감사**: 정기적인 보안 점검 및 패치
4. **확장성 고려**: Auto Scaling, Load Balancer 도입 검토
5. **재해 복구 계획**: Disaster Recovery 전략 수립

---

## 관련 문서

- **현재 상태**: [../_memory/current_state.md](../_memory/current_state.md)
- **프로젝트 개요**: [../project_brief.md](../project_brief.md)
- **API 명세서**: [../api/backend-api.md](../api/backend-api.md)
- **로컬 개발 가이드**: [../runbooks/local-dev.md](../runbooks/local-dev.md)
- **단계 개요**: [phase-0-overview.md](phase-0-overview.md)

---

## 참고 자료

**AWS 공식 문서:**
- [EC2 인스턴스 시작 가이드](https://docs.aws.amazon.com/ec2/index.html)
- [CloudWatch Logs Agent 설치](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/install-CloudWatch-Agent-on-EC2-Instance.html)

**GitHub Actions:**
- [GitHub Actions 공식 문서](https://docs.github.com/en/actions)
- [Self-hosted Runners](https://docs.github.com/en/actions/hosting-your-own-runners)

**Spring Boot:**
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Spring Boot 프로덕션 배포](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html)

**보안:**
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Let's Encrypt 인증서 발급](https://letsencrypt.org/getting-started/)
