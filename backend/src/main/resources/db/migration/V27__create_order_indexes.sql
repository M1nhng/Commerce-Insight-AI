-- =============================================================================
-- V27 — Create order domain indexes
-- Commerce Insight AI
--
-- Performance indexes for the order domain tables.
-- Uses IF NOT EXISTS for idempotency.
-- =============================================================================

-- ── orders ─────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_orders_status          ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_customer_id     ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_payment_status  ON orders(payment_status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at      ON orders(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_status_date     ON orders(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_analytics       ON orders(created_at, status, total);

-- ── order_items ─────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_order_items_order_id   ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);

-- ── order_addresses ─────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_order_addr_order_id    ON order_addresses(order_id);
CREATE INDEX IF NOT EXISTS idx_order_addr_type        ON order_addresses(order_id, type);

-- ── order_status_history ────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_order_hist_order_id    ON order_status_history(order_id);
CREATE INDEX IF NOT EXISTS idx_order_hist_created_at  ON order_status_history(order_id, created_at ASC);

-- ── payments ────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_payments_order_id      ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_status        ON payments(status);
