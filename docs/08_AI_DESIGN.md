# 08 — AI Design
# Commerce Insight AI

> **Document Type**: AI Architecture
> **Version**: 1.0.0
> **Status**: Approved
> **Last Updated**: 2026-07-06
> **Owner**: Chief Solution Architect

---

## Table of Contents

1. [Purpose](#1-purpose)
2. [AI Architecture Overview](#2-ai-architecture-overview)
3. [LLM Provider Pattern](#3-llm-provider-pattern)
4. [Provider Interface](#4-provider-interface)
5. [Provider Implementations](#5-provider-implementations)
6. [Prompt Strategy](#6-prompt-strategy)
7. [Conversation Context](#7-conversation-context)
8. [Fallback Strategy](#8-fallback-strategy)
9. [Rate Limit Strategy](#9-rate-limit-strategy)
10. [Retry Strategy](#10-retry-strategy)
11. [AI Module Structure](#11-ai-module-structure)
12. [Future Improvements](#12-future-improvements)

---

## 1. Purpose

This document defines the complete AI integration architecture for Commerce Insight AI. It covers the provider abstraction layer, prompt strategy, conversation management, and all resilience patterns.

**Core Rules (Non-negotiable):**

> 1. The AI NEVER accesses the database directly.
> 2. The AI NEVER generates SQL.
> 3. The AI ONLY retrieves data through MCP tools.
> 4. All business logic STAYS in Spring Boot.

---

## 2. AI Architecture Overview

### 2.1 System Placement

The AI module lives inside the Spring Boot backend as `com.commerceinsight.ai`. It is not a separate service.

```
┌─────────────────────────────────────────────────────────────────┐
│                   Spring Boot (ai module)                        │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    AI Service Layer                        │  │
│  │                                                            │  │
│  │  ┌────────────────┐    ┌────────────────────────────────┐  │  │
│  │  │  Conversation  │    │    LLM Provider Factory         │  │  │
│  │  │  Manager       │───►│    (resolve active provider)    │  │  │
│  │  └────────────────┘    └───────────────┬────────────────┘  │  │
│  │                                        │                    │  │
│  │              ┌─────────────────────────┼────────────────┐  │  │
│  │              │                         │                │  │  │
│  │        ┌─────▼──────┐  ┌──────────┐  ┌▼──────────┐  ┌──▼──┐│  │
│  │        │  OpenAI    │  │  Claude  │  │  Gemini   │  │Ollama││  │
│  │        │  Provider  │  │  Provider│  │  Provider │  │Prov  ││  │
│  │        └────────────┘  └──────────┘  └──────────┘  └──────┘│  │
│  └───────────────────────────────────────────────────────────┘  │
│                              │                                   │
│                   HTTP via MCP Client                            │
│                              │                                   │
└──────────────────────────────┼──────────────────────────────────┘
                               │
                    ┌──────────▼──────────┐
                    │    MCP Server       │
                    │    (Node.js)        │
                    └──────────┬──────────┘
                               │ REST API calls
                    ┌──────────▼──────────┐
                    │  Spring Boot REST   │
                    │  (Analytics, etc.)  │
                    └─────────────────────┘
```

### 2.2 Request Flow

```
1. User sends message to POST /api/v1/ai/chat
2. AI Service loads conversation history from session/DB
3. AI Service builds system prompt (with MCP tool descriptions)
4. AI Service calls LLMProvider.chat(messages, tools)
5. LLM decides to call one or more MCP tools
6. AI Service invokes MCPClient.callTool(toolName, args)
7. MCPClient calls MCP Server → MCP Server calls Spring Boot REST API
8. Tool result returned to AI Service → appended to message context
9. AI Service sends updated context back to LLM
10. LLM generates final natural language response
11. AI Service saves conversation turn to DB
12. Response returned to user
```

---

## 3. LLM Provider Pattern

### 3.1 Design Decision: Strategy Pattern

The LLM provider layer uses the **Strategy Pattern**:
- One interface: `LLMProvider`
- Multiple implementations: `OpenAIProvider`, `ClaudeProvider`, `GeminiProvider`, `OllamaProvider`
- One factory: `LLMProviderFactory` resolves the active provider at runtime

The active provider is configured via:
1. Database setting (Admin UI can switch without restart)
2. Environment variable fallback: `AI_PROVIDER=openai|claude|gemini|ollama`

### 3.2 Why Not LangChain4J?

| Option | Reason |
|--------|--------|
| **Custom interface** | Full control, no abstraction overhead, easy to understand |
| LangChain4J | Additional dependency, opinionated patterns, abstraction may hide important details |

For a portfolio project, a **clean custom abstraction** better demonstrates architectural thinking.

---

## 4. Provider Interface

### 4.1 Core Interface Design

```java
// Conceptual design — NOT production code
/**
 * LLMProvider — abstraction over all supported LLM backends.
 * All providers MUST implement this interface.
 * The AI module ONLY depends on this interface, never on concrete providers.
 */
public interface LLMProvider {

    /**
     * Send a conversational message with optional tools.
     *
     * @param request ChatRequest containing message history, system prompt, tools
     * @return ChatResponse with assistant message and any tool calls made
     */
    ChatResponse chat(ChatRequest request);

    /**
     * Return the provider identifier.
     * Used by LLMProviderFactory for resolution.
     */
    ProviderType getProviderType();

    /**
     * Check if this provider is available and configured.
     */
    boolean isAvailable();
}
```

### 4.2 Request / Response Models

```
ChatRequest:
  - messages:    List<ChatMessage>   (role: system | user | assistant | tool)
  - tools:       List<ToolDefinition>  (MCP tool schemas)
  - model:       String              (provider-specific model name)
  - maxTokens:   Integer
  - temperature: Double

ChatResponse:
  - content:     String              (assistant's text response)
  - toolCalls:   List<ToolCall>      (tool name + arguments LLM wants to call)
  - usage:       TokenUsage          (prompt tokens, completion tokens)
  - finishReason: FinishReason       (STOP, TOOL_CALL, LENGTH, ERROR)

ChatMessage:
  - role:        MessageRole         (SYSTEM, USER, ASSISTANT, TOOL)
  - content:     String
  - toolCallId:  String              (for TOOL role messages)
  - name:        String              (for tool result messages)

ToolDefinition:
  - name:        String
  - description: String
  - inputSchema: Object              (JSON Schema of parameters)
```

---

## 5. Provider Implementations

### 5.1 OpenAI Provider

| Property | Value |
|----------|-------|
| API URL | `https://api.openai.com/v1/chat/completions` |
| Default model | `gpt-4o-mini` |
| Auth | `Authorization: Bearer {OPENAI_API_KEY}` |
| Tool calling | Supported via `tools` + `tool_choice` parameters |
| Response format | `choices[0].message.content` + `choices[0].message.tool_calls` |

**Mapping:**
- `LLMProvider.chat(request)` → OpenAI Chat Completions API
- `ChatMessage(TOOL)` → OpenAI `tool` role message with `tool_call_id`
- `ToolDefinition` → OpenAI `function` object in `tools` array

### 5.2 Claude (Anthropic) Provider

| Property | Value |
|----------|-------|
| API URL | `https://api.anthropic.com/v1/messages` |
| Default model | `claude-3-5-haiku-20241022` |
| Auth | `x-api-key: {ANTHROPIC_API_KEY}` + `anthropic-version: 2023-06-01` |
| Tool calling | Supported via `tools` parameter |
| Response format | `content[].text` + `content[].type == 'tool_use'` |

**Mapping:**
- System message separated from messages array (Claude-specific requirement)
- `ToolDefinition` → Anthropic `tool` with `input_schema`
- Tool results → `tool_result` content block

### 5.3 Gemini Provider

| Property | Value |
|----------|-------|
| API URL | `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent` |
| Default model | `gemini-1.5-flash` |
| Auth | `?key={GEMINI_API_KEY}` query parameter |
| Tool calling | Supported via `tools[].functionDeclarations` |
| Response format | `candidates[0].content.parts` |

**Mapping:**
- Message roles: `user` and `model` (no `system` — injected as first user message)
- Tool results returned as `functionResponse` parts

### 5.4 Ollama Provider

| Property | Value |
|----------|-------|
| API URL | `http://localhost:11434/api/chat` (configurable) |
| Default model | `llama3.2` (configurable) |
| Auth | None (local deployment) |
| Tool calling | Supported in Ollama ≥ 0.3 for capable models |
| Response format | OpenAI-compatible streaming or non-streaming |

**Use case:** Local development without API costs; privacy-sensitive deployments.

### 5.5 Provider Factory

```
LLMProviderFactory.getProvider():
  1. Read active provider from DB settings (cached, refreshed every 5 min)
  2. If not found in DB, read AI_PROVIDER env var
  3. Return the matching provider bean
  4. If provider is not available (missing API key), throw ProviderNotAvailableException
     → triggers fallback logic (see §8)
```

---

## 6. Prompt Strategy

### 6.1 System Prompt Template

The system prompt is assembled dynamically on each conversation start:

```
You are a business intelligence assistant for Commerce Insight AI, 
an ecommerce analytics platform.

You help merchants analyze their business performance through data.

RULES:
- ONLY use the provided tools to retrieve data.
- NEVER make up data or statistics.
- ALWAYS cite the time period when presenting numbers.
- If a tool returns an error, explain it clearly and suggest alternatives.
- Respond in a concise, professional tone.
- Use markdown formatting for numbers and lists.

AVAILABLE DATA:
- Revenue and sales analytics (daily, weekly, monthly, quarterly, yearly)
- Product performance (top sellers, units sold, revenue per product)
- Customer analytics (new vs. returning, lifetime value, segments)
- Order analytics (count, fulfillment rate, cancellation rate, status breakdown)
- Inventory levels (current stock, low-stock alerts)
- Category performance

TODAY: {currentDate}
MERCHANT: {merchantName}
```

### 6.2 Tool Injection

Tool definitions are loaded at runtime from the MCP Server's tool registry and injected into each LLM call. The AI Service calls `GET /mcp/tools` (or reads from a static registry) to get the current tool list.

### 6.3 Context Window Management

| Strategy | Limit |
|----------|-------|
| Maximum conversation history sent to LLM | Last 20 turns |
| Maximum tool result size | 4,000 characters (truncated with note) |
| Maximum system prompt size | ~1,000 tokens |
| Maximum total context | 80% of model's context window |

When context exceeds limit → oldest user/assistant pairs are dropped (keeping system prompt and most recent turns).

---

## 7. Conversation Context

### 7.1 Session Model

```
ConversationSession:
  - id:           UUID
  - userId:       UUID (FK → users)
  - title:        String (auto-generated from first message)
  - messages:     List<ConversationMessage>
  - createdAt:    TIMESTAMPTZ
  - updatedAt:    TIMESTAMPTZ

ConversationMessage:
  - id:           UUID
  - sessionId:    UUID (FK → conversation_sessions)
  - role:         Enum (USER, ASSISTANT, TOOL_CALL, TOOL_RESULT)
  - content:      TEXT
  - toolName:     String (nullable — only for TOOL_CALL messages)
  - toolArgs:     JSONB (nullable — tool call arguments)
  - tokenCount:   Integer
  - createdAt:    TIMESTAMPTZ
```

### 7.2 Session Lifecycle

```
User starts AI chat:
  → Check for active session in request
  → If session_id provided: load existing session
  → If no session_id: create new ConversationSession
  
Each message:
  → Append user message to session
  → Load last N messages from session
  → Call LLM with history
  → If tool_calls: invoke tools, append tool results
  → Append assistant response to session
  → Return response to user with session_id
  
Session cleanup:
  → Sessions older than 30 days: auto-archived
  → Archived sessions: messages retained but not loaded into context
```

---

## 8. Fallback Strategy

### 8.1 Provider Fallback Chain

```
Primary provider fails (timeout, API error, quota exceeded)
  │
  ▼
Try fallback provider (configured in settings, e.g., Gemini → OpenAI)
  │
  ▼
If fallback also fails:
  → Return graceful error to user:
    "I'm unable to process your request at the moment. 
     Our AI service is temporarily unavailable. 
     Please try again in a few minutes."
  → Log full error details for admin review
  → Do NOT expose internal error details to the user
```

### 8.2 Fallback Configuration

| Setting | Description |
|---------|-------------|
| `ai.primary-provider` | The main LLM provider |
| `ai.fallback-provider` | The backup provider if primary fails |
| `ai.fallback-enabled` | Whether fallback is active |

---

## 9. Rate Limit Strategy

### 9.1 Per-User Limits

| Limit | Value |
|-------|-------|
| Messages per minute per user | 10 |
| Messages per hour per user | 100 |
| Messages per day per user | 500 |

Rate limits enforced by the AI controller using an in-memory token bucket (or Redis in production).

### 9.2 Provider Rate Limit Handling

When the LLM provider returns HTTP 429 (Too Many Requests):

```
1. Extract Retry-After header (if present)
2. If Retry-After ≤ 30 seconds: wait and retry
3. If Retry-After > 30 seconds or not present: try fallback provider
4. If all providers rate-limited: return 503 to user with retry guidance
```

---

## 10. Retry Strategy

| Condition | Action |
|-----------|--------|
| Provider timeout (< 30s) | Retry once after 2 seconds |
| Provider HTTP 500/503 | Retry twice with exponential backoff (2s, 4s) |
| Provider HTTP 429 | Wait for Retry-After, then retry once |
| Provider HTTP 401 (bad API key) | Do NOT retry; alert admin; return 503 to user |
| Tool call network error | Retry tool call twice with 1s backoff |
| Total time budget per request | 60 seconds maximum (then return timeout error) |

---

## 11. AI Module Structure

```
com.commerceinsight.ai/
│
├── controller/
│   └── AiController.java           ← POST /api/v1/ai/chat, GET /api/v1/ai/sessions
│
├── service/
│   ├── AiService.java              ← Orchestrates: history, tool invocation, LLM call
│   ├── ConversationService.java    ← Session CRUD, message persistence
│   └── MCPClientService.java       ← Calls MCP Server via Axios (HTTP)
│
├── provider/
│   ├── LLMProvider.java            ← Interface
│   ├── LLMProviderFactory.java     ← Runtime resolution
│   ├── openai/
│   │   └── OpenAIProvider.java
│   ├── claude/
│   │   └── ClaudeProvider.java
│   ├── gemini/
│   │   └── GeminiProvider.java
│   └── ollama/
│       └── OllamaProvider.java
│
├── repository/
│   ├── ConversationSessionRepository.java
│   └── ConversationMessageRepository.java
│
├── domain/
│   ├── ConversationSession.java
│   └── ConversationMessage.java
│
├── dto/
│   ├── request/
│   │   └── AiChatRequest.java      ← { sessionId?, message }
│   └── response/
│       ├── AiChatResponse.java     ← { sessionId, reply, toolsUsed[] }
│       └── ConversationSessionResponse.java
│
└── mapper/
    └── ConversationMapper.java
```

---

## 12. Future Improvements

| Improvement | Description | Priority |
|-------------|-------------|----------|
| Streaming responses | SSE streaming for real-time token output | High |
| Image analysis | Support for chart/screenshot analysis via multimodal LLMs | Medium |
| Persistent tool caching | Cache common tool results (e.g., top products) for 5 min | Medium |
| Fine-tuned model | Domain-specific model fine-tuned on ecommerce analytics | Low (future) |
| Semantic search on history | Vector search for relevant past conversations | Low |
| Token cost tracking | Track and display token usage and cost per session | Medium |
| Function calling improvements | Support parallel tool calls (OpenAI parallel_tool_calls) | High |
