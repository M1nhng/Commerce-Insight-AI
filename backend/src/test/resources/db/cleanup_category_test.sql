-- Cleanup script for CategoryControllerIntegrationTest
-- Removes test categories created during integration test runs
-- Safe to run multiple times (idempotent)
DELETE FROM categories WHERE slug IN ('it-electronics', 'smartphones');
