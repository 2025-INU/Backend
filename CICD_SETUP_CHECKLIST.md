# CI/CD 설정 체크리스트

이 문서는 GgUd 백엔드 프로젝트의 CI/CD 파이프라인 설정을 위한 단계별 체크리스트입니다.

## 📋 설정 개요

- ✅ **CI 워크플로우**: 자동 빌드 및 테스트
- ✅ **CD 워크플로우**: AWS EC2 자동 배포
- ✅ **Health Check**: 프로덕션 모니터링
- ✅ **배포 스크립트**: 자동 롤백 지원

---

## 1단계: GitHub Secrets 등록 (필수)

### 등록 방법
```
GitHub Repository → Settings → Secrets and variables → Actions → New repository secret
```

### 필수 Secrets 목록

#### ✅ EC2 접속 정보
- [ ] `EC2_HOST` = EC2 퍼블릭 IP 또는 도메인  
- [ ] `EC2_USER` = `ubuntu`
- [ ] `EC2_SSH_KEY` = SSH 프라이빗 키 전체 내용

**EC2_SSH_KEY 설정:**
```bash
cat ggud-server-key.pem
# BEGIN부터 END까지 전체 복사하여 Secret에 붙여넣기
```

#### ✅ 데이터베이스
- [ ] `DB_USERNAME` = `ggud_user`
- [ ] `DB_PASSWORD` = 강력한 비밀번호

**비밀번호 생성:**
```bash
openssl rand -base64 32
```

#### ✅ Redis
- [ ] `REDIS_PASSWORD` = 강력한 비밀번호

#### ✅ JWT
- [ ] `JWT_SECRET` = 256비트 랜덤 문자열

**JWT Secret 생성:**
```bash
openssl rand -base64 64
```

#### ✅ Kakao API
- [ ] `KAKAO_REST_API_KEY` = 카카오 REST API 키
- [ ] `KAKAO_JAVASCRIPT_KEY` = 카카오 JavaScript 키
- [ ] `KAKAO_REDIRECT_URI` = `https://api.ggud.com/api/v1/auth/kakao/callback`

#### ✅ AI Server
- [ ] `AI_SERVER_URL` = AI 추천 서버 URL

---

## 2단계: EC2 서버 설정 (필수)

### 2.1 SSH 접속
```bash
ssh -i ggud-server-key.pem ubuntu@<EC2_IP>
```

### 2.2 디렉토리 구조 생성
```bash
sudo mkdir -p /opt/ggud/app
sudo mkdir -p /opt/ggud/config
sudo mkdir -p /opt/ggud/backups
sudo mkdir -p /opt/ggud/scripts
sudo mkdir -p /var/log/ggud

sudo chown -R ubuntu:ubuntu /opt/ggud
sudo chown -R ubuntu:ubuntu /var/log/ggud
```

**체크:**
- [ ] 디렉토리 생성 완료
- [ ] 권한 설정 완료

### 2.3 systemd 서비스 파일 생성
```bash
sudo vim /etc/systemd/system/ggud-backend.service
```

**내용 붙여넣기:**
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
sudo systemctl daemon-reload
sudo systemctl enable ggud-backend.service
```

**체크:**
- [ ] 서비스 파일 생성 완료
- [ ] 서비스 활성화 완료

### 2.4 환경 변수 파일 생성
```bash
vim /opt/ggud/config/.env
```

**내용 입력 (GitHub Secrets와 동일한 값):**
```bash
# Database
DB_USERNAME=ggud_user
DB_PASSWORD=<STRONG_DB_PASSWORD>

# Redis
REDIS_PASSWORD=<STRONG_REDIS_PASSWORD>

# JWT
JWT_SECRET=<RANDOM_JWT_SECRET>

# Kakao API
KAKAO_REST_API_KEY=<YOUR_KAKAO_REST_API_KEY>
KAKAO_JAVASCRIPT_KEY=<YOUR_KAKAO_JS_KEY>
KAKAO_REDIRECT_URI=https://api.ggud.com/api/v1/auth/kakao/callback

# AI Server
AI_SERVER_URL=<YOUR_AI_SERVER_URL>

# Profile
SPRING_PROFILES_ACTIVE=prod
```

**파일 권한 설정:**
```bash
chmod 600 /opt/ggud/config/.env
```

**체크:**
- [ ] .env 파일 생성 완료
- [ ] 모든 환경 변수 입력 완료
- [ ] 파일 권한 설정 완료 (600)

### 2.5 프로덕션 설정 파일 생성
```bash
vim /opt/ggud/config/application-prod.yml
```

**내용은 다음 문서 참조:**
`docs/phases/phase-7-deployment.md` → Step 7.4.1

**체크:**
- [ ] application-prod.yml 생성 완료
- [ ] 데이터베이스 설정 확인
- [ ] Redis 설정 확인
- [ ] 로깅 설정 확인

### 2.6 배포 스크립트 전송
```bash
# 로컬에서 실행
scp -i ggud-server-key.pem scripts/deploy.sh ubuntu@<EC2_IP>:/tmp/

