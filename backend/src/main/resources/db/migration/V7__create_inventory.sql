-- =============================================================================
-- V7 — Create Inventory Tables
-- Commerce Insight AI
-- =============================================================================

CREATE TABLE inventory (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id              UUID        NOT NULL UNIQUE REFERENCES products(id) ON DELETE CASCADE,
    quantity                INTEGER     NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reserved_quantity       INTEGER     NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    low_stock_threshold     INTEGER     NOT NULL DEFAULT 10,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  inventory IS 'Current stock per product — 1:1 with products';
COMMENT ON COLUMN inventory.quantity IS 'Total stock on hand';
COMMENT ON COLUMN inventory.reserved_quantity IS 'Stock held for confirmed, unshipped orders';
COMMENT ON COLUMN inventory.low_stock_threshold IS 'Alert trigger threshold — alert when quantity <= this value';

CREATE TABLE inventory_movements (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      UUID        NOT NULL REFERENCES products(id),
    user_id         UUID        REFERENCES users(id) ON DELETE SET NULL,
    type            VARCHAR(50) NOT NULL
                    CHECK (type IN ('PURCHASE','SALE','ADJUSTMENT','RETURN','DAMAGE')),
    quantity        INTEGER     NOT NULL,
    before_qty      INTEGER     NOT NULL,
    after_qty       INTEGER     NOT NULL,
    reason          TEXT,
    reference_id    UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  inventory_movements IS 'Full audit trail of all stock changes';
COMMENT ON COLUMN inventory_movements.quantity IS 'Positive = stock in, Negative = stock out';
COMMENT ON COLUMN inventory_movements.reference_id IS 'Nullable — links to order_id or import job ID';
