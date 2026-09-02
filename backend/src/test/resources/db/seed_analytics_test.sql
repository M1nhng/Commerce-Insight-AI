-- =============================================================================
-- seed_analytics_test.sql — fixture for AnalyticsControllerIntegrationTest
-- =============================================================================
-- Small deterministic dataset (12 orders across 3 distinct calendar months)
-- so every analytics endpoint returns non-zero data against a real PostgreSQL.
-- Every row is marked ANALYTICS-TEST-* and removed by cleanup_analytics_test.sql.
-- Loaded via @Sql BEFORE_TEST_CLASS (after the cleanup script).
-- =============================================================================

-- ── 2 customers ─────────────────────────────────────────────────────────────
INSERT INTO customers (id, first_name, last_name, email, customer_code, status, active, created_at, updated_at)
VALUES
  (md5('analytics-test-cust-1')::uuid, 'Ana', 'Lytics', 'analytics-test-1@commerceinsight.test',
   'ANALYTICS-TEST-C1', 'ACTIVE', TRUE, now() - interval '120 days', now()),
  (md5('analytics-test-cust-2')::uuid, 'Bea', 'Chart',  'analytics-test-2@commerceinsight.test',
   'ANALYTICS-TEST-C2', 'ACTIVE', TRUE, now() - interval '120 days', now())
ON CONFLICT (id) DO NOTHING;

-- ── 3 products ──────────────────────────────────────────────────────────────
INSERT INTO products (id, sku, name, description, price, active, created_at, updated_at)
VALUES
  (md5('analytics-test-prod-1')::uuid, 'ANALYTICS-TEST-P1', 'Analytics Test Widget A', 'fixture', 100000.0000, TRUE, now() - interval '110 days', now()),
  (md5('analytics-test-prod-2')::uuid, 'ANALYTICS-TEST-P2', 'Analytics Test Widget B', 'fixture', 250000.0000, TRUE, now() - interval '110 days', now()),
  (md5('analytics-test-prod-3')::uuid, 'ANALYTICS-TEST-P3', 'Analytics Test Widget C', 'fixture', 1000000.0000, TRUE, now() - interval '110 days', now())
ON CONFLICT (id) DO NOTHING;

-- ── 12 orders across 3 months ───────────────────────────────────────────────
-- Revenue-eligible (COMPLETED/SHIPPED): n in {2,3,6,7,10,11}  → 6 orders, ≥1 per month
-- CANCELLED: n in {4,8,12} → 3    PENDING: n in {1,5,9} → 3
INSERT INTO orders (id, order_number, customer_id, status, subtotal, discount, shipping_fee, tax, total,
                    currency, payment_status, created_at, updated_at,
                    shipped_at, delivered_at, completed_at, cancelled_at)
SELECT
    md5('analytics-test-order-' || n)::uuid,
    'ANALYTICS-TEST-' || lpad(n::text, 3, '0'),
    (CASE WHEN n % 2 = 0 THEN md5('analytics-test-cust-1')::uuid ELSE md5('analytics-test-cust-2')::uuid END),
    st.status,
    st.subtotal, 0, 20000, st.tax, st.subtotal + 20000 + st.tax,
    'VND',
    (CASE st.status WHEN 'COMPLETED' THEN 'PAID' WHEN 'SHIPPED' THEN 'PAID'
                    WHEN 'CANCELLED' THEN 'FAILED' ELSE 'PENDING' END),
    st.created_at, st.created_at,
    (CASE WHEN st.status IN ('SHIPPED','COMPLETED') THEN st.created_at + interval '2 days' END),
    (CASE WHEN st.status = 'COMPLETED' THEN st.created_at + interval '4 days' END),
    (CASE WHEN st.status = 'COMPLETED' THEN st.created_at + interval '5 days' END),
    (CASE WHEN st.status = 'CANCELLED' THEN st.created_at + interval '1 day' END)
FROM generate_series(1, 12) AS g(n)
CROSS JOIN LATERAL (
    SELECT
        (CASE
            WHEN n IN (4, 8, 12) THEN 'CANCELLED'
            WHEN n IN (1, 5, 9)  THEN 'PENDING'
            WHEN n IN (10, 11)   THEN 'SHIPPED'
            ELSE 'COMPLETED'
         END) AS status,
        (CASE
            WHEN n <= 4  THEN date_trunc('month', now() AT TIME ZONE 'UTC') - interval '2 months' + interval '9 days'
            WHEN n <= 8  THEN date_trunc('month', now() AT TIME ZONE 'UTC') - interval '1 month'  + interval '9 days'
            ELSE              date_trunc('month', now() AT TIME ZONE 'UTC')                        + interval '9 days'
         END)::timestamptz AS created_at,
        -- 2 line items per order: P((n%3)+1) x (1+n%3)  +  P((n+1)%3+1) x 1
        ( (1 + (n % 3)) * (ARRAY[100000, 250000, 1000000])[1 + (n % 3)]
          + 1           * (ARRAY[100000, 250000, 1000000])[1 + ((n + 1) % 3)] )::numeric AS subtotal
) AS base
CROSS JOIN LATERAL (SELECT base.status, base.created_at, base.subtotal,
                           round(base.subtotal * 0.08) AS tax) AS st
ON CONFLICT (id) DO NOTHING;

-- ── order_items: exactly 2 per order ────────────────────────────────────────
INSERT INTO order_items (id, order_id, product_id, product_name, product_sku,
                         quantity, unit_price, discount, total,
                         sku_snapshot, product_name_snapshot, subtotal, discount_amount,
                         created_at, updated_at)
SELECT
    md5('analytics-test-oi-' || g.n || '-' || li.k)::uuid,
    md5('analytics-test-order-' || g.n)::uuid,
    p.id, p.name, p.sku,
    li.qty, p.price, 0, li.qty * p.price,
    p.sku, p.name, li.qty * p.price, 0,
    now() - interval '30 days', now() - interval '30 days'
FROM generate_series(1, 12) AS g(n)
CROSS JOIN LATERAL (
    VALUES
      (1, 1 + (g.n % 3),       1 + (g.n % 3)),
      (2, 1 + ((g.n + 1) % 3), 1)
) AS li(k, prod_idx, qty)
JOIN products p ON p.sku = 'ANALYTICS-TEST-P' || li.prod_idx
ON CONFLICT (id) DO NOTHING;

-- ── payments: one per order, amount = order total ──────────────────────────
INSERT INTO payments (id, order_id, method, status, amount, currency, created_at, updated_at)
SELECT
    md5('analytics-test-pay-' || o.order_number)::uuid,
    o.id,
    (ARRAY['CASH', 'CARD', 'BANK_TRANSFER'])[1 + (right(o.order_number, 3)::int % 3)],
    (CASE o.status WHEN 'COMPLETED' THEN 'PAID' WHEN 'SHIPPED' THEN 'PAID'
                   WHEN 'CANCELLED' THEN 'FAILED' ELSE 'PENDING' END),
    o.total, 'VND', o.created_at, o.created_at
FROM orders o
WHERE o.order_number LIKE 'ANALYTICS-TEST-%'
ON CONFLICT (id) DO NOTHING;
