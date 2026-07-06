# 02 — Product Requirements Document (PRD)
# Commerce Insight AI

> **Document Type**: Product Requirements
> **Version**: 1.0.0
> **Status**: Approved
> **Last Updated**: 2026-07-06
> **Owner**: Chief Solution Architect

---

## Table of Contents

1. [Purpose](#1-purpose)
2. [Functional Requirements](#2-functional-requirements)
3. [Non-Functional Requirements](#3-non-functional-requirements)
4. [User Stories](#4-user-stories)
5. [Acceptance Criteria](#5-acceptance-criteria)
6. [Business Rules](#6-business-rules)
7. [Feature List & Priorities](#7-feature-list--priorities)
8. [Future Features](#8-future-features)

---

## 1. Purpose

This Product Requirements Document (PRD) defines what the Commerce Insight AI platform must do. It translates the strategic vision into concrete, implementable requirements. Each requirement is traceable to a business goal defined in `01_PROJECT_VISION.md`.

This document is the authoritative reference for:
- Sprint planning and task breakdown
- Backend API design
- Frontend feature development
- QA test case creation

---

## 2. Functional Requirements

### FR-AUTH: Authentication & Authorization

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-AUTH-01 | The system SHALL allow users to register with email and password | Must Have |
| FR-AUTH-02 | The system SHALL authenticate users using JWT Access Tokens (15-minute expiry) | Must Have |
| FR-AUTH-03 | The system SHALL issue Refresh Tokens (7-day expiry) with automatic rotation | Must Have |
| FR-AUTH-04 | The system SHALL detect and invalidate refresh token reuse (token family revocation) | Must Have |
| FR-AUTH-05 | The system SHALL enforce RBAC with three roles: ADMIN, MANAGER, STAFF | Must Have |
| FR-AUTH-06 | The system SHALL allow users to log out and revoke all refresh tokens | Must Have |
| FR-AUTH-07 | The system SHALL lock accounts after 5 consecutive failed login attempts | Should Have |
| FR-AUTH-08 | The system SHALL allow admins to reset user passwords | Should Have |

### FR-DASH: Dashboard

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-DASH-01 | The dashboard SHALL display total revenue for selected period | Must Have |
| FR-DASH-02 | The dashboard SHALL display total orders, total customers, total products | Must Have |
| FR-DASH-03 | The dashboard SHALL display a revenue trend line chart (daily/weekly/monthly) | Must Have |
| FR-DASH-04 | The dashboard SHALL display top 5 selling products | Must Have |
| FR-DASH-05 | The dashboard SHALL display recent orders list (last 10) | Must Have |
| FR-DASH-06 | The dashboard SHALL display low stock alerts | Should Have |
| FR-DASH-07 | The dashboard SHALL include an AI insight summary widget | Should Have |
| FR-DASH-08 | The dashboard SHALL support date range filtering | Should Have |

### FR-PROD: Products

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-PROD-01 | The system SHALL support product creation with: name, SKU, description, price, category, image URL | Must Have |
| FR-PROD-02 | The system SHALL support product listing with pagination, search, and category filter | Must Have |
| FR-PROD-03 | The system SHALL support product update (all fields) | Must Have |
| FR-PROD-04 | The system SHALL support soft delete of products | Must Have |
| FR-PROD-05 | The system SHALL enforce SKU uniqueness per store | Must Have |
| FR-PROD-06 | The system SHALL support product bulk import via CSV and Excel | Must Have |
| FR-PROD-07 | The system SHALL track product-level sales metrics | Should Have |

### FR-CAT: Categories

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-CAT-01 | The system SHALL support hierarchical categories (parent/child, 2 levels) | Must Have |
| FR-CAT-02 | The system SHALL allow products to be assigned to one category | Must Have |
| FR-CAT-03 | The system SHALL prevent deletion of categories with active products | Must Have |
| FR-CAT-04 | The system SHALL provide category performance analytics | Should Have |

### FR-CUST: Customers

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-CUST-01 | The system SHALL store customer profiles: name, email, phone, address | Must Have |
| FR-CUST-02 | The system SHALL display order history per customer | Must Have |
| FR-CUST-03 | The system SHALL calculate customer lifetime value (LTV) | Should Have |
| FR-CUST-04 | The system SHALL support customer search by name and email | Must Have |
| FR-CUST-05 | The system SHALL support customer bulk import via CSV | Should Have |

### FR-ORD: Orders

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-ORD-01 | The system SHALL support order creation with line items | Must Have |
| FR-ORD-02 | The system SHALL track order status: PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED, REFUNDED | Must Have |
| FR-ORD-03 | The system SHALL record order total, discount, shipping fee, and tax | Must Have |
| FR-ORD-04 | The system SHALL allow status updates following a valid transition matrix | Must Have |
| FR-ORD-05 | The system SHALL link orders to customers | Must Have |
| FR-ORD-06 | The system SHALL support order search and filter by status, date range, customer | Must Have |
| FR-ORD-07 | The system SHALL automatically adjust inventory on order confirmation | Must Have |

### FR-INV: Inventory

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-INV-01 | The system SHALL track stock quantity per product | Must Have |
| FR-INV-02 | The system SHALL decrease stock on order confirmation | Must Have |
| FR-INV-03 | The system SHALL increase stock on order cancellation/refund | Must Have |
| FR-INV-04 | The system SHALL support manual stock adjustment with reason | Must Have |
| FR-INV-05 | The system SHALL alert when stock falls below configurable threshold | Should Have |
| FR-INV-06 | The system SHALL maintain an inventory movement audit log | Should Have |

### FR-ANA: Analytics

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-ANA-01 | The system SHALL provide revenue summary: total, by period, by category | Must Have |
| FR-ANA-02 | The system SHALL provide order analytics: count, average value, fulfillment rate | Must Have |
| FR-ANA-03 | The system SHALL provide top N products by revenue and by units sold | Must Have |
| FR-ANA-04 | The system SHALL provide customer analytics: new vs. returning, LTV distribution | Should Have |
| FR-ANA-05 | The system SHALL support period comparison (this period vs. last period) | Should Have |
| FR-ANA-06 | The system SHALL support date range, category, and product filtering | Must Have |

### FR-IMP: Import

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-IMP-01 | The system SHALL support CSV import for products, customers, and orders | Must Have |
| FR-IMP-02 | The system SHALL support Excel (.xlsx) import for products, customers, and orders | Must Have |
| FR-IMP-03 | The system SHALL validate each row and report errors with row numbers | Must Have |
| FR-IMP-04 | The system SHALL use atomic transactions per import job (all-or-nothing) | Must Have |
| FR-IMP-05 | The system SHALL return an import summary: success count, error count, error details | Must Have |
| FR-IMP-06 | The system SHALL reject files exceeding 10MB | Should Have |

### FR-EXP: Export

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-EXP-01 | The system SHALL support PDF export for orders, analytics reports | Must Have |
| FR-EXP-02 | The system SHALL support Excel export for orders, products, analytics | Must Have |
| FR-EXP-03 | The system SHALL apply the same filters used in the UI to exported data | Must Have |
| FR-EXP-04 | The system SHALL include branding (logo, timestamp) on PDF exports | Should Have |

### FR-AI: AI Assistant

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-AI-01 | The system SHALL provide a chat interface for natural language analytics queries | Must Have |
| FR-AI-02 | The AI SHALL retrieve all data through MCP tools only (never direct DB access) | Must Have |
| FR-AI-03 | The system SHALL support multiple LLM providers: OpenAI, Claude, Gemini, Ollama | Must Have |
| FR-AI-04 | The system SHALL maintain conversation history within a session | Must Have |
| FR-AI-05 | The system SHALL gracefully handle LLM provider failures with fallback | Should Have |
| FR-AI-06 | The system SHALL display which tools the AI invoked for transparency | Should Have |

### FR-ADM: Administration

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-ADM-01 | Admins SHALL be able to create, view, update, and deactivate user accounts | Must Have |
| FR-ADM-02 | Admins SHALL be able to assign and change user roles | Must Have |
| FR-ADM-03 | The system SHALL maintain a searchable audit log of all critical operations | Should Have |
| FR-ADM-04 | Admins SHALL be able to configure LLM provider and API keys | Should Have |

---

## 3. Non-Functional Requirements

### NFR-PERF: Performance

| ID | Requirement | Metric |
|----|-------------|--------|
| NFR-PERF-01 | All read API endpoints SHALL respond within 300ms at P95 under normal load | P95 < 300ms |
| NFR-PERF-02 | Dashboard KPI aggregations SHALL be served within 500ms | P95 < 500ms |
| NFR-PERF-03 | Import of 1,000 rows SHALL complete within 30 seconds | < 30s |
| NFR-PERF-04 | PDF export of 500 orders SHALL complete within 15 seconds | < 15s |

### NFR-SEC: Security

| ID | Requirement |
|----|-------------|
| NFR-SEC-01 | All API endpoints SHALL require authentication (except /auth/**) |
| NFR-SEC-02 | All passwords SHALL be hashed with BCrypt (strength ≥ 12) |
| NFR-SEC-03 | All API responses SHALL include appropriate security headers |
| NFR-SEC-04 | The system SHALL validate ALL input at the API boundary |
| NFR-SEC-05 | The system SHALL not expose internal error details in production responses |

### NFR-QUAL: Code Quality

| ID | Requirement |
|----|-------------|
| NFR-QUAL-01 | Backend test coverage SHALL be ≥ 80% (unit + integration) |
| NFR-QUAL-02 | All API endpoints SHALL be documented via OpenAPI / Swagger |
| NFR-QUAL-03 | Code SHALL pass all CI lint and type checks |
| NFR-QUAL-04 | No entity SHALL be returned directly from API endpoints (DTO only) |

### NFR-MAINT: Maintainability

| ID | Requirement |
|----|-------------|
| NFR-MAINT-01 | All database changes SHALL be managed via Flyway migrations |
| NFR-MAINT-02 | Each module SHALL have zero cross-module repository access |
| NFR-MAINT-03 | All cross-cutting concerns (logging, exception handling) SHALL be centralized |

### NFR-SCALE: Scalability

| ID | Requirement |
|----|-------------|
| NFR-SCALE-01 | The architecture SHALL allow extraction of individual modules to microservices without major refactoring |
| NFR-SCALE-02 | The database schema SHALL support future multi-tenancy (tenant_id columns prepared) |

---

## 4. User Stories

### Authentication Stories

| Story ID | As a... | I want to... | So that... |
|----------|---------|--------------|------------|
| US-AUTH-01 | New user | Register with my email and a secure password | I can access the platform |
| US-AUTH-02 | Returning user | Log in with email and password and receive a JWT | I can make authenticated API calls |
| US-AUTH-03 | Logged-in user | Have my session automatically extended without re-logging in | I have a seamless experience |
| US-AUTH-04 | Logged-in user | Log out and know all my tokens are revoked | My account is secure |
| US-AUTH-05 | Admin | Reset another user's password | I can help users who are locked out |

### Dashboard Stories

| Story ID | As a... | I want to... | So that... |
|----------|---------|--------------|------------|
| US-DASH-01 | Manager | See total revenue for the current month on login | I immediately understand business performance |
| US-DASH-02 | Manager | See a revenue trend chart for the last 30 days | I can identify trends quickly |
| US-DASH-03 | Manager | See the top 5 best-selling products | I know what to prioritize |
| US-DASH-04 | Manager | See recent order activity | I can respond to issues quickly |
| US-DASH-05 | Manager | Ask the AI assistant a question about my sales | I get instant insight without building reports |

### Product Stories

| Story ID | As a... | I want to... | So that... |
|----------|---------|--------------|------------|
| US-PROD-01 | Manager | Add a new product with SKU, price, and category | It becomes available in orders and inventory |
| US-PROD-02 | Staff | View all products with search and filter | I can find a specific product quickly |
| US-PROD-03 | Manager | Update a product's price | Pricing stays accurate |
| US-PROD-04 | Admin | Delete a product that is discontinued | The catalog stays clean |
| US-PROD-05 | Manager | Import 500 products from a CSV file | I don't have to enter them manually |

### Analytics Stories

| Story ID | As a... | I want to... | So that... |
|----------|---------|--------------|------------|
| US-ANA-01 | Manager | View revenue broken down by category | I know which product lines are profitable |
| US-ANA-02 | Manager | Compare this month's revenue to last month | I can measure growth |
| US-ANA-03 | Manager | Export an analytics report to PDF | I can share it with stakeholders |
| US-ANA-04 | Manager | Ask the AI "Which product had the most returns this quarter?" | I get an answer without writing SQL |

### AI Stories

| Story ID | As a... | I want to... | So that... |
|----------|---------|--------------|------------|
| US-AI-01 | Manager | Type a question in plain English and get a business answer | I don't need to know SQL or navigate dashboards |
| US-AI-02 | Admin | Switch the LLM provider from OpenAI to Claude | I can control AI costs |
| US-AI-03 | Manager | See which data the AI accessed to answer my question | I can trust the response |

---

## 5. Acceptance Criteria

### AC-AUTH-01 (Login)

```
GIVEN a registered user with valid credentials
WHEN POST /api/v1/auth/login with correct email and password
THEN the response is 200 OK
AND the response body contains { accessToken, refreshToken, expiresIn, user }
AND the accessToken is a valid JWT
AND the JWT expires in 15 minutes
AND the refreshToken is stored (hashed) in the database
```

### AC-AUTH-03 (Refresh)

```
GIVEN a user with a valid, non-expired refresh token
WHEN POST /api/v1/auth/refresh with { refreshToken }
THEN the response is 200 OK
AND the response contains a new accessToken and a new refreshToken
AND the old refreshToken is marked as revoked in the database
AND the new refreshToken is stored (hashed) in the database
```

### AC-PROD-01 (Create Product)

```
GIVEN an authenticated MANAGER user
WHEN POST /api/v1/products with valid product data
THEN the response is 201 Created
AND the response contains the created product as ProductResponse DTO
AND the entity is persisted in the database
AND no entity fields are exposed directly (DTO mapping confirmed)
```

### AC-IMP-01 (CSV Import)

```
GIVEN an authenticated MANAGER user
AND a valid CSV file with 100 product rows
WHEN POST /api/v1/import/products with the file
THEN the response is 200 OK
AND the response contains { successCount: 100, errorCount: 0, errors: [] }
AND 100 products are persisted in the database
```

```
GIVEN a CSV file with 98 valid rows and 2 invalid rows (missing required field)
WHEN POST /api/v1/import/products with the file
THEN the response is 200 OK
AND the response contains { successCount: 0, errorCount: 2, errors: [{row: X, reason: ...}] }
AND NO products are persisted (atomic transaction)
```

---

## 6. Business Rules

| Rule ID | Rule Description |
|---------|-----------------|
| BR-01 | A product's SKU must be unique within the store |
| BR-02 | An order cannot be deleted; it can only be cancelled or refunded |
| BR-03 | Order status transitions must follow: PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED. Cancellation is allowed from PENDING or CONFIRMED only. Refund is allowed from DELIVERED only. |
| BR-04 | Stock quantity cannot go below zero |
| BR-05 | A category with products cannot be deleted |
| BR-06 | Only ADMIN role can change another user's role |
| BR-07 | A user cannot change their own role |
| BR-08 | AI must not generate SQL or access the database directly |
| BR-09 | The MCP server must not access the database directly; it must use the REST API |
| BR-10 | CSV/Excel imports are atomic: all rows succeed, or none are persisted |
| BR-11 | Soft-deleted records must not appear in standard list queries |
| BR-12 | All monetary values are stored in the database as DECIMAL(19,4) |
| BR-13 | Prices must be non-negative |

---

## 7. Feature List & Priorities

### Must Have (MVP)

| Feature | Module | Sprint |
|---------|--------|--------|
| JWT Authentication + Refresh Tokens | Auth | 2 |
| RBAC (Admin/Manager/Staff) | Auth | 2 |
| Product CRUD | Products | 3 |
| Category Management | Categories | 3 |
| Customer Management | Customers | 3 |
| Order Management + Status Flow | Orders | 3 |
| Inventory Tracking | Inventory | 4 |
| Revenue Analytics | Analytics | 4 |
| Dashboard KPIs | Dashboard | 4 |
| CSV Import (Products, Orders) | Import | 5 |
| Excel Import | Import | 5 |
| PDF/Excel Export | Export | 5 |
| AI Chat Interface | AI | 6 |
| MCP Server with Tool Registry | MCP | 6 |

### Should Have

| Feature | Module | Sprint |
|---------|--------|--------|
| Period Comparison Analytics | Analytics | 4 |
| Low Stock Alerts | Inventory | 4 |
| Customer Lifetime Value | Customers | 5 |
| Audit Logs | Admin | 5 |
| LLM Provider Switching (Admin UI) | AI/Admin | 6 |
| AI Tool Transparency (show tools used) | AI | 7 |

### Nice to Have

| Feature | Module | Sprint |
|---------|--------|--------|
| Account lockout after failed attempts | Auth | TBD |
| Password history enforcement | Auth | TBD |
| Category Performance Analytics | Analytics | TBD |
| Inventory Movement History | Inventory | TBD |

---

## 8. Future Features

| Feature | Description | Rationale |
|---------|-------------|-----------|
| Real-time notifications | WebSocket alerts for low stock, new orders | Operational responsiveness |
| Shopify/WooCommerce sync | Webhook integration with popular platforms | Market fit for real merchants |
| Multi-tenancy | Isolated data per merchant | SaaS business model |
| Predictive analytics | ML-based demand forecasting | AI product differentiation |
| Mobile application | React Native companion app | Cross-device accessibility |
| OAuth 2.0 / OIDC | Social login via Google, GitHub | User convenience |
| Scheduled reports | Email PDF reports on a schedule | Business value delivery |
| MFA | TOTP for admin accounts | Enhanced security |
