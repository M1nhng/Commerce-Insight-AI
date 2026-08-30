# Sprint 13B — Production Readiness

_Branch: `sprint13` · Executed: 2026-08-30 · Docker + PostgreSQL available this session_

---

## 1. Executive Summary

Sprint 13B closes the last verification gap left open by Sprint 13A: the
**deterministic commerce E2E seed** needed to drive a real order stock-conflict
scenario end to end. With that seed in place the Playwright suite runs **25/25
green** against the live Docker stack — the former `test.fixme` (9.10 order
conflict) is now a real, passing test.

Every other Sprint 13B phase (real CI pipeline, base `docker-compose.yml` MCP env
fix, frontend Docker `VITE_API_BASE_URL` build arg, disposable-Postgres backend
integration, dependency-audit workflow, healthcheck robustness) was already
implemented and verified in Sprint 13A; this document **re-executes** those
verification layers and records the fresh results.

**Architecture changes: NONE.** No authentication rebuild, no token-contract
change, no new HTTP client / state library, no Redis, no OAuth, no MCP database
or LLM access. All Sprint 12A/12B/12C security controls remain in force and were
re-verified live.

| Layer | Result |
|---|---|
| Backend suite (disposable Postgres 16) | **421 run / 419 pass / 0 fail / 0 error / 2 skipped** (documented `@Disabled`) |
| Frontend `tsc` / Vitest / build / lint | PASS / **33 pass** / PASS / **0 errors** (17 advisory warnings) |
| MCP type-check / tests / build / runtime | PASS / **49 pass** / PASS / **healthy, 8 providers, stdio** |
| Playwright E2E (live 4-service stack) | **25 / 25 passed** (37.7 s) — 0 fail, 0 skip, 0 fixme |
| `scripts/security-check.mjs` | **0 errors / 0 warnings** |
| Live security spot-checks | HSTS · CSP · X-Frame · Referrer-Policy · Permissions-Policy · X-Request-Id · 401 envelope · restrictive CORS · MCP wrong-key → 401 |

---

## 2. Sprint Objective

Transform the project from _"security-hardened but locally verified only"_ into
_"reproducibly verifiable through CI and Docker-based integration/E2E
infrastructure"_ — specifically:

1. Reproducible E2E stack. **(done — 13A; re-verified)**
2. Backend integration tests against disposable PostgreSQL. **(done — 13A; re-run)**
3. Complete Playwright execution path. **(completed — 25/25 this sprint)**
4. Real CI workflow. **(done — 13A; static-validated)**
5. Fix only infra/config defects found during the work. **(1 added: base MCP healthcheck)**
6. Base `docker-compose.yml` MCP env var mismatch. **(done — 13A)**
7. Frontend Docker `VITE_API_BASE_URL` build arg. **(done — 13A)**
8. Deterministic commerce E2E seed for the order-conflict scenario. **(done — this sprint)**
9. Dependency/security verification without churn. **(audits run; deferrals documented)**
10. Preserve all Sprint 12A/12B decisions. **(verified — §4, §15)**
11. This report. **(this document)**

---

## 3. Baseline From Sprint 12C / 13A

- Sprint 12C shipped the E2E _infrastructure_ (compose overlay, seed users,
  Playwright specs, static scanner) but could not **run** it — no Docker /
  Postgres / browser in that environment. Backend integration, Docker E2E, and
  the CI pipeline were all marked **NOT RUN**.
- Sprint 13A had Docker + Postgres. It replaced the placeholder `ci.yml` with a
  real 5-job pipeline, ran the backend integration suite for the first time
  (found + fixed 3 pre-existing security defects — HSTS delivery, account-lockout
  persistence, rate-limit path resolution), brought the full Docker stack up, and
  ran Playwright **24 passed / 1 `test.fixme`**.
- The single deferred item entering Sprint 13B: **9.10 order stock-conflict**,
  `test.fixme` pending a deterministic commerce seed.

---

## 4. Architecture Preservation

Re-verified against the live stack and the diff:

| Guarantee | Evidence |
|---|---|
| JWT access + refresh contract unchanged | no change to `JwtTokenUtil` / `RefreshTokenService` this sprint |
| Single-flight refresh, retry-at-most-once | E2E 9.5 / 9.6 pass live (exactly one `/auth/refresh`) |
| 403 never refreshes / never logs out | E2E 9.4 pass; new 9.10 oversell test also asserts `refreshCalls === 0`, `logoutCalls === 0` |
| 429 never refreshes / never logs out | E2E 9.8 pass live (Retry-After respected, token intact) |
| Query cache cleared on auth failure | E2E 9.7 pass (persisted store cleared, one redirect) |
| Role hierarchy ADMIN > MANAGER > STAFF | E2E 9.3 pass; backend is the authority (`@PreAuthorize`) |
| Bucket4j rate limiting, security headers, SecretsValidator | live header dump §11; `SecretsValidator.java` unchanged |
| MCP: no DB, no ORM, no LLM, key via existing filter | `security-check.mjs` 0/0; wrong-key → 401 live |
| RequestCorrelationFilter / `X-Request-Id` | echoed on every live response §11 |
| Import MCP read-only boundary | MCP suite 49/49; no upload tool exposed |

