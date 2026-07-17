-- =============================================
-- V12 — Create login_history table
-- =============================================
-- Tracks every login attempt (success and failure) for security auditing.
-- Pairs with the audit_logs table for full traceability.
-- =============================================

CREATE TABLE login_history (
    id             UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id        UUID        NULL REFERENCES users(id) ON DELETE SET NULL,
    email          VARCHAR(255) NOT NULL,
    ip_address     VARCHAR(45)  NULL,
    user_agent     TEXT         NULL,
    success        BOOLEAN      NOT NULL,
    failure_reason VARCHAR(100) NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_login_history PRIMARY KEY (id)
);

-- Index for fetching login history by user (most recent first)
CREATE INDEX idx_login_history_user_id  ON login_history (user_id,  created_at DESC);

-- Index for security investigation by email (catches attempts on non-existent users)
CREATE INDEX idx_login_history_email    ON login_history (email,    created_at DESC);

-- Index for IP-based brute-force investigation
CREATE INDEX idx_login_history_ip       ON login_history (ip_address, created_at DESC);

-- Index for fast failure queries (monitoring dashboards)
CREATE INDEX idx_login_history_success  ON login_history (success,  created_at DESC);

COMMENT ON TABLE  login_history               IS 'Immutable log of every login attempt';
COMMENT ON COLUMN login_history.user_id       IS 'Null when the email was not found in the users table';
COMMENT ON COLUMN login_history.failure_reason IS 'e.g. INVALID_CREDENTIALS, ACCOUNT_LOCKED, ACCOUNT_DISABLED';
