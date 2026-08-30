-- =============================================================================
-- R__seed_e2e_commerce.sql  — REPEATABLE, E2E PROFILE ONLY
-- =============================================================================
-- Loaded ONLY when spring.flyway.locations includes classpath:db/e2e
-- (set in application-e2e.yml). NEVER runs under dev / test / prod.
--
-- Purpose: the minimum valid commerce graph needed to provoke a DETERMINISTIC
-- stock conflict on POST /api/v1/orders using the EXISTING backend rules
-- (OrderService -> OrderInventoryService -> InventoryService.reserveStock):
--
--   * one ACTIVE customer                (Order step 1: customer must be ACTIVE)
--   * one ACTIVE product                 (Order step 2: product must be active)
--   * one inventory row, quantity = 3    (available = quantity - reserved = 3)
--
-- An order for > 3 units of this product makes reserveStock() throw
-- BusinessRuleException(INSUFFICIENT_STOCK). The GlobalExceptionHandler maps
-- every BusinessRuleException to HTTP 422 UNPROCESSABLE_ENTITY (NOT 409) — that
-- is the pre-existing contract and Sprint 13B does not change business rules to
-- force a 409. The E2E assertion targets the real 422 + INSUFFICIENT_STOCK
-- envelope. See docs/security/SPRINT_13B_PRODUCTION_READINESS.md §7.
--
-- Idempotent: ON CONFLICT (id) DO NOTHING on first insert, then an explicit
-- UPDATE re-asserts the low-stock baseline on every run so repeated
-- `docker compose up` without `down -v` stays deterministic.
--
-- Fixed UUIDs (referenced verbatim by frontend/e2e/rate-limit-and-features.spec.ts):
--   customer  e2e00000-0000-0000-0000-000000000001
--   product   e2e00000-0000-0000-0000-000000000002
--   inventory e2e00000-0000-0000-0000-000000000004
--   warehouse 00000000-0000-0000-0000-000000000001  (Main Warehouse, seeded in V14)
-- =============================================================================

-- ── ACTIVE customer ──────────────────────────────────────────────────────────
INSERT INTO customers (id, first_name, last_name, email, customer_code, status, active)
VALUES ('e2e00000-0000-0000-0000-000000000001',
        'E2E', 'Commerce', 'e2e-commerce@commerceinsight.test',
        'CUST-E2E-00001', 'ACTIVE', TRUE)
ON CONFLICT (id) DO NOTHING;

-- ── ACTIVE product ───────────────────────────────────────────────────────────
INSERT INTO products (id, sku, name, description, price, active)
VALUES ('e2e00000-0000-0000-0000-000000000002',
        'E2E-LOWSTOCK-001', 'E2E Low-Stock Widget',
        'Deterministic low-stock product for the Playwright order-conflict scenario.',
        10.0000, TRUE)
ON CONFLICT (id) DO NOTHING;

-- ── Inventory row: only 3 units available in the default Main Warehouse ───────
INSERT INTO inventory (id, product_id, warehouse_id, quantity, reserved_quantity, low_stock_threshold)
VALUES ('e2e00000-0000-0000-0000-000000000004',
        'e2e00000-0000-0000-0000-000000000002',
        '00000000-0000-0000-0000-000000000001',
        3, 0, 10)
ON CONFLICT (id) DO NOTHING;

-- Re-assert the baseline every run (self-healing if a prior run reserved stock).
UPDATE inventory
   SET quantity          = 3,
       reserved_quantity  = 0,
       low_stock_threshold = 10,
       updated_at         = NOW()
 WHERE id = 'e2e00000-0000-0000-0000-000000000004';