# EC2에서 실행
sudo mv /tmp/deploy.sh /opt/ggud/scripts/
sudo chmod +x /opt/ggud/scripts/deploy.sh
sudo chown ubuntu:ubuntu /opt/ggud/scripts/deploy.sh
```

**체크:**
- [ ] deploy.sh 전송 완료
- [ ] 실행 권한 설정 완료

---

## 3단계: 데이터베이스 및 Redis 확인

### PostgreSQL 확인
```bash
# PostgreSQL 상태 확인
sudo systemctl status postgresql

# 데이터베이스 연결 테스트
sudo -u postgres psql -d ggud_prod -c "SELECT version();"
```

**체크:**
- [ ] PostgreSQL 실행 중
- [ ] ggud_prod 데이터베이스 존재
- [ ] ggud_user 사용자 권한 확인

### Redis 확인
```bash
# Redis 상태 확인
sudo systemctl status redis-server

# Redis 연결 테스트
redis-cli
AUTH <REDIS_PASSWORD>
PING
# 응답: PONG
```

**체크:**
- [ ] Redis 실행 중
- [ ] 비밀번호 인증 성공

---

## 4단계: 첫 배포 테스트 (수동)

### 4.1 로컬에서 JAR 빌드
```bash
./gradlew clean bootJar -Pprod
```

**체크:**
- [ ] 빌드 성공
- [ ] JAR 파일 생성 확인 (`build/libs/`)

### 4.2 JAR 파일 전송
```bash
scp -i ggud-server-key.pem \
  build/libs/ggud-backend-*.jar \
  ubuntu@<EC2_IP>:/tmp/ggud-backend.jar
```

**체크:**
- [ ] JAR 파일 전송 완료

### 4.3 배포 스크립트 실행
```bash
ssh -i ggud-server-key.pem ubuntu@<EC2_IP>
sudo /opt/ggud/scripts/deploy.sh /tmp/ggud-backend.jar
```

**배포 프로세스:**
1. 기존 애플리케이션 백업
2. 서비스 중지
3. 새 JAR 복사
4. 서비스 시작
5. 헬스체크 (최대 30회 시도)
6. 성공/실패 보고

**체크:**
- [ ] 배포 스크립트 정상 실행
- [ ] 헬스체크 통과
- [ ] 애플리케이션 정상 실행

### 4.4 배포 확인
```bash
# 서비스 상태 확인
sudo systemctl status ggud-backend.service

# 헬스체크
curl http://localhost:8080/actuator/health

# 로그 확인
sudo journalctl -u ggud-backend.service -n 50

# 포트 확인
sudo netstat -tlnp | grep 8080
```

**예상 결과:**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"}
  }
}
```

**체크:**
- [ ] 서비스 active (running)
- [ ] 헬스체크 200 OK
- [ ] 포트 8080 LISTEN
- [ ] 에러 로그 없음

---

## 5단계: GitHub Actions 워크플로우 테스트

### 5.1 CI 워크플로우 테스트

**Git 작업 (수동):**
```bash
# feature 브랜치 생성
git checkout -b feature/test-ci

# 파일 수정 (예: README.md)
echo "# CI Test" >> README.md
git add .
git commit -m "test: CI workflow test"
git push origin feature/test-ci
```

**GitHub 작업 (수동):**
1. GitHub에서 Pull Request 생성 (feature/test-ci → main)
2. Actions 탭에서 CI 워크플로우 자동 실행 확인
3. 테스트 통과 확인

**체크:**
- [ ] CI 워크플로우 자동 실행
- [ ] 빌드 성공
- [ ] 테스트 통과

### 5.2 CD 워크플로우 테스트

**GitHub Actions에서 수동 실행:**
1. Repository → Actions 탭
2. "CD - Production Deployment" 선택
3. "Run workflow" 버튼 클릭

**모니터링:**
1. Build Job 완료 확인
2. Deploy Job 실행 확인
3. Health Check 통과 확인

**체크:**
- [ ] Build Job 성공
- [ ] Deploy Job 성공
- [ ] Health Check 통과
- [ ] 프로덕션 애플리케이션 정상 동작

### 5.3 Health Check 워크플로우 테스트
```bash
# GitHub에서 수동 실행
# Repository → Actions → "Health Check" → Run workflow
```

**체크:**
- [ ] 애플리케이션 헬스 체크 통과
- [ ] 데이터베이스 연결 확인
- [ ] Redis 연결 확인
- [ ] 응답 시간 정상 (<3초)
- [ ] 시스템 리소스 정상

---

## 6단계: 전체 배포 플로우 테스트

