-- =============================================================================
-- V13 — Create Product Images Table
-- Commerce Insight AI
-- =============================================================================
-- Products can have multiple images beyond the primary image_url column.
-- The primary image is stored in products.image_url for denormalisation /
-- query efficiency. Additional images are stored here.
-- =============================================================================

CREATE TABLE product_images (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id  UUID            NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    url         VARCHAR(1000)   NOT NULL,
    alt_text    VARCHAR(255),
    sort_order  INTEGER         NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  product_images IS 'Additional product images; products.image_url holds the primary image';
COMMENT ON COLUMN product_images.sort_order IS 'Display order — lower value = shown first';

-- Index for fast per-product image queries
CREATE INDEX idx_product_images_product
    ON product_images(product_id, sort_order);
