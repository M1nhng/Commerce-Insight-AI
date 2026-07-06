# 01 — Project Vision
# Commerce Insight AI

> **Document Type**: Vision & Strategy
> **Version**: 1.0.0
> **Status**: Approved
> **Last Updated**: 2026-07-06
> **Owner**: Chief Solution Architect

---

## Table of Contents

1. [Purpose](#1-purpose)
2. [Project Background](#2-project-background)
3. [Problem Statement](#3-problem-statement)
4. [Mission](#4-mission)
5. [Vision](#5-vision)
6. [Business Goals](#6-business-goals)
7. [Target Users](#7-target-users)
8. [Project Scope](#8-project-scope)
9. [Out of Scope](#9-out-of-scope)
10. [Future Expansion](#10-future-expansion)
11. [Success Metrics](#11-success-metrics)

---

## 1. Purpose

This document defines the strategic vision, mission, and business goals for the **Commerce Insight AI** platform. It serves as the north star for all technical and product decisions throughout the project lifecycle. Every sprint, feature, and architectural choice must align with the vision stated here.

This document is intended for:
- Engineering team (architectural alignment)
- Product stakeholders (scope validation)
- Evaluators and reviewers (portfolio context)

---

## 2. Project Background

The modern ecommerce landscape is data-rich but insight-poor. Small and medium-sized merchants generate enormous volumes of transactional data — orders, customer behavior, inventory movement, revenue fluctuations — yet most lack the tools to extract actionable intelligence from it.

Existing analytics tools in the market either:
- Require significant data engineering expertise to operate
- Are locked behind expensive enterprise SaaS subscriptions
- Provide generic dashboards with no contextual AI interpretation
- Force merchants to export data manually for any meaningful analysis

**Commerce Insight AI** is designed to bridge this gap. It is a full-stack, AI-powered ecommerce analytics platform that unifies data management, real-time analytics, and natural language AI insights in a single, cohesive product.

This project is also a **portfolio-quality software product** built to demonstrate enterprise-level engineering capabilities in the areas of:
- Backend API design (Spring Boot, Modular Monolith)
- Frontend engineering (React 19, TypeScript, TailwindCSS)
- AI integration with provider abstraction (OpenAI, Claude, Gemini, Ollama)
- Model Context Protocol (MCP) implementation for AI agent tooling
- Security engineering (JWT, RBAC, Spring Security)
- Data engineering (CSV/Excel import, PDF/Excel export, Flyway)

---

## 3. Problem Statement

### 3.1 Core Problems

| # | Problem | Impact |
|---|---------|--------|
| P1 | Merchants cannot quickly understand their revenue trends, top products, or customer behavior without manual spreadsheet work | Lost business opportunities, slow decision-making |
| P2 | No affordable, integrated tool exists that combines product management, order tracking, and AI-driven analytics | Fragmented tooling, data silos |
| P3 | AI assistants (ChatGPT, Claude) are general-purpose and cannot access live business data | Low AI relevance, hallucinated insights |
| P4 | Importing and exporting data requires custom scripts or expensive ETL tools | High operational overhead |
| P5 | Small teams with no data engineering background cannot self-serve analytics | Dependency on external consultants |

### 3.2 Root Cause Analysis

The fundamental root cause is the **absence of a unified platform** that:
1. Stores and manages ecommerce operational data (products, orders, customers, inventory)
2. Processes and visualizes that data as real-time analytics
3. Exposes that data securely to AI agents through a standardized protocol (MCP)
4. Allows AI to interpret business context and generate natural-language insights

---

## 4. Mission

> **To empower merchants and analysts with an intelligent, unified platform that transforms raw ecommerce data into clear, actionable insights — through the power of AI, delivered through an enterprise-quality product experience.**

---

## 5. Vision

> **Commerce Insight AI will become the reference implementation of how modern AI-powered business intelligence platforms should be architected — combining clean API design, provider-agnostic AI integration, and the emerging Model Context Protocol standard.**

In three years, the platform will:
- Support multi-tenant merchant onboarding
- Integrate with major ecommerce platforms (Shopify, WooCommerce, Lazada) via webhooks
- Offer a marketplace of AI-powered analytics tools via MCP
- Provide predictive analytics using fine-tuned domain-specific models

---

## 6. Business Goals

### 6.1 Primary Goals

| Goal ID | Goal Description | Priority |
|---------|-----------------|----------|
| BG-01 | Deliver a fully functional ecommerce analytics platform that demonstrates professional-grade backend architecture | Critical |
| BG-02 | Implement a provider-agnostic AI layer supporting OpenAI, Claude, Gemini, and Ollama through one interface | Critical |
| BG-03 | Implement the Model Context Protocol (MCP) as the primary AI-data bridge | Critical |
| BG-04 | Achieve RBAC-protected multi-role access (Admin, Manager, Staff) | High |
| BG-05 | Support bulk data operations: CSV/Excel import, PDF/Excel export | High |
| BG-06 | Build a responsive, professional-grade React frontend with dark mode | High |

### 6.2 Portfolio Goals

| Goal ID | Goal Description |
|---------|-----------------|
| PG-01 | Demonstrate mastery of Spring Boot 3.5 Modular Monolith architecture |
| PG-02 | Demonstrate React 19 / TypeScript / TailwindCSS / Shadcn UI proficiency |
| PG-03 | Demonstrate practical AI integration using MCP |
| PG-04 | Demonstrate production-quality code: DTOs, MapStruct, Flyway, Bean Validation |
| PG-05 | Demonstrate security engineering: JWT, Refresh Tokens, RBAC |
| PG-06 | Demonstrate test engineering: JUnit5, Mockito, Testcontainers |

---

## 7. Target Users

### 7.1 Primary Users

#### 7.1.1 Ecommerce Manager / Store Owner
- **Role**: Has full visibility into business performance
- **Needs**: Revenue dashboards, order management, customer analytics, AI insights
- **Pain Points**: Spending hours on manual reports, no real-time visibility
- **Maps to System Role**: `ADMIN` or `MANAGER`

#### 7.1.2 Operations Staff
- **Role**: Manages day-to-day order processing and inventory
- **Needs**: Order status management, inventory tracking, product updates
- **Pain Points**: No centralized tool, using spreadsheets
- **Maps to System Role**: `STAFF`

#### 7.1.3 Data Analyst (Internal)
- **Role**: Analyzes business performance, prepares reports
- **Needs**: Exportable data (PDF/Excel), analytics dashboards, AI-assisted analysis
- **Pain Points**: Manual export-import cycles, no AI to help interpret data
- **Maps to System Role**: `MANAGER`

### 7.2 Secondary Users

#### 7.2.1 System Administrator
- **Role**: Manages the platform itself (users, roles, system health)
- **Maps to System Role**: `ADMIN`

#### 7.2.2 AI Agents (Non-Human)
- **Type**: MCP-compatible AI agents (Claude Desktop, custom agents)
- **Needs**: Access to business data via MCP tools without direct DB access
- **Access Method**: MCP protocol → REST API

---

## 8. Project Scope

### 8.1 In-Scope Modules

| Module | Description |
|--------|-------------|
| **Authentication** | JWT login, refresh token rotation, RBAC with Admin/Manager/Staff roles |
| **Dashboard** | KPI cards, revenue charts, recent activity, AI summary widget |
| **Products** | CRUD, categories assignment, SKU management, image URL support |
| **Categories** | Hierarchical product categorization (parent/child) |
| **Customers** | Customer profiles, order history, lifetime value |
| **Orders** | Order lifecycle management, line items, status tracking |
| **Inventory** | Stock tracking, low-stock alerts, restock history |
| **Analytics** | Revenue trends, top products, category performance, customer segments |
| **Import** | CSV and Excel file import for products, orders, customers |
| **Export** | PDF and Excel export for reports, orders, analytics |
| **AI Assistant** | Natural language business Q&A via MCP-connected LLM |
| **Administration** | User management, role assignment, system audit logs |

### 8.2 Technical Scope

| Layer | Technology | Scope |
|-------|-----------|-------|
| Backend | Spring Boot 3.5 | Full REST API, security, data layer |
| Frontend | React 19 + TypeScript | Full SPA with all module UIs |
| AI | OpenAI, Claude, Gemini, Ollama | Pluggable LLM provider layer |
| MCP | Node.js + MCP SDK | Full MCP server with tools registry |
| Database | PostgreSQL 16 | Primary operational store |
| Migrations | Flyway | Schema versioning |
| CI/CD | GitHub Actions | Build, test, lint pipelines |

---

## 9. Out of Scope

The following are explicitly **not** part of this project:

| Item | Reason |
|------|--------|
| Payment gateway integration (Stripe, PayPal) | Scope too broad for portfolio; business logic complexity |
| Real ecommerce checkout flow | Platform is analytics-focused, not a storefront |
| Multi-tenancy (per-merchant database isolation) | Deferred to future expansion |
| Mobile native applications (iOS/Android) | Web-only for this version |
| Shopify / WooCommerce webhooks | Deferred to Phase 2 |
| Real-time WebSocket notifications | Deferred; polling-based initially |
| Direct AI database access | Explicitly prohibited by architecture rules |
| Microservices decomposition | Modular Monolith is the chosen architecture |
| Kubernetes deployment | Docker Compose is sufficient for portfolio scope |

---

## 10. Future Expansion

### Phase 2 (Post-Portfolio)

| Item | Description |
|------|-------------|
| Multi-tenancy | Tenant-per-schema isolation for SaaS model |
| Shopify Integration | Webhook receiver to sync live store data |
| WooCommerce Integration | REST API pull for product/order sync |
| WebSocket Notifications | Real-time alerts for low stock, large orders |
| Predictive Analytics | ML model for demand forecasting |

### Phase 3 (Long-term)

| Item | Description |
|------|-------------|
| MCP Tool Marketplace | Public registry of ecommerce-specific MCP tools |
| Fine-tuned AI Model | Domain-specific LLM fine-tuned on ecommerce data |
| Mobile App | React Native companion for on-the-go analytics |
| White-label Mode | Configurable branding per merchant |

---

## 11. Success Metrics

### 11.1 Technical Quality Metrics

| Metric | Target |
|--------|--------|
| Backend test coverage | ≥ 80% (unit + integration) |
| API response time (P95) | < 300ms for all read endpoints |
| CI pipeline pass rate | 100% on main branch |
| Zero critical security vulnerabilities | Verified via OWASP checklist |
| OpenAPI specification coverage | 100% of all endpoints documented |

### 11.2 Functional Completeness Metrics

| Metric | Target |
|--------|--------|
| All 13 modules fully implemented | 100% |
| All RBAC permissions enforced | 100% |
| Import/Export working for all supported formats | CSV, Excel (import); PDF, Excel (export) |
| AI assistant responds correctly using MCP tools | ≥ 5 tool categories operational |
| All Flyway migrations run cleanly | Zero migration failures |

### 11.3 Portfolio Quality Metrics

| Metric | Target |
|--------|--------|
| README provides clear setup instructions | One-command dev setup working |
| Architecture documented and visualized | All ADRs written |
| Code follows defined architecture rules | 0 architecture violations |
| Git history is clean and meaningful | Conventional Commits throughout |
