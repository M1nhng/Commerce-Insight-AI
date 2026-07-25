-- =============================================================================
-- V14 — Create Warehouses Table
-- Commerce Insight AI
-- =============================================================================
-- Warehouses represent physical or virtual stock locations.
-- Supports multi-warehouse inventory management.
-- =============================================================================

CREATE TABLE warehouses (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(150) NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    address     TEXT,
    city        VARCHAR(100),
    country     VARCHAR(100),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

COMMENT ON TABLE  warehouses IS 'Physical or virtual stock locations for multi-warehouse support';
COMMENT ON COLUMN warehouses.code IS 'Short unique identifier (e.g. WH-MAIN, WH-EAST)';
COMMENT ON COLUMN warehouses.active IS 'Inactive warehouses cannot receive new stock';

-- Unique warehouse code (partial — ignores soft-deleted rows)
CREATE UNIQUE INDEX uq_warehouses_code ON warehouses(code) WHERE deleted_at IS NULL;

-- Performance index
CREATE INDEX idx_warehouses_active ON warehouses(active) WHERE deleted_at IS NULL;

-- Seed a default "Main Warehouse" so existing inventory rows still work
INSERT INTO warehouses (id, name, code, address, active)
VALUES ('00000000-0000-0000-0000-000000000001', 'Main Warehouse', 'WH-MAIN', 'Default Location', TRUE);
