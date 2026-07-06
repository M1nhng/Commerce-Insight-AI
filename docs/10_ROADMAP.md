# 10 — Project Roadmap
# Commerce Insight AI

> **Document Type**: Sprint Plan & Roadmap
> **Version**: 1.0.0
> **Status**: Approved
> **Last Updated**: 2026-07-06
> **Owner**: Chief Solution Architect

---

## Table of Contents

1. [Purpose](#1-purpose)
2. [Definition of Done](#2-definition-of-done)
3. [Sprint Overview](#3-sprint-overview)
4. [Sprint Details](#4-sprint-details)
5. [Milestone Map](#5-milestone-map)
6. [Dependencies](#6-dependencies)
7. [Risk Analysis](#7-risk-analysis)
8. [Project Timeline](#8-project-timeline)

---

## 1. Purpose

This document defines the complete sprint plan, milestones, and delivery timeline for the Commerce Insight AI project. It translates the PRD requirements into executable sprints with clear deliverables, dependencies, and success criteria.

---

## 2. Definition of Done

A task or feature is considered **Done** when ALL of the following are true:

### Code Quality
- [ ] Code follows all architecture rules defined in `03_ARCHITECTURE.md`
- [ ] No entity is returned from any API endpoint (DTO only)
- [ ] No business logic exists in any controller
- [ ] All inputs validated via Bean Validation (backend) or Zod (frontend)
- [ ] MapStruct mappers used for all entity ↔ DTO conversions

### Testing
- [ ] Unit tests written for all service methods
- [ ] Integration tests written for all controller endpoints
- [ ] All tests pass in CI
- [ ] Code coverage ≥ 80% for the module

### Documentation
- [ ] OpenAPI annotations added to all new endpoints
- [ ] README updated if new setup steps are required

### Security
- [ ] Endpoint protected with correct `@PreAuthorize` annotation
- [ ] No sensitive data logged or exposed in error responses

### CI/CD
- [ ] All CI pipeline checks pass (build, test, lint, type-check)
- [ ] No merge to `main` with failing checks

---

## 3. Sprint Overview

| Sprint | Name | Duration | Key Deliverable |
|--------|------|----------|----------------|
| **0** | Architecture & Scaffolding | 1 week | Complete project structure, all docs |
| **1** | Design Phase | 1 week | All 10 design documents (this sprint) |
| **2** | Backend Foundation | 2 weeks | Shared module, Auth, Users, Security |
| **3** | Core Business Modules | 3 weeks | Products, Categories, Customers, Orders |
| **4** | Operations & Analytics | 2 weeks | Inventory, Analytics, Dashboard APIs |
| **5** | Import/Export | 2 weeks | CSV/Excel import, PDF/Excel export |
| **6** | AI & MCP | 2 weeks | MCP server tools + AI chat endpoint |
| **7** | Frontend — Foundation | 2 weeks | React app shell, auth, routing, API layer |
| **8** | Frontend — Core Modules | 3 weeks | Products, Orders, Customers, Categories UI |
| **9** | Frontend — Intelligence | 2 weeks | Analytics charts, AI assistant UI |
| **10** | Frontend — Data Operations | 1 week | Import/Export UI, Admin panel |
| **11** | Testing & QA | 2 weeks | Integration tests, E2E, performance |
| **12** | Polish & Documentation | 1 week | README, docs, demo data, final review |

**Total Estimated Duration**: ~22 weeks

---

## 4. Sprint Details

### Sprint 0 — Architecture & Scaffolding ✓ COMPLETED

**Goal**: Establish the complete project skeleton.

| Task | Status |
|------|--------|
| Monorepo structure (`/backend`, `/frontend`, `/mcp-server`) | ✓ Done |
| `pom.xml` with all dependencies | ✓ Done |
| `frontend/package.json` with all dependencies | ✓ Done |
| `mcp-server/package.json` with MCP SDK | ✓ Done |
| `docker-compose.yml` placeholder | ✓ Done |
| `.gitignore` | ✓ Done |
| `root README.md` | ✓ Done |
| Module READMEs | ✓ Done |
| GitHub Actions CI pipeline | ✓ Done |
| ADR-001, ADR-002 | ✓ Done |

---

### Sprint 1 — Design Phase ✓ IN PROGRESS

**Goal**: Complete all software design documentation before any code is written.

| Task | Document | Status |
|------|----------|--------|
| Project Vision | `01_PROJECT_VISION.md` | ✓ Done |
| PRD | `02_PRD.md` | ✓ Done |
| Architecture | `03_ARCHITECTURE.md` | ✓ Done |
| Database Design | `04_DATABASE.md` | ✓ Done |
| API Specification | `05_API_SPECIFICATION.md` | ✓ Done |
| Authentication Design | `06_AUTHENTICATION.md` | ✓ Done |
| MCP Design | `07_MCP_DESIGN.md` | ✓ Done |
| AI Design | `08_AI_DESIGN.md` | ✓ Done |
| UI/UX Design | `09_UI_UX.md` | ✓ Done |
| Roadmap | `10_ROADMAP.md` | ✓ Done |

---

### Sprint 2 — Backend Foundation

**Goal**: Working authentication API with JWT, RBAC, and shared infrastructure.

**Backend Tasks:**

| Task | Module | Priority |
|------|--------|----------|
| `BaseEntity` with id, createdAt, updatedAt, deletedAt | shared | P0 |
| `ApiResponse<T>` and `PageResponse<T>` | shared | P0 |
| `GlobalExceptionHandler` | shared/exception | P0 |
| `ErrorCode` enum | shared | P0 |
| Spring Security config (stateless, JWT filter chain) | security | P0 |
| `JwtTokenUtil` (create, parse, validate) | security | P0 |
| `JwtAuthenticationFilter` | security | P0 |
| `User` entity + `Role` enum | user/domain | P0 |
| `UserRepository` | user/repository | P0 |
| `UserDetailsServiceImpl` | security | P0 |
| `RefreshToken` entity + repository | auth/domain | P0 |
| `AuthService` (login, register, refresh, logout) | auth/service | P0 |
| `AuthController` (4 endpoints) | auth/controller | P0 |
| `UserService` (CRUD) | user/service | P1 |
| `UserController` (CRUD) | user/controller | P1 |
| Flyway V1-V2 migrations (extensions, users, refresh_tokens) | resources | P0 |
| Flyway V11 seed (default admin user) | resources | P1 |
| OpenAPI config + Swagger UI | config | P1 |
| CORS config | config | P0 |
| Unit tests for AuthService | test | P0 |
| Integration tests for AuthController | test | P0 |

**Deliverable**: `POST /auth/login`, `POST /auth/register`, `POST /auth/refresh`, `POST /auth/logout`, user CRUD — all secured with JWT + RBAC.

---

### Sprint 3 — Core Business Modules

**Goal**: Full CRUD for Products, Categories, Customers, Orders.

| Task | Module | Priority |
|------|--------|----------|
| Category entity, repository, service, controller | category | P0 |
| Product entity, repository, service, controller | product | P0 |
| Customer entity, repository, service, controller | customer | P0 |
| Order + OrderItem entity, repository, service | order | P0 |
| Order controller + status transition logic | order | P0 |
| Order number generation (ORD-YYYY-{seq}) | order/service | P0 |
| SKU uniqueness validation | product/service | P0 |
| MapStruct mappers for all 4 modules | all | P0 |
| Flyway V3-V6 migrations | resources | P0 |
| Unit tests + integration tests per module | test | P0 |

**Deliverable**: Full CRUD API for all core business entities.

---

### Sprint 4 — Operations & Analytics

**Goal**: Inventory tracking and analytics aggregation APIs.

| Task | Module | Priority |
|------|--------|----------|
| Inventory entity + repository | inventory | P0 |
| Inventory service (track, adjust, decrease on order) | inventory | P0 |
| InventoryMovement logging | inventory | P0 |
| Inventory controller | inventory | P0 |
| Analytics service — revenue summary, trend | analytics | P0 |
| Analytics service — top products | analytics | P0 |
| Analytics service — order summary | analytics | P0 |
| Analytics controller | analytics | P0 |
| Dashboard endpoint (aggregated KPIs) | analytics | P0 |
| Flyway V7-V9 migrations | resources | P0 |
| Performance indexes | resources | P1 |

**Deliverable**: Inventory management + all analytics APIs ready for frontend and MCP.

---

### Sprint 5 — Import / Export

**Goal**: CSV/Excel import and PDF/Excel export fully operational.

| Task | Module | Priority |
|------|--------|----------|
| CSV parser with validation (Apache Commons CSV) | importexport | P0 |
| Excel parser with validation (Apache POI) | importexport | P0 |
| Product import service (atomic) | importexport | P0 |
| Customer import service (atomic) | importexport | P0 |
| Order import service (atomic) | importexport | P0 |
| Import controller (3 endpoints + template download) | importexport | P0 |
| Excel export (JasperReports or Apache POI) | importexport | P0 |
| PDF export (iText or JasperReports) | importexport | P0 |
| Export controller | importexport | P0 |

**Deliverable**: Complete import/export functionality for all supported formats.

---

### Sprint 6 — AI & MCP

**Goal**: AI chat functional with real data via MCP tools.

| Task | Module | Priority |
|------|--------|----------|
| `LLMProvider` interface | ai/provider | P0 |
| `OpenAIProvider` implementation | ai/provider | P0 |
| `ClaudeProvider` implementation | ai/provider | P1 |
| `GeminiProvider` implementation | ai/provider | P1 |
| `OllamaProvider` implementation | ai/provider | P2 |
| `LLMProviderFactory` | ai/provider | P0 |
| `ConversationSession` + `ConversationMessage` entities | ai/domain | P0 |
| `ConversationService` | ai/service | P0 |
| `AiService` (orchestration, tool invocation) | ai/service | P0 |
| `MCPClientService` (HTTP calls to MCP server) | ai/service | P0 |
| `AiController` | ai/controller | P0 |
| MCP Server: API client + config | mcp-server | P0 |
| MCP Server: Revenue tools | mcp-server | P0 |
| MCP Server: Analytics tools | mcp-server | P0 |
| MCP Server: Product tools | mcp-server | P0 |
| MCP Server: Order tools | mcp-server | P0 |
| MCP Server: Customer tools | mcp-server | P0 |
| MCP Server: Inventory tools | mcp-server | P0 |
| MCP Server: Export tools | mcp-server | P1 |
| Flyway V8 migration (conversation tables) | resources | P0 |

**Deliverable**: AI chat endpoint operational; all MCP tools callable with live data.

---

### Sprint 7 — Frontend Foundation

**Goal**: React app with auth, routing, layout, and API layer.

| Task | Layer | Priority |
|------|-------|----------|
| Axios instance with JWT interceptor + refresh logic | lib/api | P0 |
| TanStack Query client setup | lib/queryClient | P0 |
| React Router route definitions | router | P0 |
| AuthProvider + token storage | providers | P0 |
| ThemeProvider (dark/light) | providers | P0 |
| App shell layout (sidebar, header) | components/layout | P0 |
| Sidebar with navigation items | components/layout | P0 |
| Login page | features/auth | P0 |
| Protected route wrapper | router | P0 |
| Auth service + hooks | features/auth | P0 |
| Zustand auth store | features/auth/store | P0 |
| Common components: StatCard, DataTable, PageHeader | components/common | P0 |
| Shadcn UI initialization | components/ui | P0 |
| CSS custom properties (design tokens from §09_UI_UX) | index.css | P0 |

**Deliverable**: Working frontend with login, persistent auth, and app shell.

---

### Sprint 8 — Frontend Core Modules

**Goal**: Products, Orders, Customers, Categories fully functional in UI.

| Task | Feature | Priority |
|------|---------|----------|
| Product list page (table, search, filter) | products | P0 |
| Product create/edit form | products | P0 |
| Product detail panel | products | P0 |
| Category management | categories | P0 |
| Customer list + detail | customers | P0 |
| Order list (with status tabs) | orders | P0 |
| Order detail with line items | orders | P0 |
| Order status update flow | orders | P0 |
| Inventory management table | inventory | P0 |
| Stock adjustment form | inventory | P0 |

**Deliverable**: All CRUD modules fully functional in the UI.

---

### Sprint 9 — Frontend Intelligence

**Goal**: Analytics dashboards and AI assistant UI.

| Task | Feature | Priority |
|------|---------|----------|
| Dashboard page (4 KPI cards + charts) | dashboard | P0 |
| Revenue trend chart (Recharts AreaChart) | analytics | P0 |
| Top products chart (BarChart) | analytics | P0 |
| Analytics page (revenue, orders, customers tabs) | analytics | P0 |
| Period comparison UI | analytics | P0 |
| AI chat interface (messages, input) | ai-assistant | P0 |
| Session history sidebar | ai-assistant | P0 |
| Tool usage indicator | ai-assistant | P1 |

**Deliverable**: Full analytics dashboards + working AI chat interface.

---

### Sprint 10 — Frontend Data Operations

**Goal**: Import/Export UI + Admin panel.

| Task | Feature | Priority |
|------|---------|----------|
| Import wizard (upload → preview → import) | import-export | P0 |
| Import result display (errors table) | import-export | P0 |
| Export triggers (PDF/Excel buttons in list pages) | import-export | P0 |
| Admin user management table | admin | P0 |
| Admin audit log viewer | admin | P1 |
| Admin system settings (AI provider config) | admin | P1 |

**Deliverable**: Complete import/export UI + admin panel.

---

### Sprint 11 — Testing & QA

**Goal**: Achieve ≥ 80% backend test coverage; all critical flows tested.

| Task | Type | Priority |
|------|------|----------|
| Service unit tests for all modules | Unit | P0 |
| Controller integration tests (Testcontainers) | Integration | P0 |
| Auth flow integration tests | Integration | P0 |
| Import/Export integration tests | Integration | P0 |
| Security tests (role enforcement) | Integration | P0 |
| Performance baseline (JMeter/Gatling) | Performance | P1 |
| Manual QA of all UI flows | Manual | P0 |

---

### Sprint 12 — Polish & Documentation

**Goal**: Portfolio-ready: clean code, full docs, demo data, impressive README.

| Task | Priority |
|------|----------|
| Demo data seeder (realistic ecommerce data) | P0 |
| Root README with screenshots | P0 |
| Setup guide (`scripts/setup.sh` working) | P0 |
| API documentation review (Swagger) | P0 |
| Architecture diagrams (draw.io or Mermaid) | P0 |
| Code cleanup & lint pass | P0 |
| Docker Compose end-to-end test | P0 |
| Git history cleanup (rebase, squash noisy commits) | P1 |

---

## 5. Milestone Map

```
Sprint 0  ─── Project Skeleton
Sprint 1  ─── Design Complete ◄── YOU ARE HERE
Sprint 2  ─── Auth API Working
Sprint 3  ─── Core CRUD APIs ────────────────────► BACKEND MVP
Sprint 4  ─── Inventory + Analytics
Sprint 5  ─── Import + Export
Sprint 6  ─── AI + MCP ──────────────────────────► FULL BACKEND
Sprint 7  ─── Frontend Foundation
Sprint 8  ─── Core UI Modules
Sprint 9  ─── Analytics + AI UI ─────────────────► FRONTEND MVP
Sprint 10 ─── Data Operations UI
Sprint 11 ─── Testing
Sprint 12 ─── Polish ────────────────────────────► PORTFOLIO READY
```

---

## 6. Dependencies

### 6.1 Sprint Dependencies (Must complete before next can start)

```
Sprint 0 → Sprint 1 → Sprint 2 → Sprint 3 → Sprint 4 → Sprint 5 → Sprint 6
                                         ↘ Sprint 7 → Sprint 8 → Sprint 9 → Sprint 10

Sprint 6 is required before Sprint 9 (AI needs MCP backend)
Sprint 4 is required before Sprint 9 (analytics charts need analytics APIs)
Sprint 5 is required before Sprint 10 (import/export UI needs backend)
```

### 6.2 Technical Dependencies

| Dependency | Affects | Notes |
|------------|---------|-------|
| PostgreSQL running | All backend | Docker Compose |
| Backend running | Frontend API calls | Backend must be on :8080 |
| MCP Server running | AI chat | MCP must be on :3001 |
| LLM API key configured | AI chat | OpenAI / Anthropic key required |
| Flyway migrations applied | All DB operations | Applied on startup |

---

## 7. Risk Analysis

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| LLM API rate limits in development | Medium | Low | Use Ollama locally; mock responses in tests |
| MCP tool call latency too high | Low | Medium | Add caching layer for repeated queries |
| CSV import of large files (>10MB) | Low | Medium | Enforce file size limit; async processing (future) |
| Flyway migration conflict | Low | High | Never modify existing migrations; strict naming |
| CORS issues between services | Medium | Medium | Explicit CORS config; test in Docker environment |
| BCrypt hash time too slow (strength 12) | Low | Low | Benchmark on target hardware; reduce to 10 if needed |
| Complex order state machine bugs | Medium | High | Comprehensive unit tests for each transition |
| AI response time > 30 seconds | Medium | Medium | Implement timeout + streaming (future) |
| OpenAI API key compromised | Low | Critical | Use env vars only; rotate immediately on compromise |
| Database schema change after migration | Medium | High | Plan schema carefully; never modify applied migrations |

---

## 8. Project Timeline

### Estimated Schedule (Starting from Sprint 2)

```
Week  1-2:  Sprint 2 — Backend Foundation (Auth, Security, Users)
Week  3-5:  Sprint 3 — Core Business Modules (Products, Orders, etc.)
Week  6-7:  Sprint 4 — Operations & Analytics
Week  8-9:  Sprint 5 — Import/Export
Week 10-11: Sprint 6 — AI & MCP Integration
Week 12-13: Sprint 7 — Frontend Foundation
Week 14-16: Sprint 8 — Frontend Core Modules
Week 17-18: Sprint 9 — Frontend Intelligence (Charts + AI UI)
Week 19:    Sprint 10 — Frontend Data Operations
Week 20-21: Sprint 11 — Testing & QA
Week 22:    Sprint 12 — Polish & Documentation

Target completion: ~22 weeks from Sprint 2 start
```

### Review Gates

| Gate | After Sprint | Criteria |
|------|-------------|----------|
| Backend API Review | Sprint 6 | All APIs working, Swagger docs complete, 80% test coverage |
| Frontend Review | Sprint 10 | All pages functional, responsive, dark mode |
| QA Sign-off | Sprint 11 | All critical flows tested, no P0 bugs |
| Portfolio Ready | Sprint 12 | One-command setup, demo data, full README |
