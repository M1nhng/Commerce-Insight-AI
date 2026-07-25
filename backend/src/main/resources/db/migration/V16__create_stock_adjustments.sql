-- =============================================================================
-- V16 — Create Stock Adjustments Table
-- Commerce Insight AI
-- =============================================================================
-- Stock adjustments represent requested inventory corrections that require
-- an approval workflow before they are applied to inventory.
--
-- Lifecycle:  PENDING → APPROVED (stock applied) | REJECTED (no stock change)
-- =============================================================================

CREATE TABLE stock_adjustments (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_id    UUID         NOT NULL REFERENCES inventory(id) ON DELETE RESTRICT,
    product_id      UUID         NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    warehouse_id    UUID         NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    quantity_delta  INTEGER      NOT NULL,   -- positive = add stock, negative = remove stock
    reason          TEXT         NOT NULL,
    status          VARCHAR(50)  NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    requested_by    UUID         REFERENCES users(id) ON DELETE SET NULL,
    reviewed_by     UUID         REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at     TIMESTAMPTZ,
    review_notes    TEXT,
    transaction_id  UUID         REFERENCES inventory_transactions(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  stock_adjustments IS 'Pending inventory corrections requiring approval before being applied';
COMMENT ON COLUMN stock_adjustments.quantity_delta IS 'Positive = add stock, Negative = remove stock. Applied only on APPROVED.';
COMMENT ON COLUMN stock_adjustments.transaction_id IS 'Set when APPROVED — links to the resulting inventory_transaction';
COMMENT ON COLUMN stock_adjustments.review_notes IS 'Reason for approval or rejection by the reviewer';

-- Indexes
CREATE INDEX idx_stock_adj_inventory    ON stock_adjustments(inventory_id);
CREATE INDEX idx_stock_adj_product      ON stock_adjustments(product_id);
CREATE INDEX idx_stock_adj_warehouse    ON stock_adjustments(warehouse_id);
CREATE INDEX idx_stock_adj_status       ON stock_adjustments(status);
CREATE INDEX idx_stock_adj_requested_by ON stock_adjustments(requested_by);
CREATE INDEX idx_stock_adj_created      ON stock_adjustments(created_at DESC);
