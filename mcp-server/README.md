# MCP Server — Commerce Insight AI

> Node.js · TypeScript · Model Context Protocol SDK

---

## Overview

This module implements a **Model Context Protocol (MCP) server** for Commerce Insight AI. It exposes **tools**, **resources**, and **prompts** that allow AI agents (like Claude) to interact with the platform's data through well-defined, type-safe interfaces.

### Critical Constraint

> ⚠️ **The MCP server NEVER accesses the database directly.**
> It communicates exclusively with the Spring Boot REST API.

---

## Project Structure

```
mcp-server/
├── package.json
├── tsconfig.json
├── .env.example
│
└── src/
    ├── index.ts            # MCP server entry point
    │
    ├── tools/              # MCP tool definitions (one per domain)
    │   ├── analytics.tool.ts
    │   ├── products.tool.ts
    │   ├── orders.tool.ts
    │   └── ai.tool.ts
    │
    ├── resources/          # MCP resource handlers
    ├── prompts/            # Prompt templates for AI agents
    │
    ├── client/             # HTTP client to call Spring Boot API
    │   └── api.client.ts
    │
    ├── config/             # Server config & environment loading
    │   └── index.ts
    │
    ├── types/              # TypeScript interfaces mirroring backend DTOs
    │   └── index.ts
    │
    └── utils/              # Shared helpers
```

---

## MCP Tools

| Tool File | Exposed Tools | Description |
|-----------|--------------|-------------|
| `analytics.tool.ts` | `get_revenue_summary`, `get_top_products`, `get_sales_trend` | Analytics & KPI queries |
| `products.tool.ts` | `list_products`, `get_product`, `search_products` | Product catalog access |
| `orders.tool.ts` | `list_orders`, `get_order`, `get_order_stats` | Order data queries |
| `ai.tool.ts` | `generate_insight`, `get_insights` | AI insight generation |

---

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Node.js | 20+ | Runtime |
| TypeScript | 5.x | Type safety |
| MCP SDK | latest | Model Context Protocol |
| Axios | 1.x | HTTP client |
| Zod | 3.x | Schema validation |

---

## Running Locally

```bash
# Install dependencies
npm install

# Copy environment variables
cp .env.example .env

# Start dev server
npm run dev
```

MCP server will run on: `http://localhost:3001`

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | MCP server port | `3001` |
| `BACKEND_API_URL` | Spring Boot API base URL | `http://localhost:8080` |
| `MCP_API_KEY` | API key for authenticating to backend | — |
| `LOG_LEVEL` | Logging verbosity | `info` |

---

## Communication Flow

```
AI Agent
   │
   ▼ MCP Protocol (stdio / SSE)
MCP Server
   │
   ▼ HTTP REST (with API key)
Spring Boot API
   │
   ▼ JPA / SQL
PostgreSQL
```

---

## Status

🚧 **Structure initialized** — No tools implemented yet.
