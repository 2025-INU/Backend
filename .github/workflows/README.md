# GitHub Actions Workflows

이 디렉토리에는 GgUd 백엔드 프로젝트의 CI/CD 파이프라인 워크플로우가 포함되어 있습니다.

## 워크플로우 목록

### 1. 📋 CI - Build and Test (`ci.yml`)
**목적:** 코드 품질 검증 및 자동화된 테스트

**트리거:**
- Pull Request (→ main, develop)
- Push (develop, feature/* 브랜치)

**주요 작업:**
- Spring Boot 애플리케이션 빌드
- 단위 테스트 및 통합 테스트 실행
- 테스트 커버리지 리포트 생성
- PR 코멘트로 결과 알림

**실행 시간:** ~5-10분

---

### 2. 🚀 CD - Production Deployment (`cd-prod.yml`)
**목적:** AWS EC2로 간단하고 빠른 자동 배포

**트리거:**
- Push (main 브랜치)
- 수동 실행 (workflow_dispatch)

**주요 작업:**
- 프로덕션 JAR 빌드
- 기존 애플리케이션 자동 백업
- EC2로 JAR 파일 전송
- 환경 변수 업데이트
- 애플리케이션 재시작

**실행 시간:** ~5-7분

**배포 환경:**
- Server: AWS EC2 t2.medium
- OS: Ubuntu 22.04
- Java: OpenJDK 17

---

## 빠른 시작

### 1. GitHub Secrets 설정 필수

다음 Secrets를 Repository Settings에 등록해야 합니다:

```
# EC2 접속
EC2_HOST=<EC2_PUBLIC_IP>
EC2_USER=ubuntu
EC2_SSH_KEY=<SSH_PRIVATE_KEY_CONTENT>

# 데이터베이스
DB_USERNAME=ggud_user
DB_PASSWORD=<DB_PASSWORD>

# Redis
REDIS_PASSWORD=<REDIS_PASSWORD>

# JWT
JWT_SECRET=<JWT_SECRET>

# Kakao API
KAKAO_REST_API_KEY=<KAKAO_KEY>
KAKAO_JAVASCRIPT_KEY=<KAKAO_JS_KEY>
KAKAO_REDIRECT_URI=https://api.ggud.com/api/v1/auth/kakao/callback

# AI Server
AI_SERVER_URL=<AI_SERVER_URL>
```

### 2. EC2 서버 준비

배포 전 EC2에서 다음을 준비해야 합니다:

```bash
# 1. 디렉토리 구조 생성
sudo mkdir -p /opt/ggud/{app,config,backups}
sudo mkdir -p /var/log/ggud
sudo chown -R ubuntu:ubuntu /opt/ggud /var/log/ggud

# 2. systemd 서비스 파일 생성
sudo vim /etc/systemd/system/ggud-backend.service
sudo systemctl daemon-reload
sudo systemctl enable ggud-backend.service

# 3. 환경 설정 파일 생성
vim /opt/ggud/config/.env
chmod 600 /opt/ggud/config/.env

# 4. 프로덕션 설정 파일 생성
vim /opt/ggud/config/application-prod.yml
```

### 3. 첫 배포 실행

```bash
# 1. main 브랜치로 푸시 (자동 배포)
git checkout main
git merge develop
git push origin main

# 또는 수동 배포
# GitHub → Actions → "CD - Production Deployment" → Run workflow
```

---

## 워크플로우 작동 방식

### 개발 플로우
```
feature 브랜치 개발
    ↓
develop 브랜치 PR (CI 실행)
    ↓
코드 리뷰 & 테스트 확인
    ↓
develop 머지
    ↓
main 브랜치 PR (CI 실행)
    ↓
최종 승인
    ↓
main 머지 (CD 자동 실행)
    ↓
프로덕션 배포 완료
```

### 배포 프로세스
```
1. Build Job
   - JAR 빌드
   - 아티팩트 업로드

2. Deploy Job
   - EC2 SSH 연결
   - 기존 앱 백업
   - JAR 파일 전송
   - 환경 변수 업데이트
   - 서비스 재시작
```

---

## 트러블슈팅

### CI 실패
```bash
# 로컬에서 테스트 재현
docker-compose up -d
./gradlew clean test

# 로그 확인
# Actions → 실패한 워크플로우 → 로그 다운로드
```

### CD 실패
```bash
# SSH 연결 테스트
ssh -i ggud-server-key.pem ubuntu@<EC2_IP>

# 애플리케이션 로그 확인
sudo journalctl -u ggud-backend.service -n 100

# 서비스 상태 확인
sudo systemctl status ggud-backend.service

# 수동 재시작
sudo systemctl restart ggud-backend.service
```

### 배포 후 확인
```bash
# 서비스 상태 확인
sudo systemctl status ggud-backend.service

# 로그 실시간 모니터링
sudo journalctl -u ggud-backend.service -f

# 포트 확인
sudo netstat -tlnp | grep 8080
```

---

## 수동 롤백

배포 후 문제가 발생하면 수동으로 롤백할 수 있습니다:

```bash
# EC2에 접속
ssh -i ggud-server-key.pem ubuntu@<EC2_IP>

# 백업 목록 확인
ls -lh /opt/ggud/backups/

# 이전 버전으로 롤백
sudo systemctl stop ggud-backend.service
sudo cp /opt/ggud/backups/ggud-backend_<TIMESTAMP>.jar /opt/ggud/app/ggud-backend.jar
sudo systemctl start ggud-backend.service

# 상태 확인
sudo systemctl status ggud-backend.service
```

---

## 주의사항

⚠️ **보안:**
- Secrets를 절대 코드에 포함하지 마세요
- SSH 키는 안전하게 보관하세요
- 정기적으로 비밀번호를 변경하세요

⚠️ **배포:**
- main 브랜치 푸시 시 자동으로 프로덕션 배포됩니다
- 신중하게 검토 후 머지하세요
- 배포 후 수동으로 애플리케이션 상태를 확인하세요

⚠️ **모니터링:**
- 배포 후 로그를 확인하세요
- 문제 발생 시 즉시 롤백하세요

---

## 지원

문제가 발생하면 다음을 확인하세요:
1. GitHub Actions 실행 로그
2. EC2 애플리케이션 로그
3. systemd 서비스 상태
