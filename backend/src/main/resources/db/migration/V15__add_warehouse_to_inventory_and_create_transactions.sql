-- =============================================================================
-- V15 — Add Warehouse Support to Inventory + Create inventory_transactions
-- Commerce Insight AI
-- =============================================================================
-- Extends the existing `inventory` table with a warehouse_id column,
-- enabling multi-warehouse stock tracking.
-- Creates `inventory_transactions` as the authoritative audit trail for
-- all inventory changes (supersedes the simpler inventory_movements table).
-- =============================================================================

-- 1. Add warehouse_id to inventory table
--    Existing rows are assigned to the default Main Warehouse seeded in V14.
ALTER TABLE inventory
    ADD COLUMN warehouse_id UUID REFERENCES warehouses(id) ON DELETE RESTRICT;

-- Assign all existing inventory rows to the default warehouse
UPDATE inventory
SET warehouse_id = '00000000-0000-0000-0000-000000000001'
WHERE warehouse_id IS NULL;

-- Now enforce NOT NULL
ALTER TABLE inventory
    ALTER COLUMN warehouse_id SET NOT NULL;

-- Drop the old UNIQUE constraint on product_id alone
--   (one product can now have stock in multiple warehouses)
ALTER TABLE inventory
    DROP CONSTRAINT inventory_product_id_key;

-- New unique constraint: one row per (product, warehouse)
ALTER TABLE inventory
    ADD CONSTRAINT uq_inventory_product_warehouse UNIQUE (product_id, warehouse_id);

-- Performance indexes
CREATE INDEX idx_inventory_warehouse    ON inventory(warehouse_id);
CREATE INDEX idx_inventory_product      ON inventory(product_id);
CREATE INDEX idx_inventory_low_stock    ON inventory(quantity, low_stock_threshold);

-- 2. Create inventory_transactions table
--    Richer replacement for inventory_movements with TRANSFER support.
CREATE TABLE inventory_transactions (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_id    UUID         NOT NULL REFERENCES inventory(id) ON DELETE RESTRICT,
    product_id      UUID         NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    warehouse_id    UUID         NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    performed_by    UUID         REFERENCES users(id) ON DELETE SET NULL,
    type            VARCHAR(50)  NOT NULL
                    CHECK (type IN (
                        'PURCHASE',
                        'SALE',
                        'ADJUSTMENT',
                        'RETURN',
                        'DAMAGE',
                        'TRANSFER_IN',
                        'TRANSFER_OUT'
                    )),
    quantity        INTEGER      NOT NULL,      -- positive = in, negative = out
    quantity_before INTEGER      NOT NULL,
    quantity_after  INTEGER      NOT NULL,
    reference_id    UUID,                       -- links to order_id, adjustment_id, etc.
    notes           TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  inventory_transactions IS 'Authoritative audit trail for all inventory changes';
COMMENT ON COLUMN inventory_transactions.quantity IS 'Positive = stock added, Negative = stock removed';
COMMENT ON COLUMN inventory_transactions.reference_id IS 'Optional: order_id, stock_adjustment_id, etc.';
COMMENT ON COLUMN inventory_transactions.type IS 'TRANSFER_IN/OUT for warehouse-to-warehouse moves';

-- Indexes for performance
CREATE INDEX idx_inv_txn_inventory  ON inventory_transactions(inventory_id, created_at DESC);
CREATE INDEX idx_inv_txn_product    ON inventory_transactions(product_id, created_at DESC);
CREATE INDEX idx_inv_txn_warehouse  ON inventory_transactions(warehouse_id, created_at DESC);
CREATE INDEX idx_inv_txn_type       ON inventory_transactions(type);
CREATE INDEX idx_inv_txn_ref        ON inventory_transactions(reference_id) WHERE reference_id IS NOT NULL;
