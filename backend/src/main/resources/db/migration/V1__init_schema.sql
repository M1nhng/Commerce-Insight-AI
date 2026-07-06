-- =============================================================================
-- V1 — Enable PostgreSQL Extensions
-- Commerce Insight AI
-- =============================================================================
-- Purpose: Enable required PostgreSQL extensions before any schema creation.
-- Extensions must exist before using gen_random_uuid(), pg_trgm GIN indexes.
-- =============================================================================

-- UUID generation (used for all primary keys via gen_random_uuid())
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- UUID functions alternative (some versions need this for gen_random_uuid())
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Trigram indexing for full-text search on product names
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
