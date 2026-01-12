# GitHub Actions CI/CD 설정 가이드

## 개요

이 문서는 GgUd 백엔드 프로젝트의 GitHub Actions CI/CD 파이프라인 설정 방법을 안내합니다.

## 목차
- [워크플로우 개요](#워크플로우-개요)
- [필수 설정](#필수-설정)
- [워크플로우 상세](#워크플로우-상세)
- [트러블슈팅](#트러블슈팅)

---

## 워크플로우 개요

### 1. CI 워크플로우 (`.github/workflows/ci.yml`)
- **트리거**: PR 생성, develop/feature 브랜치 푸시
- **목적**: 자동화된 빌드 및 테스트
- **실행 시간**: ~3-5분

### 2. CD 워크플로우 (`.github/workflows/cd-prod.yml`)
- **트리거**: main 브랜치 푸시, 수동 트리거
- **목적**: 프로덕션 배포
- **실행 시간**: ~3-5분

---

## 필수 설정

### 1. GitHub Secrets 등록

GitHub Repository → Settings → Secrets and variables → Actions → New repository secret

#### Docker Hub 인증 정보
```
DOCKER_USERNAME=<도커_허브_사용자명>
DOCKER_PASSWORD=<도커_허브_비밀번호_또는_액세스_토큰>
```

**Docker Hub Token 생성 방법:**
1. Docker Hub 로그인 (https://hub.docker.com)
2. Account Settings → Security → New Access Token
3. Token 설명 입력 (예: ggud-github-actions)
4. 생성된 Token을 GitHub Secrets에 등록

#### EC2 접속 정보
```
EC2_HOST=<EC2_PUBLIC_IP_OR_DOMAIN>
EC2_USER=ubuntu
EC2_SSH_KEY=<SSH_PRIVATE_KEY_CONTENT>
```

**EC2_SSH_KEY 설정 방법:**
1. SSH 키 파일 내용 복사:
   ```bash
   cat ggud-server-key.pem
   ```
2. 전체 내용을 복사 (BEGIN ~ END 포함)
3. GitHub Secrets에 붙여넣기

#### 데이터베이스 설정
```
DB_USERNAME=ggud_user
DB_PASSWORD=<STRONG_DB_PASSWORD>
```

#### Redis 설정
```
REDIS_PASSWORD=<STRONG_REDIS_PASSWORD>
```

#### JWT 설정
```
JWT_SECRET=<RANDOM_256BIT_SECRET>
```

**JWT_SECRET 생성 방법:**
```bash
openssl rand -base64 64
```

#### Kakao API 설정
```
KAKAO_REST_API_KEY=<YOUR_KAKAO_REST_API_KEY>
KAKAO_JAVASCRIPT_KEY=<YOUR_KAKAO_JS_KEY>
KAKAO_REDIRECT_URI=https://api.ggud.com/api/v1/auth/kakao/callback
```

#### AI 서버 설정
```
AI_SERVER_URL=<YOUR_AI_SERVER_URL>
```

### 2. GitHub Environments 설정 (선택 사항)

Repository → Settings → Environments → New environment

**Environment 이름:** `production`

**Protection rules 설정:**
- ✅ Required reviewers (배포 승인자 지정)
- ✅ Wait timer (배포 전 대기 시간)

**Environment secrets:**
- 프로덕션 전용 시크릿을 여기에 추가 가능

### 3. EC2 서버 준비

#### 3.1. Docker 및 Docker Compose 설치
```bash
# EC2에 접속
ssh -i ggud-server-key.pem ubuntu@<EC2_IP>

# Docker 설치
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 현재 사용자를 docker 그룹에 추가
sudo usermod -aG docker $USER

# Docker Compose 설치
sudo apt install docker-compose-plugin -y

# 로그아웃 후 재접속 (그룹 변경 적용)
exit
ssh -i ggud-server-key.pem ubuntu@<EC2_IP>

# 설치 확인
docker --version
docker compose version
```

#### 3.2. 디렉토리 구조 생성
```bash
# 배포 디렉토리 생성
sudo mkdir -p /opt/ggud/{data/{postgres,redis},logs,backups/{postgres,redis}}
sudo chown -R ubuntu:ubuntu /opt/ggud

# 권한 확인
ls -la /opt/ggud
```

**디렉토리 구조:**
```
/opt/ggud/
├── docker-compose.yml    # 프로덕션 docker-compose 파일
├── .env                        # 환경 변수 파일
├── data/                       # 데이터 저장소
│   ├── postgres/              # PostgreSQL 데이터
│   └── redis/                 # Redis 데이터
├── logs/                       # 애플리케이션 로그
└── backups/                    # 백업 파일
    ├── postgres/              # DB 백업
    └── redis/                 # Redis 백업
```

---

## 워크플로우 상세

### CI 워크플로우

#### 트리거 조건
```yaml
on:
  pull_request:
    branches: [ main, develop ]
  push:
    branches: [ develop, 'feature/**' ]
```

#### 주요 단계
1. **코드 체크아웃**: Repository 코드 가져오기
2. **Java 17 설정**: JDK 17 환경 구성
3. **서비스 시작**: PostgreSQL, Redis 컨테이너 시작
4. **빌드 및 테스트**: Gradle을 통한 애플리케이션 빌드 및 테스트 실행

#### 실행 예시
```bash
# PR 생성 시 자동 실행
git checkout -b feature/new-feature
git push origin feature/new-feature
# GitHub에서 PR 생성 → CI 자동 실행
```

### CD 워크플로우

#### 트리거 조건
```yaml
on:
  push:
    branches: [ main ]
  workflow_dispatch:  # 수동 트리거
```

#### 주요 단계
1. **Build and Push Job**
   - Docker 이미지 빌드
   - Docker Hub에 이미지 푸시
   - 버전 태그 지정 (latest, commit-sha, timestamp)

2. **Deploy Job**
   - EC2 SSH 연결
   - docker-compose.yml 파일 전송
   - 환경 변수 파일 생성
   - Docker 이미지 pull
   - 기존 컨테이너 중지
   - 새 컨테이너 시작
   - 오래된 이미지 정리

#### 배포 프로세스
```
Build Docker Image → Push to Docker Hub →
SSH to EC2 → Pull Image → Stop Old Containers →
Start New Containers with docker-compose
```

#### 수동 배포 방법
1. GitHub Repository 페이지 이동
2. Actions 탭 클릭
3. "CD - Production Deployment" 워크플로우 선택
4. "Run workflow" 버튼 클릭
5. main 브랜치 선택 후 실행

---

## 배포 플로우

### 일반적인 개발 플로우

```
1. feature 브랜치에서 개발
   ↓
2. develop 브랜치로 PR 생성
   ↓ (CI 자동 실행)
3. 코드 리뷰 및 테스트 확인
   ↓
4. develop 브랜치에 머지
   ↓ (CI 자동 실행)
5. develop 브랜치에서 충분히 테스트
   ↓
6. main 브랜치로 PR 생성
   ↓
7. 최종 검토 및 승인
   ↓
8. main 브랜치에 머지
   ↓ (CD 자동 실행 → 프로덕션 배포)
```

### Hotfix 플로우

```
1. main 브랜치에서 hotfix 브랜치 생성
   ↓
2. 긴급 수정 작업
   ↓
3. main 브랜치로 직접 PR
   ↓ (CI 자동 실행)
4. 긴급 승인 및 머지
   ↓ (CD 자동 실행)
5. develop 브랜치에도 머지
```

---

## 트러블슈팅

### 1. CI 테스트 실패

**문제:** 테스트가 로컬에서는 통과하지만 CI에서 실패

**해결방법:**
```bash
# 1. 테스트 환경 변수 확인
# ci.yml의 env 섹션 확인

# 2. 로컬에서 CI 환경과 동일하게 테스트
docker-compose up -d
./gradlew clean test

# 3. 테스트 로그 확인
# GitHub Actions → 실패한 워크플로우 → 로그 다운로드
```

### 2. CD 배포 실패

**문제:** SSH 연결 실패

**해결방법:**
```bash
# 1. EC2_SSH_KEY 시크릿 확인
# - BEGIN/END 포함 여부
# - 줄바꿈 문자 정상 포함 여부

# 2. EC2 보안 그룹 확인
# - SSH 포트(22) 허용 여부
# - GitHub Actions IP 허용 (또는 0.0.0.0/0)

# 3. 수동 SSH 연결 테스트
ssh -i ggud-server-key.pem ubuntu@<EC2_IP>
```

**문제:** 배포 스크립트 실행 권한 없음

**해결방법:**
```bash
# EC2에서 실행
sudo chmod +x /opt/ggud/scripts/deploy.sh
sudo chown ubuntu:ubuntu /opt/ggud/scripts/deploy.sh
```

### 3. 배포 후 애플리케이션 확인

**Docker 컨테이너 상태 확인:**
```bash
# 1. EC2 접속
ssh -i ggud-server-key.pem ubuntu@<EC2_IP>

# 2. 실행 중인 컨테이너 확인
cd /opt/ggud
docker-compose -f docker-compose.yml ps

# 3. 애플리케이션 로그 확인
docker-compose -f docker-compose.yml logs app -f

# 4. 데이터베이스 로그 확인
docker-compose -f docker-compose.yml logs db --tail 50

# 5. 헬스체크
curl http://localhost:8080/actuator/health
```

**환경 변수 문제:**
```bash
# 1. .env 파일 확인
cat /opt/ggud/.env

# 2. 컨테이너 환경 변수 확인
docker-compose -f docker-compose.yml exec app env | grep SPRING

# 3. 컨테이너 재시작
docker-compose -f docker-compose.yml restart app
```

### 4. 수동 롤백

**이전 Docker 이미지로 롤백:**
```bash
# 1. EC2 접속
ssh -i ggud-server-key.pem ubuntu@<EC2_IP>
cd /opt/ggud

# 2. 사용 가능한 이미지 확인
docker images | grep ggud-backend

# 3. .env 파일에서 이미지 태그 변경
vim .env
# IMAGE_TAG=<이전_커밋_SHA> 로 수정

# 4. 컨테이너 재시작
docker-compose -f docker-compose.yml down
docker-compose -f docker-compose.yml up -d

# 5. 상태 확인
docker-compose -f docker-compose.yml ps
docker-compose -f docker-compose.yml logs app --tail 100
```

**완전 초기화 및 재배포:**
```bash
# 모든 컨테이너 및 볼륨 제거 (주의!)
docker-compose -f docker-compose.yml down -v

# 최신 이미지 pull
docker pull <DOCKER_USERNAME>/ggud-backend:latest

# 재시작
docker-compose -f docker-compose.yml up -d
```

### 5. Secrets 관련 문제

**문제:** GitHub Secrets가 워크플로우에서 인식되지 않음

**해결방법:**
```bash
# 1. Secrets 이름 확인
# - 대소문자 정확히 일치
# - 공백 없음

# 2. Environment 설정 확인
# - cd-prod.yml의 environment 섹션 확인
# - production environment에 secrets 추가 여부

# 3. 워크플로우 재실행
# - Actions 탭 → 실패한 워크플로우 → Re-run jobs
```

---

---

## 보안 고려사항

### Secrets 관리
- ✅ 모든 민감한 정보는 GitHub Secrets에 저장
- ✅ 절대 코드에 하드코딩하지 않음
- ✅ 정기적인 비밀번호 변경
- ✅ 최소 권한 원칙 적용

### SSH 키 관리
- ✅ EC2 전용 SSH 키 사용
- ✅ 키 파일 안전하게 보관
- ✅ 불필요한 접근 제한

### 네트워크 보안
- ✅ 보안 그룹 최소 권한 설정
- ✅ SSH 포트 변경 고려
- ✅ Fail2Ban 설치

---

## 참고 자료

- [GitHub Actions 공식 문서](https://docs.github.com/en/actions)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [AWS EC2 사용 가이드](https://docs.aws.amazon.com/ec2/)
- [프로젝트 배포 가이드](../phases/phase-7-deployment.md)

---

## 다음 단계

1. ✅ GitHub Secrets 등록
2. ✅ EC2 서버 설정
3. ✅ 첫 배포 테스트 (수동)
4. ✅ CI/CD 파이프라인 검증

## 주의사항

### Docker 관련
- Docker Hub 저장소는 public으로 설정하거나, private인 경우 적절한 인증 설정 필요
- EC2에서 Docker 사용 시 디스크 공간 모니터링 필수 (`docker system df`)
- 정기적으로 사용하지 않는 이미지 정리 (`docker image prune`)
- PostgreSQL과 Redis 데이터는 `/opt/ggud/data`에 영구 저장됨

### CI/CD 운영
- CI/CD는 빌드 및 배포 자동화에만 집중합니다
- 헬스체크, 모니터링, 알림은 별도 시스템에서 관리하세요
- 배포 후 수동으로 애플리케이션 상태를 확인하세요
- Docker 이미지는 자동으로 버전 태그가 지정됩니다 (latest, commit-sha, timestamp)

### 데이터 백업
- PostgreSQL 데이터: `/opt/ggud/data/postgres`에 저장
- Redis 데이터: `/opt/ggud/data/redis`에 저장
- 정기적인 백업 스크립트 설정 권장 (`/opt/ggud/backups`로 복사)
- 중요 데이터는 S3 등 외부 스토리지로 백업 권장