---

## 5. Docker Changes

| File | Change | Reason |
|---|---|---|
| `docker-compose.yml` (13A) | `mcp-server` env: `BACKEND_API_URL` / `PORT` / `MCP_TRANSPORT` replacing the ignored `BACKEND_BASE_URL` / `MCP_SERVER_PORT` | `mcp-server/src/config` reads the former names; the latter were silently dropped (Sprint 12C finding) |
| `docker-compose.yml` (13A) | `frontend.build.args.VITE_API_BASE_URL` wired; removed the no-op runtime `environment` entry | Vite bakes config at build time, not runtime |
| **`docker-compose.yml` (13B)** | **added a base `mcp-server` healthcheck** (`wget 127.0.0.1:3001/health`, `start_period 15s`) | base stack previously reported `Up` not `healthy`; the e2e overlay already had one — this makes `depends_on: condition: service_healthy` usable at base too. `127.0.0.1` avoids busybox's `::1` first-resolution miss |
| `frontend/Dockerfile` (13A) | `ARG VITE_API_BASE_URL` + `ENV` in the builder stage | documented public build-time config — **never** a secret; no `VITE_MCP_API_KEY`, no `JWT_SECRET` |
| `backend/Dockerfile` (13A) | `sed -i 's/\r$//' mvnw` before `chmod +x` | committed `mvnw` is CRLF → `./mvnw: not found` on Alpine |
| `mcp-server/Dockerfile` (13A) | created (compose referenced a non-existent file) | Node 20 multi-stage, non-root, `/health` HEALTHCHECK |
| `frontend/nginx.conf` (13A) | CSP `connect-src` allows the API origin | `'self'` blocked the SPA's own cross-origin XHR — surfaced only once the stack actually ran |
| `docker-compose.e2e.yml` (13A) | `RL_LOGIN_CAPACITY` / `RL_REFRESH_CAPACITY` → 50; frontend healthcheck host → `127.0.0.1` | serial Playwright logs in dozens of times; nginx is IPv4-only |

**Before / after — base `mcp-server` healthcheck (Phase 1 / Phase 11):**

```yaml
# before (13A): no healthcheck block on the base mcp-server service
# after (13B):
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://127.0.0.1:3001/health | grep -q '\"status\":\"UP\"' || exit 1"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 15s
```

**Verification:** `docker compose -f docker-compose.yml -f docker-compose.e2e.yml ps`
→ `cia-mcp-server … Up (healthy)`; MCP API-key mechanism unchanged
(wrong-key → 401, right-key → 200 live).

---

## 6. E2E Environment

Uses the existing `docker-compose.e2e.yml` overlay — **no stack duplication**.

| Requirement | Status | Evidence |
|---|---|---|
| Disposable PostgreSQL, deterministic DB name | PASS | `commerce_insight_e2e`, volume `cia-postgres-e2e-data` (own `name:`) |
| Isolated volume, safe to `down -v` | PASS | volume created fresh on `up`, removed on `down -v` |
| Backend uses `e2e` profile | PASS | `SPRING_PROFILES_ACTIVE=e2e`; boot log shows `e2e` secret in use |
| Flyway migrations run | PASS | `flyway_schema_history` populated through `V31` |
| Repeatable e2e seeds run | PASS | `R__seed_e2e_users.sql` **and** new `R__seed_e2e_commerce.sql` applied (extra location `classpath:db/e2e`) |
| Frontend starts | PASS | `cia-frontend … (healthy)`, `GET /` → 200 |
| MCP starts | PASS | `cia-mcp-server … (healthy)`, `/health` → `{"status":"UP","transport":"stdio"}` |
| Healthchecks work | PASS | all 4 services `(healthy)` within `start_period` |
| Security NOT weakened for E2E | PASS | real JWT/RBAC/rate-limit/headers; only tuning values differ (documented in `application-e2e.yml`) |
| E2E secrets are test-only | PASS | all labelled "not-for-production" in `docker-compose.e2e.yml` / `application-e2e.yml` |

`docker exec cia-postgres pg_isready -U postgres -d commerce_insight_e2e` → **accepting connections**.

---

## 7. E2E Commerce Seed

**New file:** `backend/src/main/resources/db/e2e/R__seed_e2e_commerce.sql`
(repeatable, e2e profile only — loaded solely because `application-e2e.yml` adds
`classpath:db/e2e` to `spring.flyway.locations`; never runs in dev/test/prod).

Minimum valid commerce graph, derived from the actual creation rules in
`OrderService.createOrder` → `OrderInventoryService.reserveInventory` →
`InventoryService.reserveStock`:

