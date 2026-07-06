-- =============================================================================
-- V3 — Create Categories Table
-- Commerce Insight AI
-- =============================================================================

CREATE TABLE categories (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(150)    NOT NULL,
    slug        VARCHAR(150)    NOT NULL,
    description TEXT,
    parent_id   UUID            REFERENCES categories(id) ON DELETE RESTRICT,
    sort_order  INTEGER         NOT NULL DEFAULT 0,
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

COMMENT ON TABLE  categories IS 'Product categories — supports tree hierarchy via parent_id self-reference';
COMMENT ON COLUMN categories.slug IS 'URL-friendly name — unique (partial index on deleted_at IS NULL)';
COMMENT ON COLUMN categories.parent_id IS 'Self-referencing FK — NULL means root category';
