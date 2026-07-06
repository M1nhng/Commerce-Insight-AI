-- =============================================================================
-- V5 — Create Customers Table
-- Commerce Insight AI
-- =============================================================================

CREATE TABLE customers (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    email           VARCHAR(255),
    phone           VARCHAR(50),
    address_line1   VARCHAR(255),
    address_line2   VARCHAR(255),
    city            VARCHAR(100),
    country         VARCHAR(100),
    postal_code     VARCHAR(20),
    notes           TEXT,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

COMMENT ON TABLE customers IS 'Customer profiles — linked to orders';
