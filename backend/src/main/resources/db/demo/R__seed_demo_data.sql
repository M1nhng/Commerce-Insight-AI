-- =============================================================================
-- R__seed_demo_data.sql  — REPEATABLE FLYWAY MIGRATION, DEMO PROFILE ONLY
-- =============================================================================
-- Loaded ONLY when spring.flyway.locations includes classpath:db/demo, which is
-- set exclusively by application-demo.yml (SPRING_PROFILES_ACTIVE=demo).
-- It NEVER runs under dev / test / e2e / prod — those profiles use
-- classpath:db/migration only. See docs/demo/SPRINT_13C_DEMO_DATA.md.
--
-- PURPOSE
--   Populate the EXISTING schema with a large, realistic, deterministic
--   ecommerce dataset so the EXISTING backend + REST APIs + React dashboard
--   show meaningful numbers with zero frontend/backend code changes.
--
-- DETERMINISM
--   * Every primary key is md5('demo-<domain>-<n>')::uuid — stable across runs.
--   * Every "random-looking" choice is a modulo of a deterministic md5 hash
--     (helper demo_h(text) below). No random(), no clock-derived IDs.
--   * Timestamps are relative to now() but bucketed, so trends are stable.
--
-- IDEMPOTENCY
--   * Every INSERT is `ON CONFLICT (id) DO NOTHING`.
--   * The one UPDATE (order totals) recomputes identical values and is scoped
--     to `order_number LIKE 'DEMO-%'`.
--   * Running the seed twice produces the SAME row counts — no growth.
--   Flyway re-runs a repeatable migration only when this file's checksum
--   changes; the guarantees above make that safe too.
--
-- ISOLATION FROM REAL DATA
--   All demo rows carry an obvious marker: emails @commerceinsight.demo,
--   codes/SKUs/order-numbers prefixed DEMO-/DEMO_. Nothing here touches or
--   depends on rows created by the running application.
-- =============================================================================

-- ── Deterministic hash helper (dropped at the end of this migration) ─────────
CREATE OR REPLACE FUNCTION demo_h(seed text)
RETURNS bigint
LANGUAGE sql IMMUTABLE PARALLEL SAFE AS
$$ SELECT ('x' || substr(md5(seed), 1, 8))::bit(32)::bigint $$;

-- =============================================================================
-- 1. DEMO USERS  (ADMIN / MANAGER / STAFF) — hashed with BCrypt ($2a$10$)
--    DEMO ONLY — credentials are documented in docs/demo/SPRINT_13C_DEMO_DATA.md
-- =============================================================================
INSERT INTO users (id, email, password_hash, first_name, last_name, role,
                   active, locked, failed_attempts, created_at, updated_at)
