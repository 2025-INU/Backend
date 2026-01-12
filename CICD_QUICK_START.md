# CI/CD 빠른 시작 가이드

GgUd 백엔드 프로젝트의 **최소 설정**으로 CI/CD를 빠르게 시작하는 가이드입니다.

## 📋 필수 단계만 간단하게

### 1단계: GitHub Secrets 등록 (5분)

**경로:** `Repository → Settings → Secrets and variables → Actions → New repository secret`

```bash
# EC2 접속 정보
EC2_HOST=<EC2_퍼블릭_IP>
EC2_USER=ubuntu
EC2_SSH_KEY=<SSH_키_전체_내용>

# 환경 변수 (GitHub Secrets와 동일하게)
DB_USERNAME=ggud_user
DB_PASSWORD=<강력한_비밀번호>
REDIS_PASSWORD=<강력한_비밀번호>
JWT_SECRET=<256bit_랜덤_문자열>
KAKAO_REST_API_KEY=<카카오_키>
KAKAO_JAVASCRIPT_KEY=<카카오_JS_키>
KAKAO_REDIRECT_URI=https://api.ggud.com/api/v1/auth/kakao/callback
AI_SERVER_URL=<AI_서버_URL>
```

**비밀번호 생성 명령:**
```bash
# DB, Redis 비밀번호
openssl rand -base64 32

# JWT Secret
openssl rand -base64 64
```

---

### 2단계: EC2 서버 설정 (10분)

#### SSH 접속
```bash
ssh -i ggud-server-key.pem ubuntu@<EC2_IP>
```

#### 디렉토리 생성
```bash
sudo mkdir -p /opt/ggud/{app,config,backups}
sudo mkdir -p /var/log/ggud
sudo chown -R ubuntu:ubuntu /opt/ggud /var/log/ggud
```

#### systemd 서비스 파일
```bash
sudo vim /etc/systemd/system/ggud-backend.service
```

**내용 붙여넣기:**
```ini
[Unit]
Description=GgUd Backend Application
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

Restart=on-failure
RestartSec=5

StandardOutput=append:/var/log/ggud/application.log
StandardError=append:/var/log/ggud/error.log

[Install]
WantedBy=multi-user.target
```

**서비스 활성화:**
```bash
sudo systemctl daemon-reload
sudo systemctl enable ggud-backend.service
```

#### 환경 변수 파일
```bash
vim /opt/ggud/config/.env
```

**내용 (GitHub Secrets와 동일):**
```bash
DB_USERNAME=ggud_user
DB_PASSWORD=<강력한_비밀번호>
REDIS_PASSWORD=<강력한_비밀번호>
JWT_SECRET=<256bit_랜덤_문자열>
KAKAO_REST_API_KEY=<카카오_키>
KAKAO_JAVASCRIPT_KEY=<카카오_JS_키>
KAKAO_REDIRECT_URI=https://api.ggud.com/api/v1/auth/kakao/callback
AI_SERVER_URL=<AI_서버_URL>
SPRING_PROFILES_ACTIVE=prod
```

**파일 권한:**
```bash
chmod 600 /opt/ggud/config/.env
```

#### 프로덕션 설정 파일
```bash
vim /opt/ggud/config/application-prod.yml
```

**최소 설정:**
```yaml
spring:
  application:
    name: ggud-backend
  datasource:
    url: jdbc:postgresql://localhost:5432/ggud_prod
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD}

server:
  port: 8080

logging:
  file:
    name: /var/log/ggud/application.log

jwt:
  secret: ${JWT_SECRET}
  access-token-expiration: 3600000
  refresh-token-expiration: 604800000

kakao:
  rest-api-key: ${KAKAO_REST_API_KEY}
  javascript-key: ${KAKAO_JAVASCRIPT_KEY}
  redirect-uri: ${KAKAO_REDIRECT_URI}

ai:
  server:
    url: ${AI_SERVER_URL}
```

---

### 3단계: 첫 배포 (3분)

#### 방법 1: main 브랜치 푸시 (자동 배포)
```bash
git checkout main
git merge develop
git push origin main
```

#### 방법 2: GitHub Actions 수동 실행
1. GitHub Repository 페이지 이동
2. **Actions** 탭 클릭
3. **CD - Production Deployment** 선택
4. **Run workflow** 버튼 클릭

---

### 4단계: 배포 확인 (2분)

#### GitHub Actions에서 확인
- Actions 탭에서 워크플로우 실행 로그 확인
- 모든 단계가 녹색 체크 표시되면 성공

#### EC2에서 확인
```bash
# SSH 접속
ssh -i ggud-server-key.pem ubuntu@<EC2_IP>

# 서비스 상태 확인
sudo systemctl status ggud-backend.service

# 로그 확인
sudo journalctl -u ggud-backend.service -n 50

# 포트 확인
sudo netstat -tlnp | grep 8080
```

**예상 결과:**
```
● ggud-backend.service - GgUd Backend Application
   Active: active (running)
```

---

## ✅ 완료!

이제 main 브랜치에 푸시할 때마다 자동으로 EC2에 배포됩니다.

---

## 🔄 일상적인 배포 플로우

```bash
# 1. feature 브랜치에서 작업
git checkout -b feature/new-feature
# ... 코드 작성 ...
git commit -m "feat: add new feature"
git push origin feature/new-feature

# 2. GitHub에서 PR 생성 (feature → develop)
# CI가 자동으로 실행되어 테스트

# 3. PR 승인 및 머지

# 4. develop에서 충분히 테스트 후 main으로 PR
git checkout main
git pull origin main
# GitHub에서 PR 생성 (develop → main)

# 5. PR 승인 및 머지
# CD가 자동으로 실행되어 프로덕션 배포
```

---

## 🚨 문제 발생 시

### 배포 실패
```bash
# EC2 로그 확인
ssh -i ggud-server-key.pem ubuntu@<EC2_IP>
sudo journalctl -u ggud-backend.service -n 100
```

### 수동 롤백
```bash
# 백업 목록 확인
ls -lh /opt/ggud/backups/

# 이전 버전으로 복구
sudo systemctl stop ggud-backend.service
sudo cp /opt/ggud/backups/ggud-backend_<TIMESTAMP>.jar /opt/ggud/app/ggud-backend.jar
sudo systemctl start ggud-backend.service
```

### 서비스 재시작
```bash
sudo systemctl restart ggud-backend.service
sudo systemctl status ggud-backend.service
```

---

## 📚 상세 문서

더 자세한 정보가 필요하면:
- [.github/workflows/README.md](.github/workflows/README.md) - 워크플로우 상세
- [docs/phases/phase-7-deployment.md](docs/phases/phase-7-deployment.md) - 전체 배포 가이드

---

## 💡 팁

- **자동 백업**: 배포마다 자동으로 백업 생성 (최근 5개 유지)
- **환경 변수 업데이트**: CD 워크플로우가 자동으로 .env 파일 업데이트
- **로그 확인**: 배포 후 항상 로그를 확인하는 습관
- **신중한 main 푸시**: main 브랜치는 프로덕션이므로 신중하게!