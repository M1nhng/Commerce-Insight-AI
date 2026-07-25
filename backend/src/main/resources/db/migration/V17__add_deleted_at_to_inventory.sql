-- =============================================================================
-- V17 — Add deleted_at to inventory table
-- Commerce Insight AI
-- =============================================================================
-- The Inventory entity extends BaseEntity which declares a deleted_at column.
-- V7 and V15 did not include this column, causing Hibernate schema validation
-- to fail. This migration adds the nullable column to restore consistency.
-- =============================================================================

ALTER TABLE inventory
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

COMMENT ON COLUMN inventory.deleted_at IS 'Soft-delete timestamp. NULL = active. Inherited from BaseEntity convention.';
