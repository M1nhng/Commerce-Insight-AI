-- Cleanup customer domain test data before integration tests run
-- Order matters: child tables first

DELETE FROM customer_addresses WHERE customer_id IN (
    SELECT id FROM customers WHERE customer_code LIKE 'CUST-TEST-%' OR email LIKE '%@customer-test.com'
);

DELETE FROM customers
WHERE customer_code LIKE 'CUST-TEST-%'
   OR email LIKE '%@customer-test.com';

DELETE FROM customer_groups
WHERE code LIKE 'GRP-TEST-%';
