-- Cleanup analytics integration-test data (child tables first for FK safety).
-- Runs BEFORE_TEST_CLASS and AFTER_TEST_CLASS for AnalyticsControllerIntegrationTest.

DELETE FROM payments      WHERE order_id IN (SELECT id FROM orders WHERE order_number LIKE 'ANALYTICS-TEST-%');
DELETE FROM order_items   WHERE order_id IN (SELECT id FROM orders WHERE order_number LIKE 'ANALYTICS-TEST-%');
DELETE FROM order_status_history WHERE order_id IN (SELECT id FROM orders WHERE order_number LIKE 'ANALYTICS-TEST-%');
DELETE FROM order_addresses     WHERE order_id IN (SELECT id FROM orders WHERE order_number LIKE 'ANALYTICS-TEST-%');
DELETE FROM orders        WHERE order_number LIKE 'ANALYTICS-TEST-%';
DELETE FROM products      WHERE sku LIKE 'ANALYTICS-TEST-%';
DELETE FROM customers     WHERE customer_code LIKE 'ANALYTICS-TEST-%';
