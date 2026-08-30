-- Cleanup for the Sprint 12A security integration tests.
-- Runs BEFORE_TEST_CLASS so the suite is idempotent against the shared Docker DB.

DELETE FROM login_history
WHERE email LIKE 'sec-hardening-%@example.com'
   OR email LIKE 'ratelimit-%@example.com'
   OR email LIKE 'loginhistory-%@example.com';

DELETE FROM audit_logs
WHERE user_id IN (
    SELECT id FROM users
    WHERE email LIKE 'sec-hardening-%@example.com'
       OR email LIKE 'ratelimit-%@example.com'
       OR email LIKE 'loginhistory-%@example.com'
);

DELETE FROM refresh_tokens
WHERE user_id IN (
    SELECT id FROM users
    WHERE email LIKE 'sec-hardening-%@example.com'
       OR email LIKE 'ratelimit-%@example.com'
       OR email LIKE 'loginhistory-%@example.com'
);

DELETE FROM users
WHERE email LIKE 'sec-hardening-%@example.com'
   OR email LIKE 'ratelimit-%@example.com'
   OR email LIKE 'loginhistory-%@example.com';
