-- =============================================================================
-- V19 — Create customer_groups table
-- Commerce Insight AI
-- =============================================================================

CREATE TABLE customer_groups (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(50)     NOT NULL,
    name        VARCHAR(150)    NOT NULL,
    description TEXT,
    status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_customer_groups_code ON customer_groups(code);

-- Now add the FK from customers → customer_groups
ALTER TABLE customers
    ADD CONSTRAINT fk_customers_group
    FOREIGN KEY (group_id) REFERENCES customer_groups(id)
    ON DELETE SET NULL;

COMMENT ON TABLE customer_groups IS 'Customer segmentation groups (VIP, Wholesale, Retail, etc.)';
COMMENT ON COLUMN customer_groups.code IS 'Unique code e.g. VIP, WHOLESALE, RETAIL';
COMMENT ON COLUMN customer_groups.status IS 'ACTIVE | INACTIVE';