| Entity | Fixed UUID | Value | Rule satisfied |
|---|---|---|---|
| Customer | `e2e00000-…-000000000001` | `status = ACTIVE`, `active = true` | Order step 1: customer must be ACTIVE |
| Product | `e2e00000-…-000000000002` | `active = true`, price 10.0000 | Order step 2: product must be active |
| Inventory | `e2e00000-…-000000000004` | `quantity = 3`, `reserved = 0`, warehouse = Main (`00000000-…-000000000001`, seeded V14) | available = 3 |

**Idempotent:** `ON CONFLICT (id) DO NOTHING` on first insert, then an explicit
`UPDATE` re-asserts `quantity = 3, reserved_quantity = 0` on every run — so
repeated `docker compose up` without `down -v` stays deterministic (self-healing
if a prior run reserved stock).

### Status discrepancy: 409 vs 422 — resolved with justification

The Sprint 13B brief describes the scenario as _"backend returns HTTP 409"_. **The
existing backend has no 409 path for overselling.** `InventoryService.reserveStock`
throws `BusinessRuleException(INSUFFICIENT_STOCK)`, and
`GlobalExceptionHandler` maps **every** `BusinessRuleException` to
**HTTP 422 UNPROCESSABLE_ENTITY**. Existing `OrderControllerIntegrationTest`
cases already assert 422 for business-rule violations (inactive product, invalid
transition, cancel-shipped).

Per the brief — _"If the existing backend business rules do not permit a
deterministic scenario, DO NOT alter business rules merely to satisfy E2E"_ — the
status mapping was **not** changed. Forcing a 409 would require either a new
exception type or handler special-casing, changing the error contract and
breaking existing tests, purely for cosmetic alignment with the prompt's
assumed status code.

Instead, the E2E scenario is now **deterministic and un-`fixme`d**, asserting the
**real** contract. Every security-relevant guarantee a 409 test would check is
verified identically:

- response is the safe enveloped `{ success:false, error:{ code:"INSUFFICIENT_STOCK", message } }`
- `lib/apiError.ts` already lists `INSUFFICIENT_STOCK` as a **trusted code** (status 422 is also in its trusted set) → the backend message is shown verbatim and is guaranteed leak-free by `LEAK_PATTERN`
- no stack trace / no `org.springframework` / no `jdbc:` / no JWT in the body
- **no `/auth/refresh`, no `/auth/logout`, no redirect to `/login`** — a business conflict is not an auth failure
- session token still present afterwards

Frontend business logic is **not** duplicated — the test drives the real API and
asserts the real normalized envelope.

---

## 8. Backend Integration Verification

**Command (actually executed):**
```
docker compose up -d postgres                 # disposable Postgres 16, db commerce_insight
cd backend && ./mvnw -o -q test               # profile: test  (src/test/resources/application-test.yml)
```

**Result** (aggregated from 74 `target/surefire-reports/TEST-*.xml`):

| Suite | Tests | Passed | Failed | Errors | Skipped |
|---|---|---|---|---|---|
| **Full backend suite** | **421** | **419** | **0** | **0** | **2** |

Skipped = the two pre-existing, non-security `@Disabled` cases carried from 13A,
each with a verbatim reason in the test source:

- `OrderControllerIntegrationTest.getOrderById_notFound_returns404` — authenticated
  `GET /orders/{unknown-uuid}` returns 500 instead of 404 (order-module defect, tracked).
- `CustomerControllerIntegrationTest.addAddress_secondDefault_clearsFirst` —
  incomplete test asserting 404 on a route that returns 405.

**Security integration tests — all executed, all green:**

| Test class | Verifies | Result |
|---|---|---|
| `SecurityHardeningIntegrationTest` | enveloped 401 `AUTHENTICATION_REQUIRED` / 403 `ACCESS_DENIED`; `TOKEN_EXPIRED` / `TOKEN_INVALID`; CSP, `X-Frame-Options: DENY`, nosniff, Referrer-Policy, Permissions-Policy, **HSTS**; `X-Request-Id` echoed; CORS allowed vs disallowed origin; locked user + fresh token → 401; `GET /import/templates/{type}` requires auth | PASS |
| `RateLimitIntegrationTest` | Nth `/auth/login` → 429 envelope + `Retry-After` | PASS |
| `LoginHistoryIntegrationTest` | `login_history` rows on success + every failure branch; `audit_logs` `TOKEN_REFRESH` on `/auth/refresh` | PASS |

Sprint 13B changed **no backend test source** and **no backend main source**
(the only backend addition is the seed SQL, which the `test` profile does not
load). The 3 security fixes and 2 `@Disabled` annotations from 13A are unchanged.

---

## 9. Frontend Verification

Commands actually executed in `frontend/`:

