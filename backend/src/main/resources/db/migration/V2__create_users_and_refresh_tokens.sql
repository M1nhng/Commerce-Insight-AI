-- =============================================================================
-- V2 — Create Users and Refresh Tokens Tables
-- Commerce Insight AI
-- =============================================================================
-- Purpose: Core authentication tables.
--   - users: Platform user accounts with RBAC roles
--   - refresh_tokens: JWT refresh token rotation records
-- =============================================================================

-- ── users ─────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    role            VARCHAR(50)     NOT NULL
                    CHECK (role IN ('ADMIN', 'MANAGER', 'STAFF')),
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    locked          BOOLEAN         NOT NULL DEFAULT FALSE,
    failed_attempts INTEGER         NOT NULL DEFAULT 0,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

COMMENT ON TABLE  users IS 'Platform user accounts with role-based access control';
COMMENT ON COLUMN users.email IS 'Unique email used for authentication (partial unique index on deleted_at IS NULL)';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hashed password — never store plaintext';
COMMENT ON COLUMN users.role IS 'RBAC role: ADMIN | MANAGER | STAFF';
COMMENT ON COLUMN users.active IS 'False = account deactivated, cannot log in';
COMMENT ON COLUMN users.locked IS 'True = account locked after failed login attempts';
COMMENT ON COLUMN users.failed_attempts IS 'Consecutive failed login counter, reset on success';
COMMENT ON COLUMN users.deleted_at IS 'Soft delete timestamp — NULL means active';

-- ── refresh_tokens ──────────────────────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash      VARCHAR(64)     NOT NULL,
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    family_id       UUID            NOT NULL,
    expires_at      TIMESTAMPTZ     NOT NULL,
    revoked         BOOLEAN         NOT NULL DEFAULT FALSE,
    revoked_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  refresh_tokens IS 'JWT refresh token rotation records';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'SHA-256 hex hash of the raw token UUID — never store the plain token';
COMMENT ON COLUMN refresh_tokens.family_id IS 'Token rotation family — all tokens from one login share a family_id';
COMMENT ON COLUMN refresh_tokens.revoked IS 'True if token was used (rotated) or invalidated (logout/reuse detection)';
