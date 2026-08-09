-- =============================================================================
-- V22 — Customer Domain performance indexes
-- Commerce Insight AI
-- Uses IF NOT EXISTS to be idempotent (safe if indexes already exist)
-- =============================================================================

-- customers
CREATE INDEX IF NOT EXISTS idx_customers_status         ON customers(status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_customers_group_id       ON customers(group_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_customers_created_at     ON customers(created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_customers_email          ON customers(email) WHERE deleted_at IS NULL AND email IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_customers_phone          ON customers(phone) WHERE deleted_at IS NULL AND phone IS NOT NULL;

-- customer_addresses
CREATE INDEX IF NOT EXISTS idx_caddr_customer_id        ON customer_addresses(customer_id);
CREATE INDEX IF NOT EXISTS idx_caddr_customer_type      ON customer_addresses(customer_id, type);
CREATE INDEX IF NOT EXISTS idx_caddr_default            ON customer_addresses(customer_id, type, is_default) WHERE is_default = TRUE;

-- customer_groups (already has unique index on code from V19)
CREATE INDEX IF NOT EXISTS idx_cgroups_status           ON customer_groups(status);

-- customer_segments (already has unique index on code from V21)
CREATE INDEX IF NOT EXISTS idx_csegments_type_status    ON customer_segments(type, status);
