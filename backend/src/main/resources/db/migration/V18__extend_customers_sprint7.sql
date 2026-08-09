-- =============================================================================
-- V18 — Extend customers table for Sprint 7 Customer Domain
-- Commerce Insight AI
-- =============================================================================
-- Adds: customer_code, date_of_birth, gender, status, group_id
-- The customer_groups table is created in V19; the FK is added after.
-- =============================================================================

-- Step 1: Add customer_code (unique, required)
ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS customer_code VARCHAR(50);

-- Step 2: Backfill customer_code for any existing rows
UPDATE customers
SET customer_code = 'CUST-' || TO_CHAR(created_at, 'YYYYMM') || '-' || LPAD(FLOOR(RANDOM() * 99999 + 1)::TEXT, 5, '0')
WHERE customer_code IS NULL;

-- Step 3: Enforce NOT NULL + unique on customer_code
ALTER TABLE customers
    ALTER COLUMN customer_code SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_customers_code
    ON customers(customer_code)
    WHERE deleted_at IS NULL;

-- Step 4: Add date_of_birth
ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS date_of_birth DATE;

-- Step 5: Add gender
ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS gender VARCHAR(20)
        CHECK (gender IN ('MALE', 'FEMALE', 'OTHER'));

-- Step 6: Add status
ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS status VARCHAR(20)
        NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED'));

-- Step 7: Add group_id (FK added in V19 after customer_groups exists)
ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS group_id UUID;

-- Step 8: Unique index on email (partial, excluding deleted)
CREATE UNIQUE INDEX IF NOT EXISTS uq_customers_email
    ON customers(email)
    WHERE deleted_at IS NULL AND email IS NOT NULL;

COMMENT ON COLUMN customers.customer_code IS 'Unique human-readable code e.g. CUST-202607-00042';
COMMENT ON COLUMN customers.status IS 'Lifecycle status: ACTIVE | INACTIVE | BLOCKED';
COMMENT ON COLUMN customers.gender IS 'Gender: MALE | FEMALE | OTHER';
COMMENT ON COLUMN customers.group_id IS 'FK to customer_groups — nullable';
