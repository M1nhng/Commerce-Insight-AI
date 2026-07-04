-- =============================================================================
-- PostgreSQL Initialization Script
-- Runs once when the postgres container is first created.
-- =============================================================================
-- NOTE: Flyway manages schema migrations. This file only contains
--       database-level setup (extensions, locale config, etc.)
-- =============================================================================

-- Enable UUID generation extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- TODO: Add any additional PostgreSQL extensions or database-level config
--       needed before Flyway migrations run.
