-- Cleanup script for AuthControllerIntegrationTest
-- Runs before the test class to ensure idempotent execution against a shared Docker DB
DELETE FROM refresh_tokens
WHERE user_id IN (SELECT id FROM users WHERE email = 'integration-test@example.com');
DELETE FROM users WHERE email = 'integration-test@example.com';
