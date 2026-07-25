-- Cleanup script for InventoryControllerIntegrationTest
-- Removes test warehouse created during integration test runs
-- The warehouse has soft-delete (deleted_at) — but its unique code index
-- is a partial unique index (WHERE deleted_at IS NULL), so soft-deleting is sufficient.
-- However, to be safe we hard-delete since this is a test warehouse.

-- First remove any inventory/transactions linked to this test warehouse
DELETE FROM stock_adjustments WHERE warehouse_id IN (
    SELECT id FROM warehouses WHERE code = 'WH-EAST-TEST'
);
DELETE FROM inventory_transactions WHERE warehouse_id IN (
    SELECT id FROM warehouses WHERE code = 'WH-EAST-TEST'
);
DELETE FROM inventory WHERE warehouse_id IN (
    SELECT id FROM warehouses WHERE code = 'WH-EAST-TEST'
);
DELETE FROM warehouses WHERE code = 'WH-EAST-TEST';
