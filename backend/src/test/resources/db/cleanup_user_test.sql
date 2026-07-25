-- Cleanup script for UserControllerIntegrationTest
-- Removes the test STAFF user created during setup so the test is idempotent
DELETE FROM refresh_tokens
WHERE user_id IN (SELECT id FROM users WHERE email = 'userctrl-staff@example.com');
DELETE FROM users WHERE email = 'userctrl-staff@example.com';

-- Also clean up any users created by createUser tests
DELETE FROM refresh_tokens
WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'userctrl-%@example.com');
DELETE FROM users WHERE email LIKE 'userctrl-%@example.com';
