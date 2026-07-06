# 03 — Architecture Design
# Commerce Insight AI

> **Document Type**: System Architecture
> **Version**: 1.0.0
> **Status**: Approved
> **Last Updated**: 2026-07-06
> **Owner**: Chief Solution Architect

---

## Table of Contents

1. [Purpose](#1-purpose)
2. [Architecture Principles](#2-architecture-principles)
3. [Overall Architecture](#3-overall-architecture)
4. [Backend Architecture](#4-backend-architecture)
5. [Frontend Architecture](#5-frontend-architecture)
6. [MCP Architecture](#6-mcp-architecture)
7. [Module Responsibilities](#7-module-responsibilities)
8. [Folder Structure](#8-folder-structure)
9. [Sequence Diagrams](#9-sequence-diagrams)
10. [Deployment Architecture](#10-deployment-architecture)
11. [Communication Flow](#11-communication-flow)
12. [Dependency Rules](#12-dependency-rules)

---

## 1. Purpose

This document is the authoritative technical architecture reference for Commerce Insight AI. It defines how each layer of the system is structured, how components communicate, and what rules govern their interactions.

Every implementation decision MUST align with the architecture defined here. Any deviation requires a new ADR (Architecture Decision Record) in `docs/adr/`.

---

## 2. Architecture Principles

| Principle | Description | Enforcement |
|-----------|-------------|-------------|
| **Separation of Concerns** | Each layer handles exactly one type of responsibility | Code review |
| **DTO-only API boundary** | No JPA entity leaves the service layer | MapStruct mappers; code review |
| **Business logic in Service only** | Controllers are thin HTTP adapters; no business logic | Code review |
| **No cross-module repository access** | Module A cannot inject Module B's repository | Package visibility; code review |
| **AI never touches the database** | LLM providers and MCP tools only call REST APIs | Architecture constraint |
| **Validate at the boundary** | All external input validated via Bean Validation before entering service layer | Annotation-based |
| **Immutable DTOs** | Request/Response DTOs use records or `@Builder` with final fields | Lombok, Java records |
| **Fail fast** | Invalid input rejected at the controller boundary; never propagated silently | GlobalExceptionHandler |
| **Single source of truth** | Documentation here defines behavior; code implements it | Sprint discipline |

---

## 3. Overall Architecture

### 3.1 System Context

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                              Commerce Insight AI                                 │
│                                                                                  │
│   ┌─────────────────┐      ┌────────────────────┐      ┌────────────────────┐    │
│   │   React 19 SPA  │      │  Spring Boot API    │      │   MCP Server       │   │
│   │   (TypeScript)  │◄────►│  (Java 17)          │◄────►│   (Node.js/TS)     │   │
│   │   Vite          │      │  Modular Monolith   │      │   MCP SDK          │   │
│   │   TailwindCSS   │      │  REST API           │      │   Express + Axios  │   │
│   │   Shadcn UI     │      │  Port: 8080         │      │   Port: 3001       │   │
│   │   Port: 5173    │      └────────────────────┘      └────────────────────┘    │
│   └─────────────────┘                │                           │               │
│                                      │                           │               │
│                              ┌───────────────┐                   │               │
│                              │  PostgreSQL 16 │                  │               │
│                              │  Port: 5432   │                   │               │
│                              └───────────────┘                   │               │
│                                                                  │               │
│                              ┌────────────────────────────────────┐              │
│                              │         LLM Providers              │              │
│                              │  OpenAI │ Claude │ Gemini │ Ollama │              │
│                              └────────────────────────────────────┘              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Communication Protocol Summary

| From | To | Protocol | Auth Method |
|------|----|----------|-------------|
| Browser (React) | Spring Boot API | HTTPS / REST | JWT Bearer Token |
| MCP Server | Spring Boot API | HTTP / REST | X-MCP-API-KEY header |
| Spring Boot AI Module | LLM Providers | HTTPS / REST | Provider API Key |
| Spring Boot | PostgreSQL | JDBC (TCP) | DB credentials |
| Docker services | Docker services | Internal Docker Network | — |

---

## 4. Backend Architecture

### 4.1 Modular Monolith Design

The backend is a **Modular Monolith** — a single deployable Spring Boot application organized into domain modules with strict inter-module boundaries. This provides the simplicity of a monolith while maintaining the modularity needed to extract services in the future.

### 4.2 Package Structure

```
com.commerceinsight
├── CommerceInsightApplication.java        ← Spring Boot entry point
│
├── config/                                 ← Cross-cutting Spring configs
│   ├── SecurityConfig.java                 ← Spring Security filter chain
│   ├── CorsConfig.java                     ← CORS configuration
│   ├── JpaConfig.java                      ← JPA / Hibernate settings
│   ├── OpenApiConfig.java                  ← Swagger / SpringDoc config
│   └── WebMvcConfig.java                   ← MVC configuration
│
├── exception/                              ← Global exception handling
│   ├── GlobalExceptionHandler.java         ← @ControllerAdvice
│   ├── ResourceNotFoundException.java
│   ├── ValidationException.java
│   ├── BusinessRuleException.java
│   └── ImportException.java
│
├── security/                               ← JWT security infrastructure
│   ├── JwtAuthenticationFilter.java        ← Per-request token validation
│   ├── JwtTokenUtil.java                   ← JWT create/parse/validate
│   ├── UserDetailsServiceImpl.java         ← Load user from DB
│   ├── McpApiKeyFilter.java                ← MCP server auth filter
│   └── SecurityContextHelper.java          ← Get current user from context
│
├── shared/                                 ← Cross-module shared code
│   ├── dto/
│   │   ├── ApiResponse.java                ← Standard response envelope
│   │   ├── PageResponse.java               ← Paginated response
│   │   └── ErrorResponse.java              ← Error response
│   ├── base/
│   │   └── BaseEntity.java                 ← id, createdAt, updatedAt, deletedAt
│   ├── util/
│   │   ├── SlugUtil.java
│   │   └── DateUtil.java
│   └── exception/
│       └── ErrorCode.java                  ← Enum of application error codes
│
├── auth/                                   ← DOMAIN: Authentication
│   ├── controller/
│   │   └── AuthController.java
│   ├── service/
│   │   ├── AuthService.java
│   │   └── RefreshTokenService.java
│   ├── repository/
│   │   └── RefreshTokenRepository.java
│   ├── domain/
│   │   └── RefreshToken.java
│   ├── dto/
│   │   ├── request/
│   │   │   ├── LoginRequest.java
│   │   │   └── RegisterRequest.java
│   │   └── response/
│   │       └── AuthResponse.java
│   └── mapper/
│       └── AuthMapper.java
│
├── user/                                   ← DOMAIN: User management
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── domain/
│   │   ├── User.java
│   │   └── Role.java (Enum: ADMIN, MANAGER, STAFF)
│   ├── dto/
│   └── mapper/
│
├── product/                                ← DOMAIN: Product catalog
├── category/                               ← DOMAIN: Categories
├── customer/                               ← DOMAIN: Customers
├── order/                                  ← DOMAIN: Orders + line items
├── inventory/                              ← DOMAIN: Stock management
├── analytics/                              ← DOMAIN: Aggregated analytics
├── importexport/                           ← DOMAIN: CSV/Excel import & PDF/Excel export
├── ai/                                     ← DOMAIN: AI assistant + LLM providers
├── notification/                           ← DOMAIN: Alerts and notifications
└── admin/                                  ← DOMAIN: Administration + audit logs
```

### 4.3 Layer Responsibilities

| Layer | Responsibility | What It Must NOT Do |
|-------|---------------|-------------------|
| **Controller** | Accept HTTP request, validate with Bean Validation, call Service, return response DTO | Business logic, direct repository access, return Entity |
| **Service** | Implement all business rules, orchestrate repository calls, call other module services via their service interface | Direct HTTP calls, return Entity to controller |
| **Repository** | Data access via Spring Data JPA — queries, CRUD, custom JPQL/native queries | Business logic, data transformation |
| **Domain (Entity)** | JPA entity definition — fields, relationships, lifecycle hooks | Business logic, validation logic |
| **DTO** | Input/output data shapes — Request DTOs (validated), Response DTOs (mapped) | Business logic |
| **Mapper** | MapStruct-based Entity ↔ DTO conversion | Business logic |

### 4.4 Standard Response Envelope

Every API endpoint returns the `ApiResponse<T>` wrapper:

```json
{
  "success": true,
  "data": { ... },
  "message": "Products retrieved successfully",
  "timestamp": "2026-07-06T10:00:00Z"
}
```

Error responses:

```json
{
  "success": false,
  "error": {
    "code": "PRODUCT_NOT_FOUND",
    "message": "Product with ID '123' was not found",
    "details": null
  },
  "timestamp": "2026-07-06T10:00:00Z"
}
```

---

## 5. Frontend Architecture

### 5.1 Feature-First Structure

```
frontend/src/
│
├── components/                     ← Shared, reusable UI components
│   ├── ui/                         ← Shadcn UI (auto-generated, do not edit)
│   ├── layout/
│   │   ├── AppShell.tsx            ← Main layout: sidebar + header + content
│   │   ├── Sidebar.tsx             ← Navigation sidebar
│   │   ├── Header.tsx              ← Top header bar
│   │   └── PageHeader.tsx          ← Page-level header
│   └── common/
│       ├── DataTable.tsx           ← Reusable paginated table
│       ├── StatCard.tsx            ← KPI metric card
│       ├── LoadingSpinner.tsx
│       ├── ErrorBoundary.tsx
│       └── ConfirmDialog.tsx
│
├── features/                       ← Domain feature modules
│   ├── auth/
│   │   ├── components/             ← LoginForm, RegisterForm
│   │   ├── hooks/                  ← useLogin, useRegister, useLogout
│   │   ├── services/               ← auth.service.ts (Axios calls)
│   │   ├── store/                  ← authStore (Zustand)
│   │   ├── types/                  ← AuthUser, LoginRequest, AuthResponse
│   │   └── index.ts                ← Barrel export
│   │
│   ├── dashboard/
│   ├── products/
│   ├── categories/
│   ├── customers/
│   ├── orders/
│   ├── inventory/
│   ├── analytics/
│   ├── import-export/
│   ├── ai-assistant/
│   └── admin/
│
├── hooks/                          ← Shared custom hooks
│   ├── useDebounce.ts
│   ├── usePagination.ts
│   └── useLocalStorage.ts
│
├── lib/
│   ├── api.ts                      ← Axios instance (base URL, interceptors)
│   ├── queryClient.ts              ← TanStack Query client configuration
│   └── utils.ts                   ← cn(), formatCurrency(), formatDate()
│
├── providers/
│   ├── QueryProvider.tsx           ← TanStack Query wrapper
│   ├── AuthProvider.tsx            ← Auth context + token refresh logic
│   └── ThemeProvider.tsx           ← Dark/light mode
│
├── router/
│   └── index.tsx                   ← React Router v6 route definitions
│
├── pages/                          ← Route-level page components (thin wrappers)
│   ├── DashboardPage.tsx
│   ├── ProductsPage.tsx
│   └── ...
│
├── store/
│   └── uiStore.ts                  ← Global UI state (sidebar open, theme)
│
└── types/
    └── index.ts                    ← Shared TypeScript types
```

### 5.2 Data Fetching Pattern

All server state is managed by **TanStack Query**:

```typescript
// Pattern (not code)
// services/product.service.ts — pure Axios calls, no state
// hooks/useProducts.ts — TanStack Query wrapper

useQuery({
  queryKey: ['products', { page, search, categoryId }],
  queryFn: () => productService.getProducts({ page, search, categoryId }),
  staleTime: 30_000
})

useMutation({
  mutationFn: productService.createProduct,
  onSuccess: () => queryClient.invalidateQueries({ queryKey: ['products'] })
})
```

### 5.3 Form Pattern

All forms use **React Hook Form** + **Zod**:

```typescript
// Pattern (not code)
const schema = z.object({
  name: z.string().min(1).max(255),
  price: z.number().positive(),
  categoryId: z.string().uuid()
})
const form = useForm({ resolver: zodResolver(schema) })
```

---

## 6. MCP Architecture

See `07_MCP_DESIGN.md` for full MCP architecture specification.

**Summary:**
- Standalone Node.js service
- Communicates ONLY with Spring Boot REST API via HTTP
- Never accesses database directly
- Tools organized by domain (revenue, customer, inventory, analytics, order, product)
- Supports stdio and SSE transport modes

---

## 7. Module Responsibilities

| Module | Backend Package | Frontend Feature | Responsibility |
|--------|----------------|-----------------|----------------|
| Auth | `com.commerceinsight.auth` | `features/auth` | JWT login, register, refresh, logout |
| User | `com.commerceinsight.user` | `features/admin` | User CRUD, role management |
| Product | `com.commerceinsight.product` | `features/products` | Product catalog |
| Category | `com.commerceinsight.category` | `features/categories` | Category tree |
| Customer | `com.commerceinsight.customer` | `features/customers` | Customer profiles |
| Order | `com.commerceinsight.order` | `features/orders` | Order lifecycle |
| Inventory | `com.commerceinsight.inventory` | `features/inventory` | Stock tracking |
| Analytics | `com.commerceinsight.analytics` | `features/analytics` | Aggregated metrics |
| Import/Export | `com.commerceinsight.importexport` | `features/import-export` | File processing |
| AI | `com.commerceinsight.ai` | `features/ai-assistant` | LLM integration |
| Admin | `com.commerceinsight.admin` | `features/admin` | User mgmt, audit logs |
| Shared | `com.commerceinsight.shared` | `lib/`, `types/` | Common utilities |
| Security | `com.commerceinsight.security` | `providers/AuthProvider` | JWT filter chain |

---

## 8. Folder Structure

Refer to `05_BACKEND.md` for the complete backend implementation folder structure.

Refer to `06_FRONTEND.md` for the complete frontend implementation folder structure.

The project root structure:

```
commerce-insight-ai/
├── backend/               Spring Boot Modular Monolith
├── frontend/              React 19 SPA
├── mcp-server/            Node.js MCP Server
├── docker/                Docker support files
├── scripts/               Dev utility scripts
├── docs/                  All design documentation
└── .github/               CI/CD workflows
```

---

## 9. Sequence Diagrams

### 9.1 Product Creation

```
Browser         Spring Boot API        Product Module        Database
   │                   │                    │                    │
   │  POST /api/v1/products                 │                    │
   │  { name, sku, price, ... }             │                    │
   │ ─────────────────►│                    │                    │
   │                   │  JwtFilter validates token              │
   │                   │  @PreAuthorize("MANAGER or ADMIN")       │
   │                   │  ProductController.createProduct()      │
   │                   │ ──────────────────►│                    │
   │                   │                   │  @Valid on DTO      │
   │                   │                   │  ProductService.create()
   │                   │                   │  Check SKU unique  │
   │                   │                   │ ──────────────────►│
   │                   │                   │  ◄── SKU check ───│
   │                   │                   │  ProductMapper → Entity
   │                   │                   │  productRepo.save()│
   │                   │                   │ ──────────────────►│
   │                   │                   │  ◄─ Saved entity ─│
   │                   │                   │  ProductMapper → DTO
   │                   │                   │  ApiResponse.success(dto)
   │                   │ ◄──────────────── │                    │
   │  201 Created                          │                    │
   │  { success, data: ProductResponse }   │                    │
   │ ◄─────────────────│                   │                    │
```

### 9.2 AI Insight Query

```
Browser      Spring Boot       MCP Server       LLM Provider        DB
   │              │                 │                │               │
   │  POST /api/v1/ai/chat          │                │               │
   │  { message: "What were my      │                │               │
   │    top products this month?" }  │                │               │
   │ ────────────►│                 │                │               │
   │              │  AI Service     │                │               │
   │              │  Build system prompt + tools list│               │
   │              │ ────────────────────────────────►│               │
   │              │                 │                │  LLM decides  │
   │              │                 │  Tool call:    │  to call tool │
   │              │                 │  get_top_products({period:'month', limit:5})
   │              │                 │ ◄──────────────│               │
   │              │                 │  GET /analytics/products/top   │
   │              │ ◄───────────────│                │               │
   │              │  GET /api/v1/analytics/products/top              │
   │              │ ─────────────────────────────────────────────────►│
   │              │ ◄────────────────── top 5 products ──────────────│
   │              │ ───────────────►│                │               │
   │              │                 │  Tool result → LLM             │
   │              │                 │ ──────────────►│               │
   │              │                 │                │  Generate     │
   │              │                 │                │  natural lang │
   │              │                 │                │  response     │
   │              │                 │ ◄──────────────│               │
   │              │ ◄───────────────│                │               │
   │  200 OK                        │                │               │
   │  { reply: "Your top 5 products │                │               │
   │    this month were: ..." }      │                │               │
   │ ◄────────────│                 │                │               │
```

---

## 10. Deployment Architecture

### 10.1 Docker Compose (Development)

```
docker-compose.yml
  services:
    postgres     → Port 5432 (data persistence volume)
    backend      → Port 8080 (depends_on: postgres)
    frontend     → Port 5173 (nginx serving React build)
    mcp-server   → Port 3001 (depends_on: backend)
    pgadmin      → Port 5050 (dev-tools profile only)

  networks:
    cia-network (bridge)
  volumes:
    postgres_data
```

### 10.2 Production Deployment (Future)

```
                    ┌───────────────────┐
Internet ─────────► │   Nginx / CDN     │
                    │   (TLS Termination│
                    └─────────┬─────────┘
                              │
                    ┌─────────▼─────────┐
                    │   Docker Host     │
                    │                   │
                    │  ┌─────────────┐  │
                    │  │  Frontend   │  │  (React build, served by nginx)
                    │  └─────────────┘  │
                    │  ┌─────────────┐  │
                    │  │  Backend    │  │  (Spring Boot JAR)
                    │  └─────────────┘  │
                    │  ┌─────────────┐  │
                    │  │  MCP Server │  │  (Node.js process)
                    │  └─────────────┘  │
                    │  ┌─────────────┐  │
                    │  │  PostgreSQL │  │  (with backup volumes)
                    │  └─────────────┘  │
                    └───────────────────┘
```

---

## 11. Communication Flow

### 11.1 Frontend → Backend

- **Protocol**: HTTPS REST
- **Auth**: JWT Bearer Token in `Authorization` header
- **Request format**: JSON body with `Content-Type: application/json`
- **Response format**: Always `ApiResponse<T>` envelope
- **Error handling**: Frontend catches non-2xx responses, extracts `error.message`

### 11.2 MCP Server → Backend

- **Protocol**: HTTP REST (internal Docker network; HTTPS in production)
- **Auth**: `X-MCP-API-KEY: {key}` header
- **Scope**: Read-only endpoints + export trigger
- **Data contract**: Same `ApiResponse<T>` envelope

### 11.3 Backend → LLM Providers

- **Protocol**: HTTPS REST (provider-specific SDKs or raw Axios)
- **Auth**: Provider API key in `Authorization: Bearer {apiKey}` or `x-api-key`
- **Format**: Provider-specific (OpenAI Chat Completions, Anthropic Messages, etc.)

---

## 12. Dependency Rules

### 12.1 Backend Inter-Layer Rules

```
Allowed call directions:
Controller → Service ✓
Service → Repository ✓
Service → Mapper ✓
Service → Service (same module) ✓
Service → Service (different module) ✓ (via interface only)
Repository → Domain Entity ✓

Forbidden:
Controller → Repository ✗
Controller → Domain Entity (return) ✗
Repository → Service ✗
Service → Controller ✗
Any layer → Another module's Repository ✗
```

### 12.2 Frontend Module Rules

```
features/{A}/components → components/ui ✓
features/{A}/components → components/common ✓
features/{A}/hooks → features/{A}/services ✓
features/{A}/hooks → lib/api ✓
features/{A}/hooks → features/{B}/* ✗ (use shared types only)
pages/* → features/{A}/index.ts ✓
```

### 12.3 MCP Rules

```
MCP Tool → API Client ✓
MCP Tool → Database ✗ (absolutely forbidden)
MCP Tool → Another Tool (direct) ✗ (use shared client)
API Client → Spring Boot REST API ✓
```
