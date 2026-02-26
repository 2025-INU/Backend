-- V3: ai_place_recommendations는 promise_id만 사용. meeting_id 제거
ALTER TABLE ai_place_recommendations
    DROP COLUMN IF EXISTS meeting_id;

-- 기존 meeting_id 인덱스 제거
DROP INDEX IF EXISTS idx_recommendations_meeting_id;
DROP INDEX IF EXISTS idx_recommendations_ranking;

-- promise_id 기준 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_recommendations_promise_id
    ON ai_place_recommendations(promise_id);
CREATE INDEX IF NOT EXISTS idx_recommendations_promise_ranking
    ON ai_place_recommendations(promise_id, ranking);
