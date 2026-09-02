# Project Overview — Commerce Insight AI

An interview-oriented tour. For the "what/how to run it" summary see the
[root README](../../README.md); for structural detail see
[`docs/architecture/ARCHITECTURE.md`](../architecture/ARCHITECTURE.md).

---

## 1. Problem

An ecommerce back office needs two things that usually get built as separate
systems: **operational CRUD** (products, customers, orders, inventory) and
**analytics** (revenue trends, order mix, top products). Bolting a dashboard onto
an operational database after the fact tends to produce slow ad-hoc queries, a
second copy of the domain model, and a UI that drifts from the API.

A newer requirement on top: expose the same data to **AI agents** and let a
dashboard ask an **LLM** to explain the numbers — without that AI layer becoming
a reliability or privacy liability.

## 2. Solution

One Spring Boot **modular monolith** owns the domain, the persistence, the
analytics aggregation and the AI orchestration. A React SPA and a Node **MCP
server** are both thin clients of the same REST API. The **LLM** sits behind a
provider abstraction and is entirely optional.

```
React SPA  ─┐
            ├─► Spring Boot (modular monolith) ─► PostgreSQL 16 (+ Flyway)
MCP server ─┘        │
                     └─(optional)─► LLM provider (OpenAI-compatible / Anthropic / Ollama)
```

- **509 backend tests**, **48 frontend**, **59 MCP**, **25 + 9 Playwright E2E**,
  **75.8 %** line coverage, static secret scan clean.
- Runs with **one command** (`./scripts/demo-up.sh`) against a **deterministic
  ~600-order dataset**.

## 3. Architecture in brief

- **Modular monolith** — one package per domain (`auth`, `product`, `category`,
  `customer`, `order`, `inventory`, `analytics`, `dataimport`, `export`, …), each
  with the same `controller / service / repository / domain / dto / mapper`
  layers. Enforced rules: no cross-module repository access, entities never leave
  the service layer, every response is an `ApiResponse<T>` envelope.
- **Frontend** — feature-first React 19; server state in TanStack Query, client
  state in Zustand, forms in react-hook-form + Zod; route-level code splitting.
- **MCP server** — Model Context Protocol adapter, 8 tool providers, REST-only,
  no DB driver, no LLM SDK.
- **AI layer** — `AiAnalyticsService` → `LlmClient` → `LlmProvider`
  (`OpenAiCompatibleLlmProvider` / `AnthropicLlmProvider`); aggregates-only
  context; strict-JSON output parsed into a DTO; every failure path returns
  `200 { available:false }`.
- **Database** — Flyway `V1`–`V31`, `ddl-auto=validate`, UUID PKs, soft delete,
  audited timestamps.

## 4. Key engineering challenges

| Challenge | How it was handled |
|---|---|
| **Analytics without a second domain model** | `AnalyticsRepository` runs native aggregate SQL directly against the operational tables, returning projection DTOs. No ETL, no read model. Composite indexes (`orders(created_at,status,total)`, `payments(method,status)`, …) are the production access paths. |
| **PostgreSQL NULL-bind on optional date filters** | `(:dateFrom IS NULL OR created_at >= :dateFrom)` fails to prepare — Postgres can't type `$1 IS NULL`. Fixed with `CAST(:dateFrom AS timestamptz)` on every nullable bound parameter (Sprint 13D), semantics-neutral, one file. |
| **AI must never break the dashboard** | A single `AiAnalyticsService` funnel: disabled / no key / connection refused / timeout / HTTP error / rate-limit / unparseable output all converge on `AiInsightsResponse{ available:false }` at `HTTP 200`. The frontend card renders a degraded state; no other panel is touched. Covered by unit tests for each failure reason and an E2E case. |
| **Not leaking PII or secrets into a prompt** | `AiAnalyticsContextBuilder` emits aggregates only — totals, counts, month buckets, top-N names. Tests assert no email/phone/address/UUID/key appears in the serialised context or prompt. Provider keys are resolved backend-side only and never logged. |
| **Prompt injection via catalogue text** | Product / customer / category names are user-controlled and end up in the context. The prompt fences the context as untrusted data and the system prompt forbids obeying instructions found inside it; tested with malicious values. |
| **Refresh-token security without a session store** | Opaque 122-bit tokens, stored hashed, rotated on every use, with family reuse-detection that invalidates the whole family and writes an audit event. Stateless access tokens stay short (15 min) and are re-checked against `enabled`/`locked` on every request. |
| **Consistent errors across ~14 controllers** | One `GlobalExceptionHandler` maps every exception to `ApiResponse.error(ErrorResponse.of(code, message))`. Unknown paths return `404`, not a catch-all `500`. No stack trace, SQL, framework class, path, key or JWT in any error body (grep-verified). |
| **Deterministic demo data** | `R__seed_demo_data.sql`: every PK is `md5('demo-<domain>-<n>')::uuid`, every "random" choice is a hash modulo, every insert is `ON CONFLICT DO NOTHING`. Same 600 orders every run; safe to re-run; impossible to reach `prod` (separate Flyway location + database + volume). |
| **E2E that isn't flaky** | Two stacks: a thin fixed-user `e2e` seed for the security suite (deterministic, in CI) and the large `demo` seed for the dashboard-KPI suite. No random UUIDs, no wall-clock assertions, no real LLM, route interception for provider states. |
| **`/actuator/metrics` returning 500** | The `demo`/`prod` profiles hadn't exposed `metrics`, so the request hit no handler and the catch-all mapped it to 500. Fixed by exposing `metrics` and gating everything past `health`/`info` to `ROLE_ADMIN`, plus a `NoHandlerFoundException → 404` handler (Sprint 15). |

