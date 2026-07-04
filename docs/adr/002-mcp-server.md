# ADR-002: MCP Server for AI Agent Integration

**Status**: Accepted
**Date**: 2026-07-04

## Context

Commerce Insight AI needs to expose its data and capabilities to AI agents (e.g., Claude, GPT-based tools) in a standardized, safe, and maintainable way.

Options considered:
1. **Direct database access** — AI agent queries PostgreSQL directly
2. **Custom AI API endpoints** — Bespoke REST endpoints built only for AI
3. **Model Context Protocol (MCP)** — Standardized protocol with official SDK

## Decision

We adopt **MCP (Model Context Protocol)** via a dedicated `mcp-server` Node.js module.

**Critical constraint:** The MCP server **never** accesses the database directly. All data flows through the Spring Boot REST API.

```
AI Agent → MCP Protocol → mcp-server → HTTP REST → Spring Boot API → PostgreSQL
```

The MCP server exposes:
- **Tools** — callable functions (e.g., `get_revenue_summary`, `list_products`)
- **Resources** — readable data (e.g., product catalog snapshots)
- **Prompts** — pre-built prompt templates for analytics queries

## Consequences

**Positive:**
- Standardized integration that works with any MCP-compatible AI agent
- Security: DB is never exposed outside the Spring Boot API boundary
- Separation of concerns: MCP adapter vs. business logic are cleanly separated
- Future-proof: MCP is becoming the industry standard for AI tool integration

**Negative:**
- Extra network hop (MCP server → API) vs. direct DB access
- Additional service to maintain and deploy
- Requires Node.js expertise alongside Java
