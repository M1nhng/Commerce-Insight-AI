-- Cleanup for AiAnalyticsControllerIntegrationTest.
-- Runs BEFORE_TEST_CLASS (idempotent re-runs) and AFTER_TEST_CLASS against the
-- shared Docker DB. Removes the STAFF user the test registers and its audit rows.

DELETE FROM login_history WHERE email LIKE 'ai-insights-%@example.com';

DELETE FROM audit_logs WHERE user_id IN (
    SELECT id FROM users WHERE email LIKE 'ai-insights-%@example.com'
);

DELETE FROM refresh_tokens WHERE user_id IN (
    SELECT id FROM users WHERE email LIKE 'ai-insights-%@example.com'
);

DELETE FROM users WHERE email LIKE 'ai-insights-%@example.com';
