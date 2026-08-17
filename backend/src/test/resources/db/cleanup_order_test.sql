-- Cleanup order test data before integration tests run
-- Order: delete child tables first (FK constraints), then parent

DELETE FROM order_status_history WHERE order_id IN (SELECT id FROM orders WHERE order_number LIKE 'ORD-TEST-%');
DELETE FROM payments             WHERE order_id IN (SELECT id FROM orders WHERE order_number LIKE 'ORD-TEST-%');
DELETE FROM order_addresses      WHERE order_id IN (SELECT id FROM orders WHERE order_number LIKE 'ORD-TEST-%');
DELETE FROM order_items          WHERE order_id IN (SELECT id FROM orders WHERE order_number LIKE 'ORD-TEST-%');
DELETE FROM orders               WHERE order_number LIKE 'ORD-TEST-%';