| Check | Command | Result |
|---|---|---|
| TypeScript | `npx tsc --noEmit -p tsconfig.app.json` | **PASS** (exit 0) |
| Unit tests | `npx vitest run` | **PASS** — 5 files, **33 tests passed** (2.06 s) |
| Production build | `npm run build` | **PASS** — `✓ built in 7.20s`; one advisory: main JS chunk 1,388 kB (gzip 383 kB) — perf-only, see §16 |
| Lint (errors gate) | `npm run lint` | **PASS** — `✖ 17 problems (0 errors, 17 warnings)`, exit 0 |
| Lint (strict) | `npm run lint:strict` | 17 warnings → non-zero; intentionally not gated |

Vitest coverage includes `lib/apiError.test.ts` (12) and `services/axios.test.ts`
(8) — the 401/refresh/403/429 normalization contract.

---

## 10. Playwright Results

**Command (actually executed):**
```
docker compose -f docker-compose.yml -f docker-compose.e2e.yml build backend   # bake new seed
docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d            # 4 services → all healthy
cd frontend && E2E_BASE_URL=http://localhost:5173 E2E_API_BASE=http://localhost:8080 npx playwright test
```

**`25 passed (37.7s)`** — Total 25 · Passed 25 · Failed 0 · Skipped 0 · Fixme 0.

| # | Scenario | Result |
|---|---|---|
| 9.1 | Login STAFF/MANAGER/ADMIN, invalid pw, pw-cleared, duplicate-submit → 1 `POST /auth/login`, no token in DOM | PASS (6) |
| 9.2 | Unauthenticated → `/login`; no protected-content flash; authed loads route | PASS (3) |
| 9.3 | STAFF & MANAGER blocked from `/admin`; ADMIN allowed; nav hidden for STAFF; MANAGER opens manager pages | PASS (4) |
| 9.4 | Real 403 (`GET /users` as STAFF) → `ACCESS_DENIED`, no `/auth/refresh`, no logout, no stack trace | PASS |
| 9.5 | Corrupt access token → exactly one real `POST /auth/refresh` → original retried → Products renders | PASS |
| 9.6 | Corrupt token + multi-query reload → exactly one refresh (single-flight) | PASS |
| 9.7 | Corrupt both tokens → tokens + persisted store cleared, one redirect to `/login?session=expired&redirect=`, URL stable | PASS |
| 9.8 | Rapid `GET /export/products` (cap 3) → **429 + `Retry-After`**, no refresh, no logout | PASS |
| 9.9 | STAFF import → safe **403**; STAFF sees no file input + "available to Manager and Admin"; MANAGER `.txt` rejected client-side (0 upload calls); rapid uploads → **429** | PASS (4) |
| 9.10 | MANAGER opens orders list; STAFF `POST /orders` (valid body) → true **403 `ACCESS_DENIED`**, session intact; **oversell** (MANAGER, qty 50 vs stock 3) → safe **422 `INSUFFICIENT_STOCK`**, no refresh, no logout, no stack trace | **PASS (3)** |

