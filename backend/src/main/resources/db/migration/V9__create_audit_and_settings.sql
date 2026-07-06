-- =============================================================================
-- V9 — Create Audit Logs and System Settings Tables
-- Commerce Insight AI
-- =============================================================================

CREATE TABLE audit_logs (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID            REFERENCES users(id) ON DELETE SET NULL,
    action      VARCHAR(100)    NOT NULL,
    entity_type VARCHAR(100),
    entity_id   UUID,
    old_value   JSONB,
    new_value   JSONB,
    ip_address  VARCHAR(45),
    user_agent  TEXT,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  audit_logs IS 'System-wide audit trail for significant business events';
COMMENT ON COLUMN audit_logs.action IS 'Action type: USER_LOGIN, ORDER_STATUS_CHANGED, PRODUCT_CREATED, etc.';
COMMENT ON COLUMN audit_logs.old_value IS 'JSONB snapshot of entity state before change';
COMMENT ON COLUMN audit_logs.new_value IS 'JSONB snapshot of entity state after change';

CREATE TABLE system_settings (
    key         VARCHAR(100)    PRIMARY KEY,
    value       TEXT            NOT NULL,
    description TEXT,
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_by  UUID            REFERENCES users(id) ON DELETE SET NULL
);

COMMENT ON TABLE  system_settings IS 'Key-value store for configurable system parameters';
COMMENT ON COLUMN system_settings.key IS 'Setting key, e.g., ai.provider, ai.model, inventory.default_low_stock_threshold';
