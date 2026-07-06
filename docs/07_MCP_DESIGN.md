# 07 — MCP Server Design
# Commerce Insight AI

> **Document Type**: MCP Architecture
> **Version**: 1.0.0
> **Status**: Approved
> **Last Updated**: 2026-07-06
> **Owner**: Chief Solution Architect

---

## Table of Contents

1. [Purpose](#1-purpose)
2. [Why MCP](#2-why-mcp)
3. [Architecture Overview](#3-architecture-overview)
4. [Transport Design](#4-transport-design)
5. [Tool Registry](#5-tool-registry)
6. [Tool Categories & Definitions](#6-tool-categories--definitions)
7. [Security Design](#7-security-design)
8. [Error Handling](#8-error-handling)
9. [Request / Response Contract](#9-request--response-contract)
10. [Implementation Guidelines](#10-implementation-guidelines)
11. [Future Extension](#11-future-extension)

---

## 1. Purpose

This document defines the complete design for the Commerce Insight AI MCP (Model Context Protocol) server. It describes why MCP was chosen, how it is architected, what tools are exposed, how security is enforced, and how errors are handled.

**Critical Rule — Enforced at every level:**

> **The MCP server NEVER accesses the database directly.**
> **ALL data flows through the Spring Boot REST API.**
> **Business logic NEVER moves into the MCP server.**

---

## 2. Why MCP

### 2.1 The Problem with Direct AI-DB Integration

Traditional AI assistant architectures that give LLMs direct database access create severe risks:

| Risk | Impact |
|------|--------|
| AI-generated SQL can be incorrect or malicious | Data corruption, information leakage |
| No authorization layer between AI and data | Any AI prompt can access any data |
| Business logic bypassed | AI ignores validation rules |
| No audit trail for AI data access | Security compliance failure |
| Tight coupling to database schema | Schema changes break AI integration |

### 2.2 Why MCP Solves This

The Model Context Protocol (MCP) is an open standard by Anthropic that defines how AI agents discover and invoke tools in a standardized way.

| Benefit | How MCP Achieves It |
|---------|-------------------|
| **Standardized tool discovery** | AI agents auto-discover available tools via MCP protocol |
| **Typed, validated tool inputs** | Every tool has a Zod schema — invalid inputs rejected before reaching backend |
| **Clean separation of concerns** | MCP handles AI protocol; Spring Boot handles business logic and data |
| **Authorization at the API level** | Every MCP tool call hits a protected Spring Boot endpoint with API key |
| **Auditable** | Every tool invocation is logged |
| **Vendor-agnostic** | Works with Claude, OpenAI (via tools API), Gemini, and Ollama |

### 2.3 Communication Flow

```
User (Natural Language)
        │
        ▼
  AI Service (Spring Boot)
  [LLM Provider Abstraction Layer]
        │  System prompt with tool descriptions
        │  User message
        ▼
  LLM Provider (OpenAI / Claude / Gemini / Ollama)
        │
        │  "I need to call get_revenue_summary({ period: 'month' })"
        ▼
  MCP Server (Node.js)
  [Tool Router]
        │
        │  HTTP POST /api/v1/analytics/revenue?period=month
        │  Header: X-MCP-API-KEY: {secret}
        ▼
  Spring Boot REST API
        │
        ▼
  PostgreSQL
        │
        ▼ (Response bubbles back up the chain)
  Spring Boot → MCP Server → LLM → AI Service → User
```

---

## 3. Architecture Overview

### 3.1 Component Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        MCP Server (Node.js)                      │
│                                                                  │
│  ┌─────────────────┐    ┌──────────────────┐                   │
│  │   MCP Protocol  │    │   Tool Registry   │                   │
│  │   (stdio/SSE)   │───►│   (Tool Router)   │                   │
│  └─────────────────┘    └──────────────────┘                   │
│                                    │                            │
│           ┌────────────────────────┼──────────────────┐        │
│           │                        │                  │         │
│           ▼                        ▼                  ▼         │
│  ┌──────────────┐       ┌──────────────┐    ┌──────────────┐  │
│  │  Revenue     │       │  Customer    │    │  Inventory   │  │
│  │  Tools       │       │  Tools       │    │  Tools       │  │
│  └──────────────┘       └──────────────┘    └──────────────┘  │
│           │                        │                  │         │
│           └────────────────────────┼──────────────────┘        │
│                                    │                            │
│                          ┌─────────────────┐                   │
│                          │   API Client    │                   │
│                          │   (Axios)       │                   │
│                          └─────────────────┘                   │
└─────────────────────────────────────────────────────────────────┘
                                    │
                          HTTP + API Key Auth
                                    │
                          ┌─────────────────┐
                          │  Spring Boot API │
                          └─────────────────┘
```

### 3.2 Folder Structure

```
mcp-server/
└── src/
    ├── index.ts                  # Server entry point; registers transport
    ├── server.ts                 # McpServer instantiation; registers all tools
    │
    ├── tools/                    # Tool definitions (one file per category)
    │   ├── revenue.tools.ts      # Revenue and sales tools
    │   ├── customer.tools.ts     # Customer analytics tools
    │   ├── inventory.tools.ts    # Inventory management tools
    │   ├── analytics.tools.ts    # General analytics tools
    │   ├── product.tools.ts      # Product lookup tools
    │   ├── order.tools.ts        # Order query tools
    │   ├── export.tools.ts       # Export trigger tools
    │   └── import.tools.ts       # Import status tools
    │
    ├── client/
    │   └── api.client.ts         # Axios instance; injects API key header
    │
    ├── config/
    │   └── index.ts              # Environment config loader
    │
    ├── types/
    │   └── index.ts              # TypeScript interfaces mirroring backend DTOs
    │
    └── utils/
        ├── error.handler.ts      # Centralized error-to-MCP-error conversion
        └── logger.ts             # Structured logger
```

---

## 4. Transport Design

### 4.1 Transport Selection

The MCP server supports two transport modes:

| Mode | Use Case | Configuration |
|------|----------|---------------|
| **stdio** | Used when the AI runs in-process (e.g., Claude Desktop integration) | Default for Claude Desktop |
| **SSE (Server-Sent Events)** | Used when the MCP server is a remote HTTP service | REST-based MCP clients |

### 4.2 Startup Logic

```typescript
// Pseudocode — not production code
const transport = process.env.MCP_TRANSPORT === 'sse'
  ? new SSEServerTransport('/mcp', expressApp)
  : new StdioServerTransport()

await server.connect(transport)
```

### 4.3 Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `MCP_TRANSPORT` | `stdio` or `sse` | `stdio` |
| `PORT` | HTTP port (SSE mode only) | `3001` |
| `BACKEND_API_URL` | Spring Boot base URL | `http://localhost:8080/api/v1` |
| `MCP_API_KEY` | API key for backend authentication | — (required) |
| `LOG_LEVEL` | Logging verbosity | `info` |

---

## 5. Tool Registry

### 5.1 Tool Registration Pattern

All tools are registered on the `McpServer` instance at startup. Each tool file exports a function that accepts the server instance and registers its tools:

```typescript
// Pattern (pseudocode)
// tools/revenue.tools.ts
export function registerRevenueTools(server: McpServer, client: ApiClient): void {
  server.tool(
    'get_revenue_summary',
    'Get total revenue, order count, and average order value for a period',
    { period: z.enum(['day', 'week', 'month', 'quarter', 'year']) },
    async ({ period }) => {
      const data = await client.get(`/analytics/revenue?period=${period}`)
      return { content: [{ type: 'text', text: JSON.stringify(data) }] }
    }
  )
}
```

---

## 6. Tool Categories & Definitions

### 6.1 Revenue Tools

| Tool Name | Description | Input Schema | Backend Endpoint |
|-----------|-------------|-------------|-----------------|
| `get_revenue_summary` | Total revenue, orders, AOV for a period | `{ period: 'day'|'week'|'month'|'quarter'|'year' }` | `GET /analytics/revenue/summary` |
| `get_revenue_trend` | Revenue over time as time-series data | `{ period, granularity: 'day'|'week'|'month' }` | `GET /analytics/revenue/trend` |
| `get_revenue_by_category` | Revenue broken down by product category | `{ period }` | `GET /analytics/revenue/by-category` |
| `compare_revenue_periods` | Compare two periods' revenue | `{ period1, period2 }` | `GET /analytics/revenue/compare` |

### 6.2 Customer Tools

| Tool Name | Description | Input Schema | Backend Endpoint |
|-----------|-------------|-------------|-----------------|
| `get_customer_summary` | Total customers, new vs. returning | `{ period }` | `GET /analytics/customers/summary` |
| `get_top_customers` | Top N customers by lifetime value | `{ limit: number }` | `GET /analytics/customers/top` |
| `get_customer_details` | Full customer profile + order history | `{ customerId: string }` | `GET /customers/{id}` |
| `search_customers` | Find customers by name or email | `{ query: string }` | `GET /customers?search={query}` |

### 6.3 Inventory Tools

| Tool Name | Description | Input Schema | Backend Endpoint |
|-----------|-------------|-------------|-----------------|
| `get_inventory_status` | Current stock levels for all products | `{ threshold?: number }` | `GET /inventory` |
| `get_low_stock_products` | Products below stock threshold | `{ threshold: number }` | `GET /inventory/low-stock` |
| `get_product_stock` | Stock level for a specific product | `{ productId: string }` | `GET /inventory/{productId}` |

### 6.4 Analytics Tools

| Tool Name | Description | Input Schema | Backend Endpoint |
|-----------|-------------|-------------|-----------------|
| `get_top_products` | Top N products by revenue or units | `{ limit, metric: 'revenue'|'units', period }` | `GET /analytics/products/top` |
| `get_order_analytics` | Order count, fulfillment rate, cancellation rate | `{ period }` | `GET /analytics/orders/summary` |
| `get_category_performance` | Revenue and units per category | `{ period }` | `GET /analytics/categories` |
| `get_dashboard_summary` | All KPIs in one call (for dashboard widget) | `{ period }` | `GET /analytics/dashboard` |

### 6.5 Order Tools

| Tool Name | Description | Input Schema | Backend Endpoint |
|-----------|-------------|-------------|-----------------|
| `get_recent_orders` | Latest N orders | `{ limit: number }` | `GET /orders?sort=createdAt,desc&size={limit}` |
| `get_order_details` | Full order with line items | `{ orderId: string }` | `GET /orders/{id}` |
| `search_orders` | Search by status, date range, customer | `{ status?, startDate?, endDate?, customerId? }` | `GET /orders?{filters}` |

### 6.6 Product Tools

| Tool Name | Description | Input Schema | Backend Endpoint |
|-----------|-------------|-------------|-----------------|
| `search_products` | Find products by name, SKU, or category | `{ query?, categoryId? }` | `GET /products?search={query}` |
| `get_product_details` | Full product info | `{ productId: string }` | `GET /products/{id}` |

### 6.7 Export Tools

| Tool Name | Description | Input Schema | Backend Endpoint |
|-----------|-------------|-------------|-----------------|
| `trigger_export` | Initiate a data export job | `{ type: 'orders'|'products'|'analytics', format: 'pdf'|'excel', filters? }` | `POST /export` |
| `get_export_status` | Check status of an export job | `{ jobId: string }` | `GET /export/{jobId}/status` |

---

## 7. Security Design

### 7.1 MCP-to-Backend Authentication

The MCP server authenticates with the Spring Boot API using a **dedicated API key**, NOT a user JWT. This key is:
- Stored in `MCP_API_KEY` environment variable
- Injected by the Axios interceptor into every request as `X-MCP-API-KEY: {key}`
- Validated by a Spring Security filter on the backend

The backend Spring Security filter chain has a second filter path for MCP:
```
MCP Request → McpApiKeyFilter → validates X-MCP-API-KEY → grants MCP_SERVICE role
```

### 7.2 What the MCP Service Role Can Access

The MCP service role maps to a set of read-only endpoints:
- All `GET` analytics endpoints
- All `GET` product, order, customer, inventory endpoints
- `POST` on export trigger endpoints

The MCP server **cannot** create, update, or delete business entities.

### 7.3 Tool Input Validation

Every tool input is validated with Zod before any API call is made:

```typescript
// All inputs validated BEFORE the API call
const schema = z.object({
  period: z.enum(['day', 'week', 'month', 'quarter', 'year']),
  limit: z.number().int().min(1).max(100).optional().default(10)
})
```

Invalid inputs return a structured MCP error without touching the backend.

### 7.4 No Secrets in Tool Outputs

Tool outputs must **never** include:
- User passwords or password hashes
- JWT tokens or API keys
- Internal system information (stack traces, DB connection strings)
- PII beyond what is required to answer the query

---

## 8. Error Handling

### 8.1 Error Categories

| Category | Trigger | MCP Error Code | HTTP Status |
|----------|---------|---------------|------------|
| Validation Error | Zod schema failure | `InvalidParams` | — (caught before HTTP) |
| Backend Unavailable | Network timeout / connection refused | `InternalError` | — |
| Backend 4xx | Backend returns 400/401/403/404 | `InvalidRequest` | 4xx |
| Backend 5xx | Backend returns 500 | `InternalError` | 5xx |
| Rate Limit | Backend returns 429 | `InternalError` | 429 |

### 8.2 Error Response Format

All tool errors return a structured MCP error response:

```json
{
  "content": [
    {
      "type": "text",
      "text": "Error: Unable to retrieve revenue summary. The analytics service returned: 503 Service Unavailable. Please try again in a moment."
    }
  ],
  "isError": true
}
```

Human-readable error messages are returned to the LLM so it can communicate the issue naturally to the user.

### 8.3 Retry Logic

| Condition | Retry Strategy |
|-----------|---------------|
| Network timeout | 3 retries with exponential backoff (1s, 2s, 4s) |
| Backend 503 | 2 retries with linear backoff (2s, 4s) |
| Backend 429 | Respect `Retry-After` header |
| Backend 4xx (client error) | No retry |

---

## 9. Request / Response Contract

### 9.1 Axios Client Configuration

```typescript
// Conceptual configuration (not production code)
{
  baseURL: config.backendApiUrl,     // e.g., http://localhost:8080/api/v1
  timeout: 10000,                    // 10 second timeout
  headers: {
    'X-MCP-API-KEY': config.mcpApiKey,
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  }
}
```

### 9.2 Backend API Response Contract

The MCP server expects all backend responses to follow the standard `ApiResponse<T>` envelope:

```json
{
  "success": true,
  "data": { ... },
  "message": "OK",
  "timestamp": "2026-07-06T10:00:00Z"
}
```

The MCP client extracts `response.data.data` and serializes it as the tool output.

---

## 10. Implementation Guidelines

### 10.1 Tool Naming Convention

| Convention | Rule |
|------------|------|
| Format | `snake_case` verbs: `get_`, `search_`, `compare_`, `trigger_` |
| Clarity | Names must be self-explanatory to an LLM |
| Prefix | Use domain prefix: `get_revenue_*`, `get_customer_*` |

### 10.2 Tool Description Quality

LLMs use tool descriptions to decide WHEN to call each tool. Descriptions must be:
- Specific and unambiguous
- Include the period/filter options in the description
- Mention what data is returned

**Good:** `"Get the total revenue, number of orders, and average order value for a given time period (day, week, month, quarter, or year)."`

**Bad:** `"Get revenue data"`

### 10.3 Response Serialization

All tool responses serialize data as:

```typescript
return {
  content: [{
    type: 'text',
    text: JSON.stringify(data, null, 2)
  }]
}
```

This gives the LLM structured data it can interpret and summarize for the user.

---

## 11. Future Extension

| Extension | Description | Priority |
|-----------|-------------|----------|
| Predictive tools | `predict_demand`, `forecast_revenue` | Medium |
| Write tools | `create_order`, `update_inventory` (with strict guards) | Low |
| Multi-tenant tools | Tool inputs include `tenantId` | Future |
| Streaming responses | Stream long export operations | Medium |
| Tool caching | Cache tool responses for repeated queries | Medium |
| OpenTelemetry tracing | Trace tool calls end-to-end | High |
