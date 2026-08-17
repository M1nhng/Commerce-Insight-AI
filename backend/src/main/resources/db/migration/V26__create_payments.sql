-- =============================================================================
-- V26 — Create payments table
-- Commerce Insight AI
--
-- Simulated payment records associated with orders.
-- No real payment gateway integration — payment method and status
-- are managed by internal business logic only.
-- One payment record per order.
-- =============================================================================

CREATE TABLE payments (
    id          UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID           NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    method      VARCHAR(50)    NOT NULL CHECK (method IN ('CASH', 'BANK_TRANSFER', 'CARD', 'OTHER')),
    status      VARCHAR(50)    NOT NULL DEFAULT 'PENDING'
                CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED')),
    amount      DECIMAL(19,4)  NOT NULL CHECK (amount >= 0),
    currency    VARCHAR(10)    NOT NULL DEFAULT 'VND',
    reference   VARCHAR(255),
    paid_at     TIMESTAMPTZ,
    notes       TEXT,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  payments           IS 'Simulated payment records — no external gateway integration';
COMMENT ON COLUMN payments.method    IS 'Payment method: CASH, BANK_TRANSFER, CARD, OTHER';
COMMENT ON COLUMN payments.status    IS 'Payment lifecycle: PENDING, PAID, FAILED, REFUNDED';
COMMENT ON COLUMN payments.amount    IS 'Total amount charged — mirrors orders.total_amount';
COMMENT ON COLUMN payments.currency  IS 'ISO 4217 currency code (default: VND)';
COMMENT ON COLUMN payments.reference IS 'Optional external reference (bank transaction ID, etc.)';
COMMENT ON COLUMN payments.paid_at   IS 'Timestamp when payment status moved to PAID';
