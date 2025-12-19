-- ===================================
-- PostgreSQL Database Initialization Script
-- ===================================

-- 확장 기능 활성화
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";      -- UUID 생성
CREATE EXTENSION IF NOT EXISTS "pg_trgm";        -- 유사 문자열 검색
CREATE EXTENSION IF NOT EXISTS "btree_gist";     -- GiST 인덱스 지원

-- 데이터베이스 설정
ALTER DATABASE ggud_db SET timezone TO 'Asia/Seoul';

-- 스키마 생성 (선택사항)
-- CREATE SCHEMA IF NOT EXISTS ggud;

-- 초기 사용자 생성 (예시)
-- CREATE USER ggud_user WITH ENCRYPTED PASSWORD 'ggud_password';
-- GRANT ALL PRIVILEGES ON DATABASE ggud_db TO ggud_user;

-- 로깅 설정
COMMENT ON DATABASE ggud_db IS 'GgUd Meeting Management Application Database';

\echo 'Database initialization completed successfully!'
