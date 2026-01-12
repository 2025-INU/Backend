#!/bin/bash

###############################################################################
# GgUd Backend 간단 배포 스크립트
#
# 사용법: ./deploy.sh <jar-file-path>
#
# 이 스크립트는 다음 작업을 수행합니다:
# 1. 현재 실행 중인 애플리케이션 백업
# 2. 새 JAR 파일 복사
# 3. 애플리케이션 재시작
###############################################################################

set -e  # 에러 발생 시 스크립트 종료

# 환경 변수 설정
APP_NAME="ggud-backend"
APP_DIR="/opt/ggud/app"
BACKUP_DIR="/opt/ggud/backups"
SERVICE_NAME="ggud-backend.service"
JAR_NAME="ggud-backend.jar"

# 색상 출력
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# 입력 검증
if [ -z "$1" ]; then
    log_error "Usage: $0 <jar-file-path>"
    echo "Example: $0 /tmp/ggud-backend.jar"
    exit 1
fi

NEW_JAR=$1

if [ ! -f "$NEW_JAR" ]; then
    log_error "JAR file not found: $NEW_JAR"
    exit 1
fi

log_info "Starting deployment process"
log_info "JAR file: $NEW_JAR"

# 디렉토리 생성
mkdir -p "$APP_DIR" "$BACKUP_DIR"

# 백업
if [ -f "$APP_DIR/$JAR_NAME" ]; then
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    cp "$APP_DIR/$JAR_NAME" "$BACKUP_DIR/${APP_NAME}_${TIMESTAMP}.jar"
    log_info "Backup created: ${APP_NAME}_${TIMESTAMP}.jar"

    # 오래된 백업 삭제 (최근 5개만 유지)
    ls -t "$BACKUP_DIR/${APP_NAME}"_*.jar 2>/dev/null | tail -n +6 | xargs rm -f 2>/dev/null || true
else
    log_warn "No existing application to backup (first deployment)"
fi

# JAR 파일 복사
log_info "Copying new JAR file"
cp "$NEW_JAR" "$APP_DIR/$JAR_NAME"
chown ubuntu:ubuntu "$APP_DIR/$JAR_NAME"
chmod 755 "$APP_DIR/$JAR_NAME"

# 서비스 재시작
log_info "Restarting application service"
systemctl restart "$SERVICE_NAME"

# 서비스 상태 확인
sleep 5
if systemctl is-active --quiet "$SERVICE_NAME"; then
    log_info "✅ Deployment completed successfully"
    log_info "Service status: $(systemctl is-active $SERVICE_NAME)"
    log_info "Check logs: journalctl -u $SERVICE_NAME -f"
else
    log_error "❌ Service failed to start"
    log_error "Check logs: journalctl -u $SERVICE_NAME -n 50"
    exit 1
fi
