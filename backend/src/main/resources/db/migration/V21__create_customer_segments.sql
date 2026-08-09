-- =============================================================================
-- V21 — Create customer_segments table
-- Commerce Insight AI
-- =============================================================================

CREATE TABLE customer_segments (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(50)     NOT NULL,
    name        VARCHAR(150)    NOT NULL,
    description TEXT,
    type        VARCHAR(30)     NOT NULL DEFAULT 'MANUAL'
                    CHECK (type IN ('MANUAL', 'RULE_BASED', 'AI_GENERATED')),
    status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_customer_segments_code ON customer_segments(code);

COMMENT ON TABLE customer_segments IS 'Customer segments — foundation for RFM and AI segmentation in future sprints';
COMMENT ON COLUMN customer_segments.type IS 'MANUAL | RULE_BASED | AI_GENERATED';
