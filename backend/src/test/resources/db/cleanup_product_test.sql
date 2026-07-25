-- Cleanup script for ProductControllerIntegrationTest
-- Removes test products so the test is idempotent on repeated runs
DELETE FROM inventory WHERE product_id IN (
    SELECT id FROM products WHERE sku IN ('IT-SKU-001')
);
DELETE FROM inventory_transactions WHERE product_id IN (
    SELECT id FROM products WHERE sku IN ('IT-SKU-001')
);
DELETE FROM products WHERE sku IN ('IT-SKU-001');
