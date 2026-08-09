-- =============================================================================
-- V20 — Create customer_addresses table
-- Commerce Insight AI
-- =============================================================================

CREATE TABLE customer_addresses (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id     UUID            NOT NULL
                        REFERENCES customers(id) ON DELETE CASCADE,
    type            VARCHAR(20)     NOT NULL
                        CHECK (type IN ('SHIPPING', 'BILLING')),
    recipient_name  VARCHAR(200)    NOT NULL,
    phone           VARCHAR(50),
    address_line    VARCHAR(500)    NOT NULL,
    ward            VARCHAR(150),
    district        VARCHAR(150),
    province        VARCHAR(150),
    country         VARCHAR(100)    NOT NULL DEFAULT 'VN',
    is_default      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE customer_addresses IS 'Shipping and billing addresses for customers';
COMMENT ON COLUMN customer_addresses.type IS 'SHIPPING | BILLING';
COMMENT ON COLUMN customer_addresses.is_default IS 'At most one default per (customer_id, type)';