VALUES
  (md5('demo-user-admin')::uuid,   'demo-admin@commerceinsight.demo',
   '$2a$10$4AYWn8vNzBKTxeqd7MAk0Ow.iwNnwbjyLi26OyfHty7hdm84hC9BO',
   'Demo', 'Admin',   'ADMIN',   TRUE, FALSE, 0, now() - interval '400 days', now()),
  (md5('demo-user-manager')::uuid, 'demo-manager@commerceinsight.demo',
   '$2a$10$Noa0WbErYbwfwg0Kl/lqeuIkVpsBJWXN5IMpeUFHOfFsYWtZPMcVm',
   'Demo', 'Manager', 'MANAGER', TRUE, FALSE, 0, now() - interval '400 days', now()),
  (md5('demo-user-staff')::uuid,   'demo-staff@commerceinsight.demo',
   '$2a$10$dRKfqf9UC41y8AVaEQtRee9pqYteDDjGvruiScybIh9rw86OuwG72',
   'Demo', 'Staff',   'STAFF',   TRUE, FALSE, 0, now() - interval '400 days', now())
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 2. CUSTOMER GROUPS  (5)
-- =============================================================================
INSERT INTO customer_groups (id, code, name, description, status, created_at, updated_at)
VALUES
  (md5('demo-cg-VIP')::uuid,       'DEMO_VIP',       'VIP',        'High-value repeat customers',        'ACTIVE',   now() - interval '395 days', now()),
  (md5('demo-cg-WHOLESALE')::uuid, 'DEMO_WHOLESALE', 'Wholesale',  'Bulk buyers with negotiated pricing','ACTIVE',   now() - interval '395 days', now()),
  (md5('demo-cg-RETAIL')::uuid,    'DEMO_RETAIL',    'Retail',     'Standard individual shoppers',        'ACTIVE',   now() - interval '395 days', now()),
  (md5('demo-cg-CORPORATE')::uuid, 'DEMO_CORPORATE', 'Corporate',  'Company accounts and procurement',    'ACTIVE',   now() - interval '395 days', now()),
  (md5('demo-cg-DORMANT')::uuid,   'DEMO_DORMANT',   'Dormant',    'No purchase in the last 12 months',   'INACTIVE', now() - interval '395 days', now())
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 3. CUSTOMER SEGMENTS  (6) — reference data (no FK from customers in schema)
-- =============================================================================
INSERT INTO customer_segments (id, code, name, description, type, status, created_at, updated_at)
VALUES
  (md5('demo-seg-CHAMPIONS')::uuid,     'DEMO_CHAMPIONS',     'Champions',        'Recent, frequent, high spend',     'RULE_BASED',   'ACTIVE', now() - interval '390 days', now()),
  (md5('demo-seg-LOYAL')::uuid,         'DEMO_LOYAL',         'Loyal Customers',  'Consistent repeat purchasers',      'RULE_BASED',   'ACTIVE', now() - interval '390 days', now()),
  (md5('demo-seg-POTENTIAL')::uuid,     'DEMO_POTENTIAL',     'Potential Loyalist','Recent buyers, growing frequency', 'RULE_BASED',   'ACTIVE', now() - interval '390 days', now()),
  (md5('demo-seg-ATRISK')::uuid,        'DEMO_ATRISK',        'At Risk',          'Previously active, now slowing',    'RULE_BASED',   'ACTIVE', now() - interval '390 days', now()),
  (md5('demo-seg-HIBERNATING')::uuid,   'DEMO_HIBERNATING',   'Hibernating',      'Long time since last order',        'RULE_BASED',   'ACTIVE', now() - interval '390 days', now()),
  (md5('demo-seg-NEW')::uuid,           'DEMO_NEW',           'New Customers',    'First order in the last 30 days',   'MANUAL',       'ACTIVE', now() - interval '390 days', now())
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 4. WAREHOUSES  (3 demo; "Main Warehouse" already seeded by V14)
-- =============================================================================
INSERT INTO warehouses (id, name, code, address, city, country, active, created_at, updated_at)
VALUES
  (md5('demo-wh-HAN')::uuid, 'Hanoi Warehouse',        'DEMO-WH-HAN', '18 Pham Hung',        'Hanoi',              'Vietnam', TRUE, now() - interval '380 days', now()),
  (md5('demo-wh-SGN')::uuid, 'Ho Chi Minh Warehouse',  'DEMO-WH-SGN', '72 Le Thanh Ton',     'Ho Chi Minh City',   'Vietnam', TRUE, now() - interval '380 days', now()),
  (md5('demo-wh-DAD')::uuid, 'Da Nang Warehouse',      'DEMO-WH-DAD', '255 Nguyen Van Linh', 'Da Nang',            'Vietnam', TRUE, now() - interval '380 days', now())
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 5. CATEGORIES  (5 parents + 9 children = 14)
-- =============================================================================
INSERT INTO categories (id, name, slug, description, parent_id, sort_order, active, created_at, updated_at)
VALUES
  (md5('demo-cat-electronics')::uuid, 'Electronics',        'demo-electronics',        'Consumer electronics',            NULL, 1, TRUE, now() - interval '370 days', now()),
  (md5('demo-cat-home')::uuid,        'Home & Living',      'demo-home-living',        'Homeware and appliances',         NULL, 2, TRUE, now() - interval '370 days', now()),
  (md5('demo-cat-fashion')::uuid,     'Fashion',            'demo-fashion',            'Apparel and accessories',         NULL, 3, TRUE, now() - interval '370 days', now()),
  (md5('demo-cat-office')::uuid,      'Office',             'demo-office',             'Office and stationery',           NULL, 4, TRUE, now() - interval '370 days', now()),
  (md5('demo-cat-sports')::uuid,      'Sports & Outdoors',  'demo-sports-outdoors',    'Sports and outdoor equipment',    NULL, 5, TRUE, now() - interval '370 days', now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO categories (id, name, slug, description, parent_id, sort_order, active, created_at, updated_at)
VALUES
  (md5('demo-cat-smartphones')::uuid, 'Smartphones',        'demo-smartphones',        'Mobile phones',        md5('demo-cat-electronics')::uuid, 1, TRUE, now() - interval '365 days', now()),
  (md5('demo-cat-laptops')::uuid,     'Laptops',            'demo-laptops',            'Notebooks and ultrabooks', md5('demo-cat-electronics')::uuid, 2, TRUE, now() - interval '365 days', now()),
  (md5('demo-cat-audio')::uuid,       'Audio',              'demo-audio',              'Headphones and speakers', md5('demo-cat-electronics')::uuid, 3, TRUE, now() - interval '365 days', now()),
  (md5('demo-cat-kitchen')::uuid,     'Kitchen Appliances', 'demo-kitchen-appliances', 'Small kitchen appliances', md5('demo-cat-home')::uuid, 1, TRUE, now() - interval '365 days', now()),
  (md5('demo-cat-furniture')::uuid,   'Furniture',          'demo-furniture',          'Home and office furniture', md5('demo-cat-home')::uuid, 2, TRUE, now() - interval '365 days', now()),
  (md5('demo-cat-menswear')::uuid,    'Menswear',           'demo-menswear',           'Men clothing',           md5('demo-cat-fashion')::uuid, 1, TRUE, now() - interval '365 days', now()),
  (md5('demo-cat-womenswear')::uuid,  'Womenswear',         'demo-womenswear',         'Women clothing',         md5('demo-cat-fashion')::uuid, 2, TRUE, now() - interval '365 days', now()),
  (md5('demo-cat-stationery')::uuid,  'Stationery',         'demo-stationery',         'Pens, paper, desk supplies', md5('demo-cat-office')::uuid, 1, TRUE, now() - interval '365 days', now()),
  (md5('demo-cat-camping')::uuid,     'Camping Gear',       'demo-camping-gear',       'Tents, packs, outdoor kit', md5('demo-cat-sports')::uuid, 1, TRUE, now() - interval '365 days', now())
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 6. PRODUCTS  (80) — priced in VND, distributed across the 14 demo categories
-- =============================================================================
INSERT INTO products (id, sku, name, description, price, cost_price, category_id, active, created_at, updated_at)
SELECT
    md5('demo-product-' || n)::uuid,
    'DEMO-SKU-' || lpad(n::text, 4, '0'),
    (ARRAY['Aurora','Nimbus','Vertex','Lumina','Cobalt','Terra','Zephyr','Onyx','Halcyon','Pulse',
           'Meridian','Ember','Cascade','Atlas','Nova','Quartz'])[1 + (demo_h('demo-prodbrand-' || n) % 16)]
      || ' ' ||
    (ARRAY['Pro','Max','Lite','Air','Plus','Edge','Go','One','Studio','Neo'])[1 + (demo_h('demo-prodline-' || n) % 10)]
      || ' ' ||
    (ARRAY['Smartphone','Ultrabook','Headphones','Blender','Desk Chair','Jacket','Dress','Notebook Set','Tent','Speaker',
           'Monitor','Kettle','Backpack','Sneakers','Lamp'])[1 + (demo_h('demo-prodnoun-' || n) % 15)],
    'Deterministic demo product #' || n || '. Generated by R__seed_demo_data.sql for dashboard showcases.',
    price_vnd,
    round(price_vnd * 0.62, 2),
    cat.id,
    (n % 41 <> 0),                                   -- 1 of 80 inactive (n=41)
    now() - ((360 - (n * 4)) || ' days')::interval,  -- creation spread over ~360d
    now()
FROM generate_series(1, 80) AS g(n)
CROSS JOIN LATERAL (
    SELECT CASE
             WHEN demo_h('demo-prodtier-' || g.n) % 100 < 55
                  THEN 150000  + (demo_h('demo-prodprice-' || g.n) % 1850) * 1000     -- mid    150k-2M
             WHEN demo_h('demo-prodtier-' || g.n) % 100 < 82
                  THEN 25000   + (demo_h('demo-prodprice-' || g.n) % 125)  * 1000     -- budget 25k-150k
             ELSE 2500000 + (demo_h('demo-prodprice-' || g.n) % 275)  * 100000        -- premium 2.5M-30M
           END::numeric AS price_vnd
) AS pr
JOIN (
    SELECT id, row_number() OVER (ORDER BY slug) AS rn
    FROM categories WHERE slug LIKE 'demo-%'
) AS cat ON cat.rn = 1 + (demo_h('demo-prodcat-' || g.n) % 14)
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 7. INVENTORY  (one row per product — schema enforces uq_inventory_product)
--    Distributed across the 4 warehouses (Main + 3 demo). Realistic mix of
--    healthy / low / zero / overstock. available = quantity - reserved >= 0.
-- =============================================================================
INSERT INTO inventory (id, product_id, warehouse_id, quantity, reserved_quantity,
                       low_stock_threshold, created_at, updated_at)
SELECT
    md5('demo-inv-' || g.n)::uuid,
    md5('demo-product-' || g.n)::uuid,                            -- FK: products seeded in step 6
    wh.id,
    q.quantity,
    LEAST(q.quantity, (demo_h('demo-invres-' || g.n) % 7)),       -- reserved never exceeds on-hand
    thr.threshold,
    now() - interval '250 days',
    now()
FROM generate_series(1, 80) AS g(n)
JOIN LATERAL (
    SELECT id, row_number() OVER (ORDER BY code) AS rn
    FROM warehouses
    WHERE code IN ('WH-MAIN', 'DEMO-WH-HAN', 'DEMO-WH-SGN', 'DEMO-WH-DAD')
) AS wh ON wh.rn = 1 + (demo_h('demo-invwh-' || g.n) % 4)
CROSS JOIN LATERAL (
    SELECT CASE (demo_h('demo-invthr-' || g.n) % 3) WHEN 0 THEN 5 WHEN 1 THEN 10 ELSE 20 END AS threshold
) AS thr
CROSS JOIN LATERAL (
    SELECT CASE
             WHEN demo_h('demo-invbucket-' || g.n) % 100 < 8   THEN 0                                                  -- ~8% out of stock
             WHEN demo_h('demo-invbucket-' || g.n) % 100 < 24  THEN 1 + (demo_h('demo-invqty-' || g.n) % thr.threshold)      -- ~16% low
             WHEN demo_h('demo-invbucket-' || g.n) % 100 < 90  THEN thr.threshold + 15 + (demo_h('demo-invqty-' || g.n) % 400)  -- healthy
             ELSE 600 + (demo_h('demo-invqty-' || g.n) % 1600)                                                         -- ~10% overstock
           END AS quantity
) AS q
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 8. CUSTOMERS  (200)
-- =============================================================================
INSERT INTO customers (id, first_name, last_name, email, phone, city, country,
                       postal_code, customer_code, gender, status, group_id,
                       date_of_birth, active, notes, created_at, updated_at)
SELECT
    md5('demo-customer-' || n)::uuid,
    (ARRAY['An','Binh','Chi','Dung','Duc','Giang','Ha','Hoa','Hung','Khanh',
           'Lan','Linh','Mai','Minh','Nam','Nga','Phong','Quan','Thu','Trang'])[1 + (demo_h('demo-custfn-' || n) % 20)],
    (ARRAY['Nguyen','Tran','Le','Pham','Hoang','Vu','Dang','Bui','Do','Ho',
           'Ngo','Duong','Ly','Vo','Phan','Truong','Dinh','Mai','Chau','Lam'])[1 + (demo_h('demo-custln-' || n) % 20)],
    'demo.customer' || n || '@commerceinsight.demo',
    '09' || lpad((demo_h('demo-custph-' || n) % 100000000)::text, 8, '0'),
    (ARRAY['Hanoi','Ho Chi Minh City','Da Nang','Hai Phong','Can Tho'])[1 + (demo_h('demo-custcity-' || n) % 5)],
    'Vietnam',
    lpad((demo_h('demo-custzip-' || n) % 900000 + 100000)::text, 6, '0'),
    'DEMO-CUST-' || lpad(n::text, 5, '0'),
    (ARRAY['MALE','FEMALE','OTHER'])[1 + (demo_h('demo-custg-' || n) % 3)],
    CASE
        WHEN demo_h('demo-custst-' || n) % 100 < 82 THEN 'ACTIVE'
        WHEN demo_h('demo-custst-' || n) % 100 < 94 THEN 'INACTIVE'
        ELSE 'BLOCKED'
    END,
    CASE WHEN demo_h('demo-custgrp-' || n) % 10 < 7
         THEN (SELECT id FROM customer_groups WHERE code =
                 (ARRAY['DEMO_VIP','DEMO_WHOLESALE','DEMO_RETAIL','DEMO_CORPORATE','DEMO_DORMANT'])
                 [1 + (demo_h('demo-custgrp2-' || n) % 5)])
         ELSE NULL END,
    DATE '1965-01-01' + (demo_h('demo-custdob-' || n) % 16000)::int,
    (demo_h('demo-custst-' || n) % 100 < 94),          -- active flag ~ mirrors non-BLOCKED
    NULL,
    now() - ((demo_h('demo-custcr-' || n) % 360 + 25) || ' days')::interval,
    now()
FROM generate_series(1, 200) AS n
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 9. CUSTOMER ADDRESSES  (200 default SHIPPING + ~120 BILLING)
-- =============================================================================
INSERT INTO customer_addresses (id, customer_id, type, recipient_name, phone,
                                address_line, ward, district, province, country,
                                is_default, created_at, updated_at)
SELECT
    md5('demo-caddr-ship-' || n)::uuid,
    c.id, 'SHIPPING',
    c.first_name || ' ' || c.last_name,
    c.phone,
    (demo_h('demo-caddrline-' || n) % 400 + 1) || ' ' ||
      (ARRAY['Le Loi','Nguyen Hue','Tran Hung Dao','Ba Trieu','Hai Ba Trung',
             'Dien Bien Phu','Cach Mang Thang Tam','Vo Van Tan','Nguyen Trai','Pham Ngu Lao'])
      [1 + (demo_h('demo-caddrst-' || n) % 10)],
    'Ward ' || (demo_h('demo-caddrw-' || n) % 20 + 1),
    'District ' || (demo_h('demo-caddrd-' || n) % 12 + 1),
    c.city,
    'VN',
    TRUE,
    c.created_at, now()
FROM generate_series(1, 200) AS n
JOIN customers c ON c.customer_code = 'DEMO-CUST-' || lpad(n::text, 5, '0')
ON CONFLICT (id) DO NOTHING;

INSERT INTO customer_addresses (id, customer_id, type, recipient_name, phone,
                                address_line, ward, district, province, country,
                                is_default, created_at, updated_at)
SELECT
    md5('demo-caddr-bill-' || n)::uuid,
    c.id, 'BILLING',
    c.first_name || ' ' || c.last_name,
    c.phone,
    (demo_h('demo-caddrbline-' || n) % 400 + 1) || ' ' ||
      (ARRAY['Ly Thuong Kiet','Hoang Dieu','Quang Trung','Le Duan','Nguyen Thi Minh Khai'])
      [1 + (demo_h('demo-caddrbst-' || n) % 5)],
    'Ward ' || (demo_h('demo-caddrbw-' || n) % 20 + 1),
    'District ' || (demo_h('demo-caddrbd-' || n) % 12 + 1),
    c.city,
    'VN',
    FALSE,
    c.created_at, now()
FROM generate_series(1, 200) AS n
JOIN customers c ON c.customer_code = 'DEMO-CUST-' || lpad(n::text, 5, '0')
WHERE demo_h('demo-caddrbill-' || n) % 10 < 6
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 10. ORDERS  (600) — over the last ~12 months, weighted toward recent months.
--     Totals are filled in after order_items exist (step 10c).
-- =============================================================================

DROP TABLE IF EXISTS _demo_order_gen;
CREATE TEMP TABLE _demo_order_gen AS
SELECT
    n,
    md5('demo-order-' || n)::uuid                                             AS order_id,
    cust.id                                                                    AS customer_id,
    cust.first_name || ' ' || cust.last_name                                   AS cust_name,
    cust.phone                                                                 AS cust_phone,
    cust.city                                                                  AS cust_city,
    c.created_at,
    s.status,
    f.payment_status,
    f.item_count,
    f.shipping_fee,
    f.pay_method,
    'DEMO-' || to_char(c.created_at, 'YYYYMM') || '-' || lpad(n::text, 5, '0') AS order_number
FROM generate_series(1, 600) AS n
CROSS JOIN LATERAL (
    SELECT
        floor(11 * power((demo_h('demo-ordwhen-' || n) % 100000)::numeric / 100000.0, 1.7))::int AS months_back,
        demo_h('demo-ordst-'   || n) % 100 AS sbucket,
        demo_h('demo-orditem-' || n)       AS hitem,
        demo_h('demo-ordmisc-' || n)       AS hmisc,
        demo_h('demo-ordpay-'  || n)       AS hpay
) AS b
CROSS JOIN LATERAL (
    SELECT (date_trunc('month', now())
            - (b.months_back || ' months')::interval
            + ((b.hmisc % 27) || ' days')::interval
            + ((b.hmisc % 20) || ' hours')::interval) AS created_at
) AS c
CROSS JOIN LATERAL (
    SELECT CASE
        WHEN b.months_back >= 3 THEN
            CASE WHEN b.sbucket < 86 THEN 'COMPLETED'
                 WHEN b.sbucket < 95 THEN 'CANCELLED'
                 ELSE 'REFUNDED' END
        WHEN b.months_back >= 1 THEN
            CASE WHEN b.sbucket < 50 THEN 'COMPLETED'
                 WHEN b.sbucket < 70 THEN 'DELIVERED'
                 WHEN b.sbucket < 82 THEN 'SHIPPED'
                 WHEN b.sbucket < 90 THEN 'PROCESSING'
                 ELSE 'CANCELLED' END
        ELSE
            CASE WHEN b.sbucket < 22 THEN 'PENDING'
                 WHEN b.sbucket < 44 THEN 'CONFIRMED'
                 WHEN b.sbucket < 64 THEN 'PROCESSING'
                 WHEN b.sbucket < 80 THEN 'SHIPPED'
                 WHEN b.sbucket < 92 THEN 'DELIVERED'
                 ELSE 'CANCELLED' END
    END AS status
) AS s
CROSS JOIN LATERAL (
    SELECT
        CASE
            WHEN s.status IN ('COMPLETED','DELIVERED','SHIPPED') THEN 'PAID'
            WHEN s.status = 'REFUNDED'                           THEN 'REFUNDED'
            WHEN s.status = 'CANCELLED'                          THEN 'FAILED'
            ELSE 'PENDING'
        END                                                                    AS payment_status,
        1 + (b.hitem % 5)                                                       AS item_count,
        (ARRAY[0, 15000, 30000, 45000])[1 + (b.hmisc % 4)]::numeric            AS shipping_fee,
        (ARRAY['CASH','BANK_TRANSFER','BANK_TRANSFER','CARD','CARD','CASH','OTHER'])[1 + (b.hpay % 7)] AS pay_method
) AS f
JOIN customers cust ON cust.customer_code =
     'DEMO-CUST-' || lpad((1 + (demo_h('demo-ordcust-' || n) % 200))::text, 5, '0');

-- 10a. Orders — placeholder subtotal/tax/discount, real shipping_fee + timestamps
INSERT INTO orders (id, order_number, customer_id, status, subtotal, discount,
                    shipping_fee, tax, total, currency, payment_status, notes,
                    created_at, updated_at, shipped_at, delivered_at, completed_at, cancelled_at)
SELECT
    order_id, order_number, customer_id, status,
    0, 0, shipping_fee, 0, shipping_fee,
    'VND', payment_status, 'Demo seed order',
    created_at, created_at,
    CASE WHEN status IN ('SHIPPED','DELIVERED','COMPLETED') THEN created_at + interval '2 days' END,
    CASE WHEN status IN ('DELIVERED','COMPLETED')           THEN created_at + interval '4 days' END,
    CASE WHEN status = 'COMPLETED'                          THEN created_at + interval '5 days' END,
    CASE WHEN status = 'CANCELLED'                          THEN created_at + interval '1 day'  END
FROM _demo_order_gen
ON CONFLICT (id) DO NOTHING;

-- 10b. Order items — 1..5 distinct products per order (offset 17 is coprime with 80)
INSERT INTO order_items (id, order_id, product_id, product_name, product_sku,
                         quantity, unit_price, discount, total,
                         sku_snapshot, product_name_snapshot, subtotal, discount_amount,
                         created_at, updated_at)
SELECT
    md5('demo-oi-' || g.n || '-' || k)::uuid,
    g.order_id,
    p.id,
    p.name, p.sku,
    qty.q,
    p.price,
    0,
    qty.q * p.price,
    p.sku, p.name,
    qty.q * p.price,
    0,
    g.created_at, g.created_at
FROM _demo_order_gen g
CROSS JOIN LATERAL generate_series(1, g.item_count) AS k
CROSS JOIN LATERAL (SELECT 1 + (demo_h('demo-oiqty-' || g.n || '-' || k) % 4) AS q) AS qty
JOIN LATERAL (
    SELECT id, name, sku, price
    FROM products
    WHERE sku = 'DEMO-SKU-' || lpad(
        (1 + ((demo_h('demo-oibase-' || g.n) + (k - 1) * 17) % 80))::text, 4, '0')
) AS p ON TRUE
ON CONFLICT (id) DO NOTHING;

-- 10c. Fill order money fields from the line items (deterministic, idempotent).
--      Note: an UPDATE ... FROM cannot reference the target table (o) inside a
--      FROM-list subquery, so the discount/tax are precomputed in CTEs keyed by
--      order_id, and only the SET clause references o.shipping_fee.
WITH lines AS (
    SELECT oi.order_id, SUM(oi.total) AS sub
    FROM order_items oi
    JOIN _demo_order_gen g ON g.order_id = oi.order_id
    GROUP BY oi.order_id
),
calc AS (
    SELECT l.order_id,
           l.sub,
           CASE WHEN demo_h('demo-orddisc-' || l.order_id::text) % 100 < 18
                THEN round(l.sub * 0.05) ELSE 0 END AS disc
    FROM lines l
),
calc2 AS (
    SELECT c.order_id, c.sub, c.disc,
           round((c.sub - c.disc) * 0.08) AS tax
    FROM calc c
)
UPDATE orders o
SET subtotal = c.sub,
    discount = c.disc,
    tax      = c.tax,
    total    = c.sub - c.disc + o.shipping_fee + c.tax
FROM calc2 c
WHERE o.id = c.order_id
  AND o.order_number LIKE 'DEMO-%';

-- 10d. Payments — one per order, amount mirrors the order total
INSERT INTO payments (id, order_id, method, status, amount, currency, reference,
                      paid_at, notes, created_at, updated_at)
SELECT
    md5('demo-pay-' || g.n)::uuid,
    g.order_id,
    g.pay_method,
    CASE g.payment_status
        WHEN 'PAID'     THEN 'PAID'
        WHEN 'REFUNDED' THEN 'REFUNDED'
        WHEN 'FAILED'   THEN 'FAILED'
        ELSE 'PENDING'
    END,
    o.total,
    'VND',
    CASE WHEN g.pay_method = 'BANK_TRANSFER'
         THEN 'TXN-' || upper(substr(md5('demo-pay-' || g.n), 1, 12)) END,
    CASE WHEN g.payment_status = 'PAID' THEN g.created_at + interval '1 day' END,
    'Demo seed payment',
    g.created_at, g.created_at
FROM _demo_order_gen g
JOIN orders o ON o.id = g.order_id
ON CONFLICT (id) DO NOTHING;

-- 10e. Order status history — creation row always; one transition row otherwise
INSERT INTO order_status_history (id, order_id, from_status, to_status, changed_by, reason, created_at)
SELECT
    md5('demo-osh-' || g.n || '-0')::uuid, g.order_id, NULL, 'PENDING',
    (SELECT id FROM users WHERE email = 'demo-staff@commerceinsight.demo'),
    'Order created (demo seed)', g.created_at
FROM _demo_order_gen g
ON CONFLICT (id) DO NOTHING;

INSERT INTO order_status_history (id, order_id, from_status, to_status, changed_by, reason, created_at)
SELECT
    md5('demo-osh-' || g.n || '-1')::uuid, g.order_id, 'PENDING', g.status,
    (SELECT id FROM users WHERE email = 'demo-manager@commerceinsight.demo'),
    'Advanced to ' || g.status || ' (demo seed — condensed history)', g.created_at + interval '6 hours'
FROM _demo_order_gen g
WHERE g.status <> 'PENDING'
ON CONFLICT (id) DO NOTHING;

-- 10f. Order address snapshots — SHIPPING + BILLING per order
INSERT INTO order_addresses (id, order_id, type, recipient_name, phone,
                             address_line, ward, district, province, country, created_at)
SELECT
    md5('demo-oaddr-' || g.n || '-S')::uuid, g.order_id, 'SHIPPING',
    g.cust_name, g.cust_phone,
    (demo_h('demo-oaddr-' || g.n) % 400 + 1) || ' ' ||
      (ARRAY['Le Loi','Nguyen Hue','Tran Hung Dao','Ba Trieu','Hai Ba Trung',
             'Dien Bien Phu','Vo Van Tan','Nguyen Trai'])[1 + (demo_h('demo-oaddrst-' || g.n) % 8)],
    'Ward ' || (demo_h('demo-oaddrw-' || g.n) % 20 + 1),
    'District ' || (demo_h('demo-oaddrd-' || g.n) % 12 + 1),
    g.cust_city, 'Vietnam', g.created_at
FROM _demo_order_gen g
ON CONFLICT (id) DO NOTHING;

INSERT INTO order_addresses (id, order_id, type, recipient_name, phone,
                             address_line, ward, district, province, country, created_at)
SELECT
    md5('demo-oaddr-' || g.n || '-B')::uuid, g.order_id, 'BILLING',
    g.cust_name, g.cust_phone,
    (demo_h('demo-oaddrb-' || g.n) % 400 + 1) || ' ' ||
      (ARRAY['Ly Thuong Kiet','Hoang Dieu','Quang Trung','Le Duan','Nguyen Thi Minh Khai'])
      [1 + (demo_h('demo-oaddrbst-' || g.n) % 5)],
    'Ward ' || (demo_h('demo-oaddrbw-' || g.n) % 20 + 1),
    'District ' || (demo_h('demo-oaddrbd-' || g.n) % 12 + 1),
    g.cust_city, 'Vietnam', g.created_at
FROM _demo_order_gen g
ON CONFLICT (id) DO NOTHING;

DROP TABLE IF EXISTS _demo_order_gen;

-- =============================================================================
-- 11. IMPORT JOBS (20) + IMPORT ERRORS  — populates the Import Management UI
-- =============================================================================
INSERT INTO import_jobs (id, file_name, file_type, import_type, status,
                         total_rows, successful_rows, failed_rows,
                         started_at, completed_at, created_at, created_by)
SELECT
    md5('demo-import-' || n)::uuid,
    lower((ARRAY['products','customers','orders'])[1 + (demo_h('demo-imptype-' || n) % 3)])
      || '_batch_' || lpad(n::text, 3, '0')
      || CASE WHEN demo_h('demo-impfile-' || n) % 2 = 0 THEN '.csv' ELSE '.xlsx' END,
    CASE WHEN demo_h('demo-impfile-' || n) % 2 = 0 THEN 'CSV' ELSE 'XLSX' END,
    (ARRAY['PRODUCT','CUSTOMER','ORDER'])[1 + (demo_h('demo-imptype-' || n) % 3)],
    st.status,
    st.total_rows, st.ok_rows, st.fail_rows,
    st.created_at, st.created_at + interval '3 minutes', st.created_at,
    (SELECT id FROM users WHERE email = 'demo-manager@commerceinsight.demo')
FROM generate_series(1, 20) AS n
CROSS JOIN LATERAL (
    SELECT
        now() - ((demo_h('demo-impwhen-' || n) % 180 + 2) || ' days')::interval AS created_at,
        50 + (demo_h('demo-improws-' || n) % 450)                               AS total_rows,
        CASE WHEN n <= 12 THEN 'COMPLETED'
             WHEN n <= 17 THEN 'PARTIAL_SUCCESS'
             ELSE 'FAILED' END                                                  AS status
) AS base
CROSS JOIN LATERAL (
    SELECT
        base.status,
        base.total_rows,
        base.created_at,
        CASE base.status
            WHEN 'COMPLETED'       THEN base.total_rows
            WHEN 'PARTIAL_SUCCESS' THEN base.total_rows - (1 + (demo_h('demo-impfail-' || n) % 15))
            ELSE 0
        END AS ok_rows,
        CASE base.status
            WHEN 'COMPLETED'       THEN 0
            WHEN 'PARTIAL_SUCCESS' THEN (1 + (demo_h('demo-impfail-' || n) % 15))
            ELSE base.total_rows
        END AS fail_rows
) AS st
ON CONFLICT (id) DO NOTHING;

INSERT INTO import_errors (id, import_job_id, row_number, field_name, raw_value,
                           error_code, error_message, created_at)
SELECT
    md5('demo-imperr-' || j.n || '-' || e)::uuid,
    j.id,
    e * 3 + 1,
    (ARRAY['email','sku','price','quantity','status','customer_code'])[1 + ((j.h + e) % 6)],
    'demo_bad_value_' || e,
    (ARRAY['VALIDATION_ERROR','DUPLICATE_KEY','MISSING_REQUIRED_FIELD','TYPE_MISMATCH','CONSTRAINT_VIOLATION'])
      [1 + ((j.h + e) % 5)],
    'Row ' || (e * 3 + 1) || ' rejected during demo import (' || j.import_type || ').',
    j.completed_at
FROM (
    SELECT ij.id,
           ij.import_type,
           ij.failed_rows,
           ij.completed_at,
           demo_h('demo-import-' || gs.n) AS h,
           gs.n
    FROM generate_series(1, 20) AS gs(n)
    JOIN import_jobs ij ON ij.id = md5('demo-import-' || gs.n)::uuid
    WHERE ij.status IN ('PARTIAL_SUCCESS', 'FAILED')
) AS j
CROSS JOIN LATERAL generate_series(1, LEAST(j.failed_rows, 8)) AS e
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 12. LOGIN HISTORY (40) — demo users only, deterministic mix of success/failure
-- =============================================================================
INSERT INTO login_history (id, user_id, email, ip_address, user_agent, success, failure_reason, created_at)
SELECT
    md5('demo-login-' || n)::uuid,
    u.id, u.email,
    '10.20.' || (demo_h('demo-loginip-' || n) % 254 + 1) || '.' || (demo_h('demo-loginip2-' || n) % 254 + 1),
    'Mozilla/5.0 (Demo Seed) CommerceInsight/1.0',
    (demo_h('demo-loginok-' || n) % 100 < 85),
    CASE WHEN demo_h('demo-loginok-' || n) % 100 < 85 THEN NULL
         ELSE (ARRAY['INVALID_CREDENTIALS','ACCOUNT_LOCKED','ACCOUNT_DISABLED'])[1 + (demo_h('demo-loginrs-' || n) % 3)] END,
    now() - ((demo_h('demo-loginwhen-' || n) % 45) || ' days')::interval
          - ((demo_h('demo-loginhr-' || n) % 24) || ' hours')::interval
FROM generate_series(1, 40) AS n
JOIN LATERAL (
    SELECT id, email FROM users
    WHERE email IN ('demo-admin@commerceinsight.demo','demo-manager@commerceinsight.demo','demo-staff@commerceinsight.demo')
    ORDER BY email
    OFFSET (demo_h('demo-loginu-' || n) % 3) LIMIT 1
) AS u ON TRUE
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 13. AUDIT LOGS (24) — demo users, deterministic business events
-- =============================================================================
INSERT INTO audit_logs (id, user_id, action, entity_type, entity_id, ip_address, user_agent, created_at)
SELECT
    md5('demo-audit-' || n)::uuid,
    (SELECT id FROM users WHERE email =
        (ARRAY['demo-admin@commerceinsight.demo','demo-manager@commerceinsight.demo','demo-staff@commerceinsight.demo'])
        [1 + (demo_h('demo-auditu-' || n) % 3)]),
    (ARRAY['PRODUCT_CREATED','PRODUCT_UPDATED','ORDER_STATUS_CHANGED','CUSTOMER_UPDATED',
           'INVENTORY_ADJUSTED','IMPORT_COMPLETED','USER_LOGIN'])[1 + (demo_h('demo-auditaction-' || n) % 7)],
    (ARRAY['Product','Order','Customer','Inventory','ImportJob'])[1 + (demo_h('demo-auditentity-' || n) % 5)],
    md5('demo-audit-entity-' || n)::uuid,
    '10.20.30.' || (demo_h('demo-auditip-' || n) % 254 + 1),
    'Mozilla/5.0 (Demo Seed) CommerceInsight/1.0',
    now() - ((demo_h('demo-auditwhen-' || n) % 90) || ' days')::interval
FROM generate_series(1, 24) AS n
ON CONFLICT (id) DO NOTHING;

-- ── Clean up the helper function — leaves no demo footprint in the schema ─────
DROP FUNCTION IF EXISTS demo_h(text);

-- =============================================================================
-- END R__seed_demo_data.sql
-- =============================================================================
