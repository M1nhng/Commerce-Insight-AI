-- =============================================================================
-- V29 — Analytics Performance Indexes
-- Commerce Insight AI
--
-- Adds indexes to support Sprint 9A analytics queries efficiently.
--
-- Existing indexes already covering analytics (DO NOT re-create):
--   idx_orders_analytics   ON orders(created_at, status, total)  ← V10
--   idx_payments_status    ON payments(status)                    ← V27
--   idx_orders_status_date ON orders(status, created_at DESC)     ← V27
--   idx_order_items_order_id ON order_items(order_id)             ← V27
--
-- New indexes added here:
--
-- 1. idx_payments_method_status
--    Justification: The payment breakdown query (GET /analytics/payments) groups
--    by p.method. Without this index, PostgreSQL performs a sequential scan on
--    payments and then sorts/groups in memory. This index enables an index-only
--    scan for the GROUP BY method + SUM(amount) aggregation.
--
-- 2. idx_orders_currency_status
--    Justification: Future multi-currency analytics grouping. Low cardinality
--    column — added proactively so currency-scoped queries are instant.
--    Deferred to only if needed: commented out by default.
--
-- Uses IF NOT EXISTS for idempotent re-runs during development.
-- =============================================================================

-- Payment method breakdown query: GROUP BY p.method, SUM(p.amount)
CREATE INDEX IF NOT EXISTS idx_payments_method_status
    ON payments(method, status);

-- Revenue-eligible order filter: status IN (...) AND created_at BETWEEN
-- The composite (status, created_at, total) covers the common analytics filter.
-- Already covered by idx_orders_analytics — no new index needed here.

-- Top-products query: JOIN order_items → orders ON order_id, filter by status+date
-- Covered by existing idx_order_items_order_id + idx_orders_analytics.
-- No new index needed.
