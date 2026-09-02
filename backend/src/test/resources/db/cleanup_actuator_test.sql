-- Cleanup for ActuatorSecurityIntegrationTest (Sprint 15).
-- Runs BEFORE_TEST_CLASS so re-runs against the shared Docker DB are idempotent
-- (the test registers a throwaway STAFF user for the 403 case).

DELETE FROM login_history WHERE email LIKE 'actuator-%@example.com';

DELETE FROM audit_logs WHERE user_id IN (
    SELECT id FROM users WHERE email LIKE 'actuator-%@example.com'
);

DELETE FROM refresh_tokens WHERE user_id IN (
    SELECT id FROM users WHERE email LIKE 'actuator-%@example.com'
);

DELETE FROM users WHERE email LIKE 'actuator-%@example.com';
