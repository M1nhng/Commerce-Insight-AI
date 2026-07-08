-- =============================================================================
-- V11 — Seed Default Data
-- Commerce Insight AI
-- =============================================================================
-- Purpose: Insert the default admin user and initial system settings.
--
-- IMPORTANT: The default admin password below is BCrypt hash of 'Admin@123456'.
-- Change this immediately after first deployment via the Admin UI.
--
-- BCrypt generation (strength 12):
--   BCryptPasswordEncoder(12).encode("Admin@123456")
-- =============================================================================

-- ── Default Admin User ────────────────────────────────────────────────────
INSERT INTO users (
    id,
    email,
    password_hash,
    first_name,
    last_name,
    role,
    active,
    locked,
    failed_attempts,
    created_at,
    updated_at
) VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'admin@commerceinsight.ai',
    -- BCrypt hash of 'Admin@123456' (strength 12, $2b format) — CHANGE IN PRODUCTION
    '$2b$12$PsNLhyqKzC7vnpBNTggXpOlJZzgMNWx/Ks1hSIpRisYIIBN2uni.2',
    'System',
    'Administrator',
    'ADMIN',
    TRUE,
    FALSE,
    0,
    NOW(),
    NOW()
) ON CONFLICT (id) DO NOTHING;

-- ── System Settings ───────────────────────────────────────────────────────
INSERT INTO system_settings (key, value, description) VALUES
    ('ai.provider',                     'openai',   'Active LLM provider: openai | claude | gemini | ollama'),
    ('ai.model',                        'gpt-4o-mini', 'Active LLM model identifier'),
    ('ai.fallback_provider',            'gemini',   'Fallback provider if primary is unavailable'),
    ('ai.max_tokens',                   '2048',     'Maximum tokens per LLM response'),
    ('inventory.default_low_stock_threshold', '10', 'Default low-stock alert threshold for new products'),
    ('order.sequence_prefix',           'ORD',      'Prefix for human-readable order numbers'),
    ('app.maintenance_mode',            'false',    'Set to true to enable maintenance mode')
ON CONFLICT (key) DO NOTHING;
