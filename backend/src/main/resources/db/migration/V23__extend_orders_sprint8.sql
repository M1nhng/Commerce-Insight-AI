-- =============================================================================
-- V23 — Extend orders and order_items for Sprint 8
-- Commerce Insight AI
--
-- The V6 migration created a minimal orders + order_items schema.
-- This migration adds the full Sprint 8 fields:
--   - orders: payment_status, currency, completed_at, discount/tax renames
--   - order_items: sku_snapshot, product_name_snapshot, subtotal, discount_amount
--
-- REVERSAL: ALTER TABLE orders DROP COLUMN ...; ALTER TABLE order_items DROP COLUMN ...;
-- =============================================================================

-- ── orders ─────────────────────────────────────────────────────────────────

-- Add new order columns
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS currency       VARCHAR(10)    NOT NULL DEFAULT 'VND',
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(50)    NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS completed_at   TIMESTAMPTZ;

-- Add payment_status CHECK constraint
ALTER TABLE orders
    ADD CONSTRAINT chk_orders_payment_status
    CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED'));

-- Extend status CHECK to include COMPLETED
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;
ALTER TABLE orders
    ADD CONSTRAINT chk_orders_status
    CHECK (status IN ('PENDING','CONFIRMED','PROCESSING',
                      'SHIPPED','DELIVERED','COMPLETED','CANCELLED','REFUNDED'));

-- Add unique constraint on order_number
ALTER TABLE orders
    ADD CONSTRAINT uq_orders_order_number UNIQUE (order_number);

COMMENT ON COLUMN orders.currency        IS 'ISO 4217 currency code (default: VND)';
COMMENT ON COLUMN orders.payment_status  IS 'Payment lifecycle: PENDING, PAID, FAILED, REFUNDED';
COMMENT ON COLUMN orders.completed_at    IS 'Timestamp when order reached COMPLETED status';

-- ── order_items ─────────────────────────────────────────────────────────────

-- Add snapshot columns (sku_snapshot mirrors product_sku, product_name_snapshot mirrors product_name)
ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS sku_snapshot           VARCHAR(100),
    ADD COLUMN IF NOT EXISTS product_name_snapshot  VARCHAR(255),
    ADD COLUMN IF NOT EXISTS unit_price             DECIMAL(19,4),
    ADD COLUMN IF NOT EXISTS subtotal               DECIMAL(19,4),
    ADD COLUMN IF NOT EXISTS discount_amount        DECIMAL(19,4) NOT NULL DEFAULT 0;

-- Backfill snapshot columns from existing columns (idempotent)
UPDATE order_items
   SET sku_snapshot          = COALESCE(sku_snapshot, product_sku),
       product_name_snapshot = COALESCE(product_name_snapshot, product_name),
       unit_price            = COALESCE(unit_price, unit_price),
       subtotal              = COALESCE(subtotal, total)
 WHERE sku_snapshot IS NULL OR product_name_snapshot IS NULL;

COMMENT ON COLUMN order_items.sku_snapshot           IS 'Product SKU captured at order time — immutable historical snapshot';
COMMENT ON COLUMN order_items.product_name_snapshot  IS 'Product name captured at order time — immutable historical snapshot';
COMMENT ON COLUMN order_items.unit_price             IS 'Selling price per unit at time of order';
COMMENT ON COLUMN order_items.subtotal               IS 'quantity * unit_price - discount_amount';
COMMENT ON COLUMN order_items.discount_amount        IS 'Per-line-item discount amount';
