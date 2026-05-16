-- V4: 정산 카카오톡 알림 자동 갱신을 위한 카카오 리프레시 토큰 저장 컬럼 추가
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS kakao_refresh_token TEXT;
