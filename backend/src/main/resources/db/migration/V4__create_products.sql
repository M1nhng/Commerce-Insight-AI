-- =============================================================================
-- V4 — Create Products Table
-- Commerce Insight AI
-- =============================================================================

CREATE TABLE products (
    id            UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    sku           VARCHAR(100)    NOT NULL,
    name          VARCHAR(255)    NOT NULL,
    description   TEXT,
    price         DECIMAL(19,4)   NOT NULL CHECK (price >= 0),
    cost_price    DECIMAL(19,4)   CHECK (cost_price >= 0),
    image_url     VARCHAR(1000),
    category_id   UUID            REFERENCES categories(id) ON DELETE SET NULL,
    active        BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ
);

COMMENT ON TABLE  products IS 'Product catalog';
COMMENT ON COLUMN products.sku IS 'Stock-keeping unit — unique identifier (partial index on deleted_at IS NULL)';
COMMENT ON COLUMN products.price IS 'Selling price — DECIMAL(19,4) per financial precision requirement';
COMMENT ON COLUMN products.cost_price IS 'Optional cost price for margin calculation';
