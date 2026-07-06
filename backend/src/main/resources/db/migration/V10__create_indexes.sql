-- =============================================================================
-- V10 — Create All Performance Indexes
-- Commerce Insight AI
-- =============================================================================
-- All unique and performance indexes, as specified in docs/04_DATABASE.md §7.
-- =============================================================================

-- ── UNIQUE INDEXES ────────────────────────────────────────────────────────

-- Partial unique index: email unique only for non-deleted users
CREATE UNIQUE INDEX uq_users_email
    ON users(email) WHERE deleted_at IS NULL;

-- Partial unique index: SKU unique only for non-deleted products
CREATE UNIQUE INDEX uq_products_sku
    ON products(sku) WHERE deleted_at IS NULL;

-- Partial unique index: slug unique only for non-deleted categories
CREATE UNIQUE INDEX uq_categories_slug
    ON categories(slug) WHERE deleted_at IS NULL;

-- Globally unique order number
CREATE UNIQUE INDEX uq_orders_number
    ON orders(order_number);

-- One inventory record per product
CREATE UNIQUE INDEX uq_inventory_product
    ON inventory(product_id);

-- Refresh token hash must be globally unique
CREATE UNIQUE INDEX uq_rt_token_hash
    ON refresh_tokens(token_hash);

-- ── PERFORMANCE INDEXES ───────────────────────────────────────────────────

-- User role lookup
CREATE INDEX idx_users_role
    ON users(role) WHERE deleted_at IS NULL;

-- Refresh token lookups
CREATE INDEX idx_rt_user_id      ON refresh_tokens(user_id);
CREATE INDEX idx_rt_family_id    ON refresh_tokens(family_id);
CREATE INDEX idx_rt_expires_revoked ON refresh_tokens(expires_at, revoked);

-- Product search and filtering
CREATE INDEX idx_products_category ON products(category_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_active   ON products(active, deleted_at);

-- Product full-text search (requires pg_trgm extension from V1)
CREATE INDEX idx_products_name_trgm
    ON products USING GIN(name gin_trgm_ops);

-- Order analytics (most query-critical indexes)
CREATE INDEX idx_orders_status      ON orders(status);
CREATE INDEX idx_orders_created_at  ON orders(created_at DESC);
CREATE INDEX idx_orders_customer    ON orders(customer_id);
CREATE INDEX idx_orders_status_date ON orders(status, created_at);
CREATE INDEX idx_orders_analytics   ON orders(created_at, status, total);

-- Order item lookups
CREATE INDEX idx_order_items_order   ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);

-- Customer lookup
CREATE INDEX idx_customers_email ON customers(email) WHERE deleted_at IS NULL;

-- Inventory movement history
CREATE INDEX idx_inv_mov_product_date
    ON inventory_movements(product_id, created_at DESC);

-- AI conversation lookups
CREATE INDEX idx_conv_sessions_user
    ON conversation_sessions(user_id, created_at DESC);
CREATE INDEX idx_conv_messages_session
    ON conversation_messages(session_id, created_at);

-- Audit log querying
CREATE INDEX idx_audit_user_action
    ON audit_logs(user_id, action, created_at DESC);
CREATE INDEX idx_audit_entity
    ON audit_logs(entity_type, entity_id);