**Change this sprint:** the empty `test.fixme('order creation surfaces a safe 409
business-conflict message')` stub is replaced by
`9.10 › order oversell surfaces a safe business-conflict message, session intact`
(test #18) — driven by the new commerce seed, asserting the real 422
`INSUFFICIENT_STOCK` envelope (see §7 for the 409→422 rationale).

---

## 11. MCP Verification

Commands actually executed in `mcp-server/`:

| Check | Result |
|---|---|
| `npm run type-check` (`tsc --noEmit`) | **PASS** (exit 0) |
| `npm test` (`node:test`) | **PASS** — `tests 49 · pass 49 · fail 0 · skipped 0` (263 ms) |
| `npm run build` (`tsc`) | **PASS** (exit 0) |

**Runtime (live container `cia-mcp-server`):**

| Check | Result |
|---|---|
| Server starts | PASS — `MCP Server started on STDIO transport` |
| Providers initialize | PASS — `Initializing 8 provider(s)... 8 providers initialized successfully` |
| STDIO transport | PASS — `/health` → `{"status":"UP","transport":"stdio"}` |
| Health endpoint | PASS — `Health check server listening on port 3001`, HTTP 200 |
| Backend connectivity | PASS — `BACKEND_API_URL=http://backend:8080/api/v1` reachable on `cia-network` |
| Correct MCP API key | PASS — `X-MCP-API-KEY: <e2e key>` → `GET /api/v1/products` **200** |
| Incorrect MCP API key | PASS — `X-MCP-API-KEY: wrong-key` → **401** |
| No DB / ORM / LLM imports, no key echo | PASS — `security-check.mjs` 0/0 |
| Import + Export tools present, read-only | PASS — MCP suite covers `import_*` job/template/status tools and the 3 read-only `export_*` tools; **no** binary-generation / upload tool exposed |

> Note: the brief says "7 providers". The live count is **8** — the
> `ExportToolsProvider` was added in Sprint 11C and is metadata/preview-only (no
> binary delivery). Recorded here as an expected, pre-existing discrepancy, not a
> regression.

---

## 12. CI Pipeline

`.github/workflows/ci.yml` (real, 5 jobs — replaced the 12C placeholder in 13A;
structure re-validated this sprint):

| Job | Steps |
|---|---|
| `backend` | checkout · `setup-java@v4` (temurin 17, `cache: maven`) · `postgres:16-alpine` service matching `application-test.yml` · `./mvnw -B -ntp clean verify` · upload surefire reports |
| `frontend` | `setup-node@v4` (20, `cache: npm`) · `npm ci` · `tsc --noEmit` · `npm test` · `npm run lint` · `npm run build` (`VITE_API_BASE_URL` env) |
| `mcp` | `npm ci` · `type-check` · `test` · `build` |
| `security` | `node scripts/security-check.mjs` |
| `e2e` | `needs: [backend, frontend, mcp, security]` · `playwright install --with-deps chromium` · `docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d --build` · health-wait loops · `npm run test:e2e` · **upload `playwright-report` + `test-results` always** · dump container logs on failure · `down -v` always |

- `on:` push (`main`, `develop`, `sprint*`) + PR (`main`, `develop`);
  `concurrency` cancel-in-progress; `permissions: contents: read`.
- Pinned major action versions; `npm ci` everywhere; Maven wrapper; no secrets
  referenced; E2E uses only the labelled test-only values. No production deploy.
- The `e2e` job rebuilds the backend image (`--build`), so the new
  `R__seed_e2e_commerce.sql` is picked up automatically — **no CI change needed**.

`.github/workflows/dependency-audit.yml` — weekly (`cron: 0 6 * * 1`) +
`workflow_dispatch`, `permissions: contents: read`:

| Job | Content |
|---|---|
| `npm-audit` | frontend (`--omit=dev` + all) and mcp (`|| true` — advisory) |
| `owasp` | `continue-on-error: true` · `./mvnw -Powasp -DskipTests … verify` · `NVD_API_KEY` secret optional · uploads `dependency-check-report.*` |

**Workflow: PASS (static validation).** Jobs present: backend, frontend, mcp,
security, e2e. Not executed on a GitHub runner this session — the equivalent
commands were all run locally (§8–§11).

---

## 13. Security Scanner

`node scripts/security-check.mjs` — **actually executed, twice** (after the seed
edit and after the spec edits):

```
── Sprint 12C static security scan ──────────────────────────────
scanned: backend/src, frontend/src, frontend/e2e, mcp-server/src
ERROR findings: 0
WARN  findings: 0
✓ clean — no findings
```

The new E2E spec contains a `eyJ[A-Za-z0-9_-]{10,}` **negative-assertion** regex;
the scanner correctly does not flag it (consistent with the existing `BOGUS_JWT`
fixture in `e2e/helpers.ts`). No allowlist entries were added.

---

## 14. Dependency Audit

Commands actually executed:

| Target | Command | Finding | Decision |
|---|---|---|---|
| frontend (prod) | `npm audit --omit=dev` | **2 moderate** — `react-router` / `react-router-dom` 6.30.4: open-redirect via backslash (CVE-2025-68470 bypass); `deserializeErrors()` constructor injection (SSR hydration) | **DEFER (AR-INFRA1)** — advisory range is `6.0.0 – 7.17.0`; the only fix is a **major** bump to react-router 7.18+. SPA has no SSR/hydration (`deserializeErrors` unreachable); the app's own redirect is a fixed `encodeURIComponent`-encoded `/login`. `npm audit fix --force` broke the build in 12C. No major bump merely to clean audit output. |
| frontend (all) | `npm audit` | 11 total (adds dev-only: vite/esbuild etc.) | dev-only; not shipped |
| mcp | `npm audit` | **6 (2 moderate, 4 high)** — `hono` / `@hono/node-server` (SDK **SSE** transport — unused, server runs **stdio**); `js-yaml` `!!omap` quadratic (no untrusted YAML parsed); `fast-uri`, `ip-address` (SSRF/trust-boundary — transitive, no untrusted host input in this adapter); `brace-expansion` DoS (via **eslint**, dev) | **DEFER (AR-INFRA2)** — every finding is a dev tool or an unused/transitive path under `@modelcontextprotocol/sdk`; non-`--force` `npm audit fix` cannot resolve them without an SDK/eslint major bump. `npm audit fix` **not run**. |
| backend | OWASP `dependency-check-maven` in the opt-in `-Powasp` Maven profile (`pom.xml`) + weekly `dependency-audit.yml` | not executed this session | **DEFER to the scheduled job** — the NVD data feed is a large external download with rate limits; keeping it out of the blocking pipeline is deliberate (`failBuildOnCVSS 11`, `continue-on-error`). |

No dependency was added, removed, or upgraded in Sprint 13B.
`frontend/package.json`, `frontend/package-lock.json`, `mcp-server/package.json`
were **not touched** this sprint.

---

## 15. Production Configuration

Re-verified (no change this sprint — `SecretsValidator.java`, `application-prod.yml`
untouched):

| Control | State |
|---|---|
| `JWT_SECRET` insecure default in prod | **rejected** — `SecretsValidator` throws `IllegalStateException`, app refuses to start |
| `MCP_API_KEY` insecure default in prod | **rejected** — same fail-fast |
| JWT minimum length | enforced — `MIN_JWT_SECRET_BYTES = 32` (256-bit HS256) |
| `SecretsValidator` active | yes — `ApplicationRunner`, profile-aware |
| CORS origins required, wildcard rejected, `allow-credentials: false` | live preflight: `Access-Control-Allow-Origin: http://localhost:5173` (echoed, not `*`) |
| Swagger disabled in prod | `springdoc.api-docs.enabled: false`, `swagger-ui.enabled: false` |
| Actuator limited in prod | `management.endpoints.web.exposure.include: health` only |
| Security headers active | live dump — HSTS, CSP, `X-Frame-Options: DENY`, nosniff, Referrer-Policy, Permissions-Policy |

Live 401 envelope (unauthenticated `GET /api/v1/products`):
```
{"success":false,"error":{"code":"AUTHENTICATION_REQUIRED","message":"Authentication is required."},"timestamp":"…"}
```

---

## 16. Known Limitations

**Pre-existing, non-security (tracked, `@Disabled` — not Sprint 13B scope):**
- `GET /orders/{unknown-uuid}` returns 500, not 404.
- `CustomerControllerIntegrationTest.addAddress_secondDefault_clearsFirst` asserts 404 on a 405 route.
- No `AnalyticsControllerIntegrationTest` (analytics service tests pass on real Postgres; the reported NULL-bind 500 was **not reproducible** in 13A and may already be resolved).

**Environment:**
- CI workflow not executed on a GitHub runner here — validated statically; all equivalent commands run locally.
- Backend image build from a cold Maven cache is slow on Windows Docker Desktop (~25 min in 13A). CI caches `~/.m2`. Incremental rebuild for the seed this sprint was ~3 s (deps cached).

**Deliberate Sprint 13B choices:**
- Order oversell asserts **422** `INSUFFICIENT_STOCK`, not 409 — business rules unchanged (§7).
- OWASP dependency-check stays opt-in / scheduled, never a merge gate.
- ESLint gates **errors only**; 17 advisory warnings remain (pre-existing chart/form `any`, react-refresh).
- Frontend main bundle > 500 kB — perf-only, deferred (no code-split this sprint).

---

## 17. Accepted-Risk Register

| ID | Risk | Status | Rationale | Mitigation | Future action |
|---|---|---|---|---|---|
| AR1–AR5 (12A) | authorization read-level inconsistencies; analytics no role floor; direct inventory adjust vs approval flow | **carried, unchanged** | documented in `SPRINT_12A_SECURITY_AUDIT.md`; conservative "document, don't change roles" decision | backend remains authz authority; `@PreAuthorize` on every handler | revisit if a role redesign is scheduled |
| AR-FE1–FE4 (12B) | refresh token in `localStorage`; SPA XSS surface | **carried, unchanged** | stateless JWT by design (ADR 003); no cookie/CSRF machinery | strict CSP at nginx; `lib/authTokens` is the only token accessor; `apiError` leak guard | reconsider only with a BFF/session redesign |
| **AR-INFRA1** | `react-router` 6.30.4 — 2 moderate (open-redirect, SSR deserialize) | **DEFER** | fix = major bump to 7.18+; no SSR in this SPA; redirect is fixed + encoded | `ProtectedRoute` builds `redirect` via `encodeURIComponent` to a constant `/login` | adopt react-router 7 in a dedicated upgrade sprint |
| **AR-INFRA2** | `mcp-server` — 6 (2 mod / 4 high): hono/@hono/node-server, js-yaml, fast-uri, ip-address, brace-expansion | **DEFER** | all dev-only or unused/transitive (SSE transport unused; stdio in use); no untrusted input reaches these paths | server runs stdio; no user-supplied YAML/URI/host parsing in the adapter | bump when `@modelcontextprotocol/sdk` ships a patched minor |
| **AR-INFRA3** | backend deps had no scanner | **RESOLVED (13A)** | `-Powasp` profile + weekly `dependency-audit.yml` | non-blocking scheduled job, report artifact | review findings weekly |
| **AR-INFRA4** | base compose MCP env mismatch | **RESOLVED (13A)** | `BACKEND_API_URL` / `PORT` / `MCP_TRANSPORT` now passed | verified live — MCP reaches backend | — |
| **AR-INFRA5** | frontend Dockerfile hard-coded API base | **RESOLVED (13A)** | `ARG VITE_API_BASE_URL` + `build.args` | documented public build-time config | — |
| **AR-INFRA6** | base `mcp-server` had no healthcheck | **RESOLVED (13B)** | added `/health` healthcheck | `cia-mcp-server (healthy)` live | — |
| AR-PERF1 | frontend main bundle > 500 kB | **DEFER** | perf-only, no security impact | gzip 383 kB; served by nginx with caching | manual chunking / route-level `import()` later |
| AR-DB1 | analytics NULL-bind 500 (12C report) | **NOT REPRODUCIBLE** | service tests green on real Postgres in 13A; no controller integration test exists | — | add `AnalyticsControllerIntegrationTest` to confirm/close |

No accepted risk was silently removed.

---

## 18. Exact Commands

```bash
# ── git safety ──────────────────────────────────────────────────────────────
git status --porcelain ; git diff --stat ; git diff --check

# ── static security scan ────────────────────────────────────────────────────
node scripts/security-check.mjs                       # 0 / 0

# ── backend integration (disposable Postgres) ──────────────────────────────
docker compose up -d postgres                         # cia-postgres, db commerce_insight
docker exec cia-postgres pg_isready -U postgres -d commerce_insight
cd backend && ./mvnw -o -q test                       # 421 / 419 / 0 / 0 / 2

# ── frontend ───────────────────────────────────────────────────────────────
cd frontend
npx tsc --noEmit -p tsconfig.app.json                 # exit 0
npx vitest run                                        # 33 passed
npm run build                                         # ✓ built in 7.20s
npm run lint                                          # 0 errors, 17 warnings, exit 0

# ── mcp ────────────────────────────────────────────────────────────────────
cd mcp-server
npm run type-check                                    # exit 0
npm test                                              # tests 49 · pass 49
npm run build                                         # exit 0

# ── full E2E stack + Playwright ───────────────────────────────────────────
docker compose -f docker-compose.yml -f docker-compose.e2e.yml down -v
docker compose -f docker-compose.yml -f docker-compose.e2e.yml build backend
docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d
docker compose -f docker-compose.yml -f docker-compose.e2e.yml ps         # 4 × (healthy)
docker exec cia-postgres psql -U postgres -d commerce_insight_e2e \
  -c "SELECT quantity FROM inventory WHERE id='e2e00000-0000-0000-0000-000000000004';"   # 3
cd frontend && E2E_BASE_URL=http://localhost:5173 E2E_API_BASE=http://localhost:8080 \
  npx playwright test                                 # 25 passed (37.7s)

# ── live security spot-checks ─────────────────────────────────────────────
curl -sI  http://localhost:8080/api/v1/products | grep -Ei 'strict-transport|content-security|x-frame|x-request-id'
curl -s    http://localhost:8080/api/v1/products                                   # AUTHENTICATION_REQUIRED envelope
curl -s -o /dev/null -w '%{http_code}' -H 'X-MCP-API-KEY: wrong-key' http://localhost:8080/api/v1/products   # 401
curl -s    http://localhost:3001/health                                            # {"status":"UP","transport":"stdio"}

# ── dependency audits ────────────────────────────────────────────────────
cd frontend && npm audit --omit=dev                   # 2 moderate — DEFER AR-INFRA1
cd mcp-server && npm audit                            # 6 (2 mod / 4 high) — DEFER AR-INFRA2

# ── teardown ─────────────────────────────────────────────────────────────
docker compose -f docker-compose.yml -f docker-compose.e2e.yml down -v
```

---

## 19. Exact Outputs

| Command | Output | Verdict |
|---|---|---|
| `node scripts/security-check.mjs` | `ERROR findings: 0` / `WARN findings: 0` / `✓ clean` | PASS |
| `./mvnw -o -q test` (backend) | exit 0; surefire aggregate **tests 421, passed 419, failures 0, errors 0, skipped 2** | PASS |
| `npx tsc --noEmit` (frontend) | exit 0 | PASS |
| `npx vitest run` | `Test Files 5 passed (5)` / `Tests 33 passed (33)` | PASS |
| `npm run build` (frontend) | `✓ built in 7.20s`; chunk-size advisory (1,388 kB) | PASS |
| `npm run lint` | `✖ 17 problems (0 errors, 17 warnings)`, exit 0 | PASS |
| `npm run type-check` (mcp) | exit 0 | PASS |
| `npm test` (mcp) | `ℹ tests 49` / `ℹ pass 49` / `ℹ fail 0` | PASS |
| `npm run build` (mcp) | exit 0 | PASS |
| `docker compose … ps` | `cia-postgres`, `cia-backend`, `cia-frontend`, `cia-mcp-server` all `Up (healthy)` | PASS |
| seed check `SELECT quantity …` | `3` | PASS |
| `npx playwright test` | `Running 25 tests using 1 worker` … `25 passed (37.7s)` | PASS |
| `curl -sI …/api/v1/products` | `Strict-Transport-Security: max-age=31536000 ; includeSubDomains ; preload`, `Content-Security-Policy: default-src 'none'; …`, `X-Frame-Options: DENY`, `X-Request-Id: …` | PASS |
| `curl …/api/v1/products` | `{"success":false,"error":{"code":"AUTHENTICATION_REQUIRED",…}}` | PASS |
| `curl -H 'X-MCP-API-KEY: wrong-key' …` | `401` | PASS |
| `curl …:3001/health` | `{"status":"UP","transport":"stdio"}` | PASS |
| `docker logs cia-mcp-server` | `Initializing 8 provider(s)... 8 providers initialized successfully` / `MCP Server started on STDIO transport` | PASS |
| `curl -X OPTIONS … -H 'Origin: http://localhost:5173'` | `Access-Control-Allow-Origin: http://localhost:5173` (not `*`) | PASS |
| `npm audit --omit=dev` (frontend) | `2 moderate severity vulnerabilities` (react-router) | DEFER (AR-INFRA1) |
| `npm audit` (mcp) | `6 vulnerabilities (2 moderate, 4 high)` | DEFER (AR-INFRA2) |
| `git diff --check` | `(no whitespace errors)` | PASS |

---

## 20. Final Acceptance Checklist

Legend: **PASS** = ran & passed · **SKIPPED** = not run here (reason given) · **FAIL** = ran & failed

### Infrastructure
- [x] Base docker-compose MCP env mismatch fixed — **PASS** (13A; verified live)
- [x] Frontend Docker build accepts `VITE_API_BASE_URL` — **PASS** (13A)
- [x] E2E compose remains isolated (own DB name + volume, no stack duplication) — **PASS**
- [x] Healthchecks work (4/4 services `healthy`, incl. new base MCP healthcheck) — **PASS**
- [x] Startup dependencies correct (`depends_on: condition: service_healthy`) — **PASS**

### Backend
- [x] `mvnw test-compile` — **PASS** (implied by full `test`)
- [x] Complete backend suite against disposable PostgreSQL — **PASS** (421 / 419 / 0 / 0 / 2)
- [x] Sprint 12A security integration tests — **PASS** (SecurityHardening / RateLimit / LoginHistory)

### Frontend
- [x] TypeScript — **PASS**
- [x] Vitest — **PASS** (33)
- [x] Production build — **PASS**
- [x] Playwright executes against the real stack — **PASS**

### E2E
- [x] Login tests — **PASS**
- [x] Protected-route tests — **PASS**
- [x] RBAC tests — **PASS**
- [x] 403 test — **PASS**
- [x] 401 refresh test — **PASS**
- [x] Concurrent-401 single-flight test — **PASS**
- [x] Refresh-failure test — **PASS**
- [x] 429 test — **PASS**
- [x] Import security test — **PASS**
- [x] Order security test — **PASS**
- [x] Order conflict scenario is no longer `fixme` — **PASS** (deterministic; asserts real **422 `INSUFFICIENT_STOCK`** — business rules unchanged, see §7)

### MCP
- [x] type-check — **PASS**
- [x] tests — **PASS** (49)
- [x] build — **PASS**
- [x] runtime health — **PASS**
- [x] providers initialize — **PASS** (8/8; brief said 7 — Export provider added in 11C, see §11)
- [x] API-key security verified — **PASS** (wrong → 401, right → 200; key never in body)

### Security
- [x] `security-check.mjs` — 0 errors — **PASS**
- [x] `security-check.mjs` — 0 warnings — **PASS**
- [x] Dependency audits executed — **PASS** (frontend, mcp)
- [x] Unresolved vulnerabilities documented — **PASS** (§14, §17: AR-INFRA1/2)
- [x] No new secrets — **PASS** (§17 review; scanner 0/0)

### CI
- [x] GitHub Actions workflow implemented — **PASS** (static) — not executed on a runner here
- [x] backend job — **PASS** (present)
- [x] frontend job — **PASS** (present)
- [x] MCP job — **PASS** (present)
- [x] security job — **PASS** (present)
- [x] E2E job — **PASS** (present)
- [x] Artifacts uploaded on E2E failure — **PASS** (`upload-artifact@v4`, `if: always()`)

### Documentation
- [x] Sprint 13B report created — **PASS** (this file)
- [x] Exact commands recorded — **PASS** (§18)
- [x] Exact outputs recorded — **PASS** (§19)
- [x] Limitations documented — **PASS** (§16)
- [x] Accepted risks documented — **PASS** (§17)

---

## Files Created (Sprint 13B)

```
backend/src/main/resources/db/e2e/R__seed_e2e_commerce.sql
docs/security/SPRINT_13B_PRODUCTION_READINESS.md
```

## Files Modified (Sprint 13B)

```
docker-compose.yml                              # + base mcp-server healthcheck
frontend/e2e/rate-limit-and-features.spec.ts    # test.fixme → real 9.10 oversell test
scripts/verify.sh                               # accept --e2e (alias of --with-e2e)
```

_(All other changed files in the working tree are Sprint 13A deliverables —
`docs/SPRINT_13A_*.md` records those.)_

## Architecture Changes

**NONE.**