## 5. Interesting technical decisions

- **Modular monolith over microservices.** The domain is one transactional unit
  (an order write touches customer, product, inventory, payment). One DB + one
  deploy removes distributed-transaction and discovery cost the problem doesn't
  justify. Module boundaries stay explicit so later extraction is mechanical.
  ([ADR-001](../adr/001-modular-monolith.md))
- **MCP talks REST, not SQL.** The MCP server could be faster with a direct DB
  connection; instead it's a pure REST client so it inherits authn/authz, rate
  limiting, validation and the response envelope for free, and can never become a
  second, unguarded data path. ([ADR-002](../adr/002-mcp-server.md))
- **AI isolated behind `LlmClient`.** Controllers and the MCP server have no
  provider SDK. Swapping OpenAI ↔ Anthropic ↔ Ollama is a config change
  (`AI_PROVIDER`, `AI_BASE_URL`, `AI_MODEL`). The AI feature can be deleted by
  flipping one flag with zero effect on the rest of the system.
- **Structured AI output, not free text.** The model is asked for strict JSON;
  `AiAnalyticsResponseParser` validates it into `AiInsightsResponse`. Malformed
  output is a handled `available:false`, not a rendering hazard.
- **Aggregates-only AI context.** A deliberate privacy ceiling: the LLM sees
  numbers and top-N names, never rows.
- **In-memory rate limiting (bucket4j, no Redis).** Correct for a single
  instance; a documented trade-off rather than premature infrastructure.
- **Native SQL for analytics, JPA for everything else.** Use the right tool per
  workload instead of forcing aggregates through the ORM.
- **JaCoCo report-only, no coverage gate.** Coverage is visible and tracked
  (75.8 % / 81.5 % excl. generated mappers) but not a build blocker — a
  consolidation-phase choice.
- **Report-only, non-blocking dependency audit on a schedule.** OWASP + `npm
  audit` weekly, out of the PR path, findings tracked as accepted risks.
- **Deterministic seed as a first-class artifact.** The demo dataset is a
  repeatable Flyway migration with the same isolation guarantees the project
  already trusts for its E2E seed.

## 6. Security posture

- Stateless JWT (HS256, 15 min) + rotating hashed refresh tokens with reuse
  detection; `logout` revokes.
- `ADMIN > MANAGER > STAFF` role hierarchy, method-level `@PreAuthorize`;
  `401` / `403` / `429` all returned as the standard envelope.
- Per-route rate limiting (login, register, refresh, import, export, AI).
- MCP boundary: shared `X-MCP-API-KEY`, constant-time compare, fail-closed.
- Headers: HSTS, `CSP default-src 'none'`, `X-Frame-Options: DENY`, `nosniff`,
  `Referrer-Policy`, `Permissions-Policy`, `Cache-Control: no-store`,
  `X-Request-Id` on every response.
- No `dangerouslySetInnerHTML` / `innerHTML` / `eval`; an explicit test proves AI
  text renders as inert text.
- `SecretsValidator` blocks a `prod` boot with a dev/demo secret; Swagger
  disabled in `prod`; `/actuator` sensitive endpoints unexposed.
- `node scripts/security-check.mjs` → 0 errors / 0 warnings, enforced in CI.

Full verification: [`docs/security/SPRINT_15_SECURITY.md`](../security/SPRINT_15_SECURITY.md).

## 7. AI integration

- Optional, **off by default** (`AI_INSIGHTS_ENABLED=false`).
- `LlmClient` + `LlmProvider` abstraction: OpenAI-compatible (covers OpenAI, any
  compatible gateway, and local Ollama) and Anthropic Messages API.
- Aggregates-only context; prompt-injection fencing; strict-JSON parsing.
- Every failure → `200 { available:false }`; provider bodies never logged.
- `AiMetrics` (Micrometer): request / success / unavailable / validation-failure
  / provider-failure counters + a latency timer, low-cardinality tags only, at
  `/actuator/metrics/ai.insights.*` (ADMIN).