### 6.1 feature → main PR 생성 (수동)

**Git 작업:**
```bash
# feature 브랜치에서 작업 완료 후
git add .
git commit -m "feat: test deployment flow"
git push origin feature/test-ci
```

**GitHub 작업:**
1. GitHub에서 Pull Request 생성 (feature/test-ci → main)
2. CI 워크플로우 자동 실행 확인
3. 테스트 통과 확인

**체크:**
- [ ] CI 워크플로우 자동 실행
- [ ] 빌드 및 테스트 통과

### 6.2 main 브랜치 배포 (자동)

**GitHub 작업:**
1. PR 승인 및 Merge 버튼 클릭 (수동)
2. CD 워크플로우 자동 실행 확인
3. 배포 완료 확인

**체크:**
- [ ] PR Merge 후 CD 자동 실행
- [ ] 프로덕션 배포 성공
- [ ] Health Check 통과
- [ ] 애플리케이션 정상 동작

---

## 7단계: 모니터링 설정

### 7.1 Health Check 스케줄 확인
- 30분마다 자동 실행
- Actions → "Health Check" 워크플로우에서 실행 이력 확인

**체크:**
- [ ] 자동 실행 확인 (30분 후)
- [ ] 결과 정상

### 7.2 알림 설정 (선택 사항)
- Repository → Settings → Notifications
- 워크플로우 실패 시 이메일 알림 설정

**체크:**
- [ ] 이메일 알림 설정 완료

---

## 트러블슈팅 가이드

### SSH 연결 실패
```bash
# 1. 보안 그룹 확인
# AWS Console → EC2 → Security Groups
# 인바운드 규칙에 SSH (22) 포트 허용 확인

# 2. SSH 키 권한 확인
chmod 400 ggud-server-key.pem

# 3. 수동 연결 테스트
ssh -v -i ggud-server-key.pem ubuntu@<EC2_IP>
```

### 헬스체크 실패
```bash
# 1. 애플리케이션 로그 확인
sudo journalctl -u ggud-backend.service -n 100 --no-pager

# 2. 데이터베이스 연결 확인
sudo -u postgres psql -d ggud_prod -U ggud_user

# 3. 환경 변수 확인
cat /opt/ggud/config/.env

# 4. 포트 사용 확인
sudo netstat -tlnp | grep 8080
```

### 배포 실패
```bash
# 1. 배포 스크립트 권한 확인
ls -l /opt/ggud/scripts/deploy.sh

# 2. 백업 파일 확인
ls -l /opt/ggud/backups/

# 3. 수동 롤백
sudo systemctl stop ggud-backend.service
sudo cp /opt/ggud/backups/ggud-backend_<최신>.jar /opt/ggud/app/ggud-backend.jar
sudo systemctl start ggud-backend.service
```

---

## 완료 체크리스트

### 필수 설정
- [ ] GitHub Secrets 등록 (11개)
- [ ] EC2 디렉토리 구조 생성
- [ ] systemd 서비스 파일 생성
- [ ] 환경 변수 파일 생성
- [ ] 프로덕션 설정 파일 생성
- [ ] 배포 스크립트 전송 및 권한 설정

### 첫 배포
- [ ] 수동 배포 테스트 성공
- [ ] 헬스체크 통과
- [ ] 애플리케이션 정상 동작

### CI/CD 파이프라인
- [ ] CI 워크플로우 테스트 성공
- [ ] CD 워크플로우 (수동) 테스트 성공
- [ ] CD 워크플로우 (자동) 테스트 성공
- [ ] Health Check 워크플로우 테스트 성공

### 모니터링
- [ ] Health Check 자동 실행 확인
- [ ] 알림 설정 완료 (선택 사항)

---

## 다음 단계

✅ **CI/CD 파이프라인 구축 완료!**

이제 다음을 고려하세요:
1. **보안 강화**: SSL/TLS 인증서, Fail2Ban, 방화벽 규칙
2. **모니터링**: CloudWatch 로그, 알람 설정
3. **백업**: 자동 백업 스크립트, S3 백업
4. **성능 최적화**: JVM 튜닝, 데이터베이스 인덱싱
5. **확장성**: Auto Scaling, Load Balancer

---

## 참고 문서

- 📚 [GitHub Actions 설정 가이드](docs/runbooks/github-actions-setup.md)
- 📚 [Phase 7 배포 가이드](docs/phases/phase-7-deployment.md)
- 📚 [워크플로우 README](.github/workflows/README.md)

---

## 지원

문제가 발생하면 다음을 확인하세요:
1. 워크플로우 실행 로그 (GitHub Actions)
2. 애플리케이션 로그 (`journalctl -u ggud-backend.service`)
3. 시스템 로그 (`/var/log/ggud/`)
4. GitHub Actions 설정 가이드의 트러블슈팅 섹션
