-- =============================================================================
-- V28 — Add BaseEntity audit/soft-delete columns to orders, order_items, payments
-- Commerce Insight AI
-- =============================================================================
-- Order, OrderItem, and Payment entities extend BaseEntity which declares
-- created_at, updated_at, and deleted_at columns.
-- This migration adds the missing columns to satisfy Hibernate schema validation.
-- =============================================================================

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

COMMENT ON COLUMN orders.deleted_at      IS 'Soft-delete timestamp. NULL = active. Inherited from BaseEntity.';
COMMENT ON COLUMN order_items.updated_at IS 'Last modification timestamp. Inherited from BaseEntity.';
COMMENT ON COLUMN order_items.deleted_at IS 'Soft-delete timestamp. NULL = active. Inherited from BaseEntity.';
COMMENT ON COLUMN payments.deleted_at    IS 'Soft-delete timestamp. NULL = active. Inherited from BaseEntity.';
