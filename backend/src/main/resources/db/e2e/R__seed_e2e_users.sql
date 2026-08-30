-- =============================================================================
-- R__seed_e2e_users.sql  — REPEATABLE, E2E PROFILE ONLY
-- =============================================================================
-- Loaded ONLY when spring.flyway.locations includes classpath:db/e2e
-- (set in application-e2e.yml). NEVER runs under dev / test / prod.
--
-- Deterministic RBAC test users for the Playwright E2E suite. Idempotent:
-- re-running an already-seeded database is a no-op (ON CONFLICT DO NOTHING on
-- the partial unique index uq_users_email).
--
-- Plaintext passwords (test-only, safe to publish — never used in prod):
--   e2e-staff@commerceinsight.test    / E2eStaff!234
--   e2e-manager@commerceinsight.test  / E2eManager!234
--   e2e-admin@commerceinsight.test    / E2eAdmin!234
--   e2e-locked@commerceinsight.test   / E2eLocked!234   (locked = true)
--   e2e-disabled@commerceinsight.test / E2eDisabled!234 (active = false)
--
-- BCrypt hashes were generated with cost 10 ($2b$). Spring's
-- BCryptPasswordEncoder verifies $2a/$2b/$2y transparently.
-- =============================================================================

INSERT INTO users (email, password_hash, first_name, last_name, role, active, locked)
VALUES
  ('e2e-staff@commerceinsight.test',
   '$2b$10$zCqEOiFldkebkkOtSgar7eCnaZ/.c2oTmD5MYjdc8QJ3A7e3mFagi',
   'E2E', 'Staff', 'STAFF', TRUE, FALSE),
  ('e2e-manager@commerceinsight.test',
   '$2b$10$qMPCsKd8ixxPE9kKmWSDqePpTdSaON/OEB3uvPuYCokYlyUx/jjny',
   'E2E', 'Manager', 'MANAGER', TRUE, FALSE),
  ('e2e-admin@commerceinsight.test',
   '$2b$10$Y17dA6fqonEcJzCZpmo.NO0pjqpMBbMJuMHxmZzxdQ/LjZVXCpJqu',
   'E2E', 'Admin', 'ADMIN', TRUE, FALSE),
  ('e2e-locked@commerceinsight.test',
   '$2b$10$zCqEOiFldkebkkOtSgar7eCnaZ/.c2oTmD5MYjdc8QJ3A7e3mFagi',
   'E2E', 'Locked', 'STAFF', TRUE, TRUE),
  ('e2e-disabled@commerceinsight.test',
   '$2b$10$zCqEOiFldkebkkOtSgar7eCnaZ/.c2oTmD5MYjdc8QJ3A7e3mFagi',
   'E2E', 'Disabled', 'STAFF', FALSE, FALSE)
ON CONFLICT (email) WHERE deleted_at IS NULL DO NOTHING;