- CI never needs a key; `RealProviderManualTest` is the only real-provider test
  and is opt-in.

## 8. MCP integration

- 8 tool providers mirroring the domain API (products, categories, customers,
  orders, inventory, import, export, analytics + `ai`).
- Pure REST client — no `pg`, no ORM, no LLM SDK (verified by grep +
  `security-check.mjs`).
- Authenticates with `X-MCP-API-KEY` → synthetic `ROLE_MCP_SERVICE`; subject to
  the same Spring authorization as any client.
- `analytics_ai_insights` returns only the safe response subset.

## 9. Testing

| Layer | Framework | Count | Where |
|---|---|---:|---|
| Backend unit + integration | JUnit 5, Mockito, Spring Boot Test + real Postgres | 509 (0 fail, 1 opt-in skip) | `backend/src/test/` |
| Frontend unit | Vitest + Testing Library | 48 | `frontend/src/**/*.test.tsx` |
| MCP | `node --test` | 59 | `mcp-server/src/**/*.test.ts` |
| E2E — security | Playwright vs `e2e` stack | 25 | `frontend/e2e/` |
| E2E — dashboard / AI | Playwright vs `demo` stack | 9 | `frontend/e2e/dashboard.spec.ts` |
| Static security scan | `security-check.mjs` | — | CI gate, 0/0 |
| Coverage | JaCoCo 0.8.12 | 75.8 % line (81.5 % excl. generated mappers) | report only |

The E2E total is **not** "34/34" — the two suites run against different datasets
and are reported separately.

Detail: [`docs/testing/TESTING_MATRIX.md`](../testing/TESTING_MATRIX.md).

## 10. Performance

> Local demo measurements (Docker Desktop / WSL2, ~600-order deterministic
> dataset). Indicative, not universal benchmarks.

- Analytics endpoints: **7–27 ms** across 7/30/90/365-day windows, all `200`.
- `EXPLAIN ANALYZE`: at 600 orders the planner correctly sequential-scans;
  the schema's composite indexes are the access paths a larger dataset would use.
  No index was added because the plan on the demo data didn't justify one.
- Export: orders XLSX ~**1055 ms**, orders PDF ~**452 ms**; products/customers
  31–89 ms.
- Import: job creation 23–71 ms (async). Bulk row-throughput **not benchmarked** —
  a known limitation; import correctness is covered by the service test suite.
- Frontend bundle: largest chunk `charts-vendor` ~421 kB (gzip ~114 kB); no
  chunk-size warning; all heavy routes lazy-loaded.

Detail: [`docs/performance/SPRINT_15_PERFORMANCE.md`](../performance/SPRINT_15_PERFORMANCE.md).

## 11. What I would improve next

- **Measure import throughput end-to-end** with valid fixtures matching the
  template contract; currently only job-creation latency is measured.
- **A real deploy target.** `cd.yml` builds and pushes images; wire the guarded
  `deploy` job to an actual host (or a small managed platform) and add a
  post-deploy health gate.
- **Coverage where it's thin without churn** — `OrderService` /
  `InventoryService` happy-path reservation logic sits behind integration tests
  only; a few targeted unit tests would raise confidence in the money paths.
- **Clear the last frontend lint warnings** by splitting the shadcn/provider
  component+hook colocations into separate files — low value, deferred to avoid
  an unrelated refactor.
- **Shared-store rate limiting** if the backend ever runs more than one instance.
- **Screenshot/GIF automation in CI** so `docs/screenshots/` regenerates on UI
  changes instead of being captured by hand.
- **Prompt/response evaluation harness** for the AI layer (golden aggregates →
  expected insight shape) to catch model-behaviour regressions when the model or
  provider changes.

---

## Appendix — project timeline

Delivered as `sprint*` branches merged to `main` via PR:

| Sprint | Theme |
|---|---|
| 0–2 | Project init, solution design, foundation |
| 3 | MCP foundation |
| 4 | Authentication + RBAC |
| 5–6 | Product & category, inventory |
| 7–8 | Customer domain + segmentation, order lifecycle |
| 9 | Analytics backend, dashboard UI, MCP analytics tools |
| 10–11 | CSV/Excel import, PDF/Excel export |
| 12 | Security hardening (backend + frontend + E2E verification) |
| 13 | Production-readiness CI + Docker + integration + E2E; demo data; analytics NULL-bind fix |
| 14 | AI-assisted analytics — LLM provider layer, `ai-insights` endpoint, dashboard card, CD skeleton |
| 15 | Testing / security / performance consolidation — JaCoCo, actuator-metrics fix, XSS test |
| 16 | Portfolio / Docker / documentation polish (this document set) |

> `Roadmap.md` in the repo root is an informal working list and is intentionally
> left untracked; this table is the maintained version.
