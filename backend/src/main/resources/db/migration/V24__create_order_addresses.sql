-- =============================================================================
-- V24 — Create order_addresses table
-- Commerce Insight AI
--
-- Stores immutable shipping/billing address snapshots for each order.
-- Addresses are captured at order creation time and NEVER modified.
-- Changing a customer's address does NOT affect historical order addresses.
-- =============================================================================

CREATE TABLE order_addresses (
    id             UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id       UUID           NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    type           VARCHAR(20)    NOT NULL CHECK (type IN ('SHIPPING', 'BILLING')),
    recipient_name VARCHAR(255)   NOT NULL,
    phone          VARCHAR(50),
    address_line   VARCHAR(500)   NOT NULL,
    ward           VARCHAR(100),
    district       VARCHAR(100),
    province       VARCHAR(100),
    country        VARCHAR(100)   NOT NULL DEFAULT 'Vietnam',
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  order_addresses                IS 'Immutable address snapshots per order — one SHIPPING, one BILLING';
COMMENT ON COLUMN order_addresses.type           IS 'SHIPPING or BILLING';
COMMENT ON COLUMN order_addresses.recipient_name IS 'Name of person receiving the package at this address';
COMMENT ON COLUMN order_addresses.address_line   IS 'Full street address line';
COMMENT ON COLUMN order_addresses.ward           IS 'Phường/Xã (Vietnamese administrative unit)';
COMMENT ON COLUMN order_addresses.district       IS 'Quận/Huyện';
COMMENT ON COLUMN order_addresses.province       IS 'Tỉnh/Thành phố';
