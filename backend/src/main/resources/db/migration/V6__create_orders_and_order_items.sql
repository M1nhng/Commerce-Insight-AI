-- =============================================================================
-- V6 — Create Orders and Order Items Tables
-- Commerce Insight AI
-- =============================================================================

CREATE TABLE orders (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number    VARCHAR(50)     NOT NULL,
    customer_id     UUID            REFERENCES customers(id) ON DELETE SET NULL,
    status          VARCHAR(50)     NOT NULL
                    CHECK (status IN ('PENDING','CONFIRMED','PROCESSING',
                                      'SHIPPED','DELIVERED','CANCELLED','REFUNDED')),
    subtotal        DECIMAL(19,4)   NOT NULL DEFAULT 0,
    discount        DECIMAL(19,4)   NOT NULL DEFAULT 0,
    shipping_fee    DECIMAL(19,4)   NOT NULL DEFAULT 0,
    tax             DECIMAL(19,4)   NOT NULL DEFAULT 0,
    total           DECIMAL(19,4)   NOT NULL DEFAULT 0,
    notes           TEXT,
    shipped_at      TIMESTAMPTZ,
    delivered_at    TIMESTAMPTZ,
    cancelled_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  orders IS 'Customer orders — no soft delete, only status transitions';
COMMENT ON COLUMN orders.order_number IS 'Human-readable order identifier (e.g., ORD-2026-001)';
COMMENT ON COLUMN orders.total IS 'subtotal - discount + shipping_fee + tax — maintained by application layer';

CREATE TABLE order_items (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID            NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id      UUID            REFERENCES products(id) ON DELETE SET NULL,
    product_name    VARCHAR(255)    NOT NULL,
    product_sku     VARCHAR(100)    NOT NULL,
    quantity        INTEGER         NOT NULL CHECK (quantity > 0),
    unit_price      DECIMAL(19,4)   NOT NULL CHECK (unit_price >= 0),
    discount        DECIMAL(19,4)   NOT NULL DEFAULT 0,
    total           DECIMAL(19,4)   NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  order_items IS 'Order line items — product_name/sku denormalized to preserve historical data';
COMMENT ON COLUMN order_items.product_name IS 'Snapshot of product name at time of order — intentionally denormalized';
COMMENT ON COLUMN order_items.product_sku IS 'Snapshot of product SKU at time of order — intentionally denormalized';
