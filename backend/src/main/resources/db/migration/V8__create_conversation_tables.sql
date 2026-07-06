-- =============================================================================
-- V8 — Create AI Conversation Tables
-- Commerce Insight AI
-- =============================================================================

CREATE TABLE conversation_sessions (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(255),
    archived    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE conversation_sessions IS 'AI chat sessions — each session is a conversation thread';

CREATE TABLE conversation_messages (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id  UUID        NOT NULL REFERENCES conversation_sessions(id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL
                CHECK (role IN ('USER','ASSISTANT','TOOL_CALL','TOOL_RESULT')),
    content     TEXT        NOT NULL,
    tool_name   VARCHAR(100),
    tool_args   JSONB,
    token_count INTEGER,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  conversation_messages IS 'Individual messages within a conversation session';
COMMENT ON COLUMN conversation_messages.role IS 'USER | ASSISTANT | TOOL_CALL | TOOL_RESULT';
COMMENT ON COLUMN conversation_messages.tool_args IS 'JSONB — MCP tool arguments for TOOL_CALL messages';
