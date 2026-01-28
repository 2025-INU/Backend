-- V2: Performance optimization indexes
-- Note: Run this manually if not using Flyway, or ensure Flyway is configured

-- Promises table: composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_promises_host_status
    ON promises(host_id, status);

CREATE INDEX IF NOT EXISTS idx_promises_status_created
    ON promises(status, created_at DESC);

-- Participants table: composite index for location submission queries
CREATE INDEX IF NOT EXISTS idx_participants_promise_location
    ON participants(promise_id, is_location_submitted);

-- Refresh tokens: index for cleanup queries
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expiry
    ON refresh_tokens(expiry_date)
    WHERE revoked = false;

-- Promise: index for scheduled tasks (invite expiration check)
CREATE INDEX IF NOT EXISTS idx_promises_invite_expired
    ON promises(invite_expired_at)
    WHERE status = 'RECRUITING';

-- Promise: index for scheduled tasks (status auto-transition)
CREATE INDEX IF NOT EXISTS idx_promises_confirmed_datetime
    ON promises(promise_date_time)
    WHERE status = 'CONFIRMED';
