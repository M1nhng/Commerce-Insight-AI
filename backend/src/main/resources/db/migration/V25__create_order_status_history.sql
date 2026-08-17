-- =============================================================================
-- V25 — Create order_status_history table
-- Commerce Insight AI
--
-- Append-only audit trail for every order status transition.
-- Records are NEVER modified or deleted (beyond CASCADE with parent order).
-- Every valid status transition in OrderStatusTransitionService
-- must create exactly one record here.
-- =============================================================================

CREATE TABLE order_status_history (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID         NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    from_status VARCHAR(50),
    to_status   VARCHAR(50)  NOT NULL,
    changed_by  UUID         REFERENCES users(id) ON DELETE SET NULL,
    reason      TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  order_status_history             IS 'Append-only log of every order status transition';
COMMENT ON COLUMN order_status_history.from_status IS 'NULL on initial PENDING creation';
COMMENT ON COLUMN order_status_history.to_status   IS 'Target status this transition moved the order to';
COMMENT ON COLUMN order_status_history.changed_by  IS 'User who triggered this transition (NULL = system)';
COMMENT ON COLUMN order_status_history.reason      IS 'Optional human-readable reason for the transition';
