# Sprint 13D — Analytics Reliability & Dashboard Delivery

_Branch: `sprint13c` (13C + 13D changes present) · Executed: 2026-09-01 · Docker + PostgreSQL 16 available_

---

## 1. Problem

Every `GET /api/v1/analytics/*` endpoint returned **HTTP 500**. The analytics
**data** was already correct (proven in Sprint 13C directly against the seeded
demo database: 10,801,219,074 VND revenue, 600 orders, 192 unique customers,
3,694 products sold, 52 cancelled) but nothing could flow through the HTTP layer,
so the React dashboard could not render.

## 2. Root Cause

Every method in `AnalyticsRepository` is a **native** query with the nullable
date-filter idiom:

```sql
(:dateFrom IS NULL OR o.created_at >= :dateFrom)
```

Spring Data replaces `:dateFrom` with a positional `?`. PostgreSQL prepares the
statement **before** any value is bound, and cannot infer a type for `$1` in
`$1 IS NULL` from that context, so it aborts:

```
ERROR: could not determine data type of parameter $1
```

This fails on **any** data — empty or seeded — and is unrelated to Sprint 13C's
seed. (Sprint 13A had marked it "not reproducible" from service-layer unit tests
that mock the repository; the HTTP path makes it 100% reproducible.)

## 3. Repository Fix

`backend/src/main/java/com/commerceinsight/analytics/repository/AnalyticsRepository.java`
— every nullable timestamp parameter is wrapped in an explicit cast, in all
**12 `@Query` methods** (the 6 endpoints' underlying queries):

```sql
-- before
AND (:dateFrom IS NULL OR o.created_at >= :dateFrom)
AND (:dateTo   IS NULL OR o.created_at <= :dateTo)
-- after
AND (CAST(:dateFrom AS timestamptz) IS NULL OR o.created_at >= CAST(:dateFrom AS timestamptz))
AND (CAST(:dateTo AS timestamptz)   IS NULL OR o.created_at <= CAST(:dateTo AS timestamptz))
```

`CAST(? AS timestamptz)` gives PostgreSQL the parameter type at prepare time;
`CAST(NULL AS timestamptz)` is a typed NULL, so `IS NULL` still short-circuits the
all-time case. **No semantics changed** — same revenue definition, same
revenue-eligible statuses (`CONFIRMED/PROCESSING/SHIPPED/DELIVERED/COMPLETED`),
same joins, grouping, sorting, aggregation, DTO shapes, endpoint paths.
`git diff` = 24 predicate lines changed, nothing else. `AnalyticsService`,
`AnalyticsController`, all DTOs and projections: **untouched**.

## 4. Endpoints Verified

Live, through the Sprint 13C demo Docker stack (`commerce_insight_demo`), logged
in as `demo-admin`:

| Endpoint | Before | After |
|---|---|---|
| `GET /analytics/overview` | 500 | **200** — `totalRevenue: 10,801,219,074`, `totalOrders: 600`, `totalCustomers: 192`, `totalProductsSold: 3,694`, `cancelledOrders: 52`, `cancellationRate: 8.67`, `averageOrderValue: 21,602,438.15`, `currency: "VND"` |
| `GET /analytics/revenue?groupBy=MONTH` | 500 | **200** — 11 points, `2025-10` (798,968,076 / 28 orders) … `2026-08` (1,996,069,110 / 100 orders) |
| `GET /analytics/orders` | 500 | **200** — status distribution |
| `GET /analytics/products/top?limit=5` | 500 | **200** — named products with revenue + quantity |
| `GET /analytics/customers` | 500 | **200** — unique / new / repeat customers |
| `GET /analytics/payments` | 500 | **200** — CARD / CASH / BANK_TRANSFER / OTHER breakdown |
| `…?dateFrom=…&dateTo=…` (from-only, to-only, both) | 500 | **200** — no PostgreSQL type error |
| `…?dateFrom=2099-01-01T00:00:00Z` (empty future range) | 500 | **200** — zero/empty per existing contract |
| `GET /analytics/overview` **no token** | 401 | **401** — auth still required |

## 5. Dashboard Implementation

The analytics dashboard **already existed and was already correctly wired** —
`src/features/analytics/` (`AnalyticsPage`, `analyticsService` on the shared
`apiClient`, `useAnalytics` TanStack Query hooks with a stable key factory, and
seven components with per-section loading / error / empty states using Recharts,
which is already a dependency). It only failed because the API 500'd. This is
Sprint 13D §7 "option 1": no dashboard rebuilt.

**Only change:** `frontend/src/router/index.tsx` — `/` and `/dashboard` now
render `<AnalyticsPage />` instead of `<DashboardPlaceholder />` (4 lines). The
"Dashboard" sidebar item and the post-login landing page now show the real
analytics dashboard. `/analytics` still points to the same page;
`DashboardPlaceholder` stays in the tree (still used by `/admin`).

No analytics component, hook, service, type, or `QueryProvider` config was
touched. No second Axios client / QueryClient. No Zustand for analytics (it is
server state via TanStack Query). No `fetch()` calls added to app code.

## 6. Date Filtering

`AnalyticsDateFilter` (default "Last 30 Days") already existed in `AnalyticsPage`
and drives a single `range` object shared by every query; the query keys include
`range`, so changing dates refetches every card/chart with the correct key and no
stale mix. Verified:

- **No filter** — dashboard default, all endpoints 200.
- **`dateFrom` only / `dateTo` only / both** — `AnalyticsControllerIntegrationTest.dateFilterPermutations_allReturn200` exercises all 6 endpoints × 4 combinations (24 requests) → all 200.
- **Empty future range** — `emptyDateRange_returnsZeroes`: overview totals all 0, revenue series empty, payments breakdown empty — existing contract, HTTP 200 (not an error).

The backend contract was **not** changed to fit any new UI; the minimal existing
filter UI consumes it as-is.

## 7. Tests

| Suite | Command | Result |
|---|---|---|
| Backend — full | `./mvnw -o test` | **432 tests / 430 passed / 0 failed / 0 errors / 2 skipped** (baseline 421/419/0/0/2 + **11 new**; the 2 skips are the pre-existing `@Disabled` Order/Customer cases — unchanged) |
| Backend — new | `./mvnw -o test -Dtest=AnalyticsControllerIntegrationTest` | **11 / 11 passed** (real PostgreSQL, full HTTP stack) |
| Frontend — types | `npx tsc --noEmit` | **PASS** |
| Frontend — unit | `npx vitest run` | **33 / 33 passed** (incl. `router/__tests__/guards.test.tsx`) |
| Frontend — build | `npm run build` | **PASS** (`✓ built in 7.46s`; >500 kB advisory — pre-existing, §10) |
| Frontend — Dashboard E2E (new) | `npx playwright test dashboard.spec.ts` (vs demo stack) | **3 / 3 passed** |
| MCP | `npm run type-check` / `npm test` / `npm run build` | **PASS / 49-49 / PASS** |
| Security | `node scripts/security-check.mjs` | **0 errors / 0 warnings** |

`AnalyticsControllerIntegrationTest` (`@SpringBootTest` + MockMvc + real Postgres,
fixture `seed_analytics_test.sql` = 12 orders / 3 months, cleanup
`cleanup_analytics_test.sql`): Overview non-zero KPIs · Revenue ≥ 2 monthly points
& non-negative · Orders status distribution & consistent totals · Top products
named + valued · Customers non-empty · Payments breakdown non-negative · **date
parameter permutations all 200 (the regression)** · empty range → zero/empty ·
no-token → 401.

## 8. Demo Verification

Sprint 13C demo stack, `docker compose -f docker-compose.yml -f docker-compose.demo.yml up -d --build`,
all 5 containers healthy. `POST /api/v1/auth/login` as `demo-admin@commerceinsight.demo`
→ 200 (token not printed). Then the six analytics endpoints → all **200** with the
exact seeded metrics listed in §4.

Dashboard E2E (`frontend/e2e/dashboard.spec.ts`, real Chromium vs `:5173`):

1. **ADMIN lands on `/dashboard`, sees non-zero KPI + chart** — KPI cards render, every `/api/v1/analytics/*` response observed was **200**, a KPI value contains a real digit (not `—`), the Recharts SVG renders, and the DOM contains no `could not determine data type` / `org.springframework` / stack-trace / JWT text.
2. **Overview API through the stack** — `totalRevenue > 0`, `totalOrders > 0`, `totalCustomers > 0`.
3. **Forced 500 on one endpoint** — page shows "Unable to load overview data", other sections still render, no internal detail leaks.

## 9. Security Verification

- `node scripts/security-check.mjs` → **0 / 0**.
- No secret / JWT literal / MCP key added. Frontend source unchanged except the
  router (no `console.*`, no token access, no `dangerouslySetInnerHTML`, no
  `document.cookie`).
- Analytics still `@PreAuthorize("isAuthenticated()")`; no-token → 401 (backend
  test + live curl). Not made public.
- 12A/12B untouched: Axios refresh interceptor, `lib/apiError` normalization,
  `RoleGuard`, `QueryProvider` (no 4xx retry), rate limiting — none modified.
  A failing analytics call surfaces through the existing `lib/apiError`
  normalization; the components render its safe message.
- MCP unchanged — REST-only, no DB driver / ORM / LLM. Analytics MCP tools now
  receive 200 from the backend automatically.

## 10. Before / After Behavior

| | Before (13C) | After (13D) |
|---|---|---|
| `GET /api/v1/analytics/*` | HTTP 500 `INTERNAL_ERROR` | HTTP 200, enveloped data |
| `/dashboard`, `/` | `DashboardPlaceholder` (user info only) | real analytics dashboard |
| Analytics page (`/analytics`) | 6 "Unable to load…" panels | KPI cards + revenue chart + status chart + payment chart + customer card + top-products table, all populated |
| Date filter | irrelevant (all 500) | works — no filter / from / to / both / empty |
| Backend suite | 421 / 419 / 2 skip | 432 / 430 / 2 skip |

## 11. Known Limitations

- **Frontend bundle > 500 kB** — pre-existing (flagged in 13C). The router uses
  static imports for every page; converting to `React.lazy` + `Suspense` is a
  cross-cutting change touching every route and is out of scope for an
  analytics-fix sprint (§14 says leave it and document). Recharts (the largest
  dependency) is needed by the now-default dashboard route anyway, so lazy-loading
  analytics would not shrink the initial chunk. **Follow-up.**
- `/dashboard` breadcrumb (Header.tsx) reads "Dashboard" while the page `<h1>` (from
  `AnalyticsPage`) reads "Ecommerce Analytics" — cosmetic; not changed to avoid
  touching an unrelated component.
- **Pre-existing** (unchanged, unrelated): `GET /orders/{unknown}` → 500 not 404;
  one `@Disabled` customer-address test.

## 12. Files Changed

**Created**
```
backend/src/test/java/com/commerceinsight/analytics/controller/AnalyticsControllerIntegrationTest.java
backend/src/test/resources/db/seed_analytics_test.sql
backend/src/test/resources/db/cleanup_analytics_test.sql
frontend/e2e/dashboard.spec.ts
docs/analytics/SPRINT_13D_ANALYTICS_DASHBOARD.md
```

**Modified**
```
backend/src/main/java/com/commerceinsight/analytics/repository/AnalyticsRepository.java   (12 native queries: CAST nullable date params)
frontend/src/router/index.tsx                                                             ( / and /dashboard -> AnalyticsPage )
```

_(The working tree also carries the uncommitted Sprint 13C deliverables —
`application-demo.yml`, `db/demo/`, `docker-compose.demo.yml`, `docs/demo/`,
`scripts/demo-*.sh`, `scripts/README.md`.)_

## 13. Commands Executed

```bash
git status ; git diff --check
# backend fix + tests
./mvnw -o test -Dtest=AnalyticsControllerIntegrationTest      # 11/11
./mvnw -o test                                                # 432 / 430 / 0 / 0 / 2
# frontend
npx tsc --noEmit                                              # PASS
npx vitest run                                                # 33/33
npm run build                                                 # PASS
# mcp
npm run type-check ; npm test ; npm run build                 # PASS / 49-49 / PASS
# security
node scripts/security-check.mjs                               # 0 / 0
# live demo stack
docker compose -f docker-compose.yml -f docker-compose.demo.yml up -d --build
docker exec cia-postgres createdb -U postgres commerce_insight   # for @SpringBootTest datasource
curl -s -X POST .../auth/login -d '{"email":"demo-admin@commerceinsight.demo",...}'   # 200
for ep in overview 'revenue?groupBy=MONTH' orders 'products/top?limit=5' customers payments ; do
  curl -s -o /dev/null -w '%{http_code}\n' -H "Authorization: Bearer $T" ".../analytics/$ep" ; done   # 200 x6
E2E_ADMIN_EMAIL=demo-admin@commerceinsight.demo E2E_ADMIN_PASSWORD='DemoAdmin!2024' \
  npx playwright test dashboard.spec.ts                       # 3/3
```

## 14. Actual Test Results

```
Backend  : Tests run: 432, Passed: 430, Failed: 0, Errors: 0, Skipped: 2
           (AnalyticsControllerIntegrationTest: tests=11 failures=0 errors=0 skipped=0)
Frontend : tsc PASS · vitest 33 passed / 0 failed · build PASS
           dashboard.spec.ts: 3 passed
MCP      : type-check PASS · node:test 49 pass / 0 fail · build PASS
Security : ERROR findings: 0 · WARN findings: 0
```

## 15. E2E — full Sprint 12C suite

**NOT RUN this session.** The 25-test suite targets the `e2e` Docker stack and its
own seed users (`e2e-*@commerceinsight.test`); running it needs a fresh `e2e`
backend image build (~20 min on this host). Sprint 13D changed only the analytics
native SQL (covered by 11 new integration tests + the full 432-test suite) and a
4-line route re-point (covered by `tsc`, `vitest` incl. `guards.test.tsx`, and the
3 new Dashboard E2E tests). No authentication / RBAC / refresh / rate-limit /
security code was touched. Last full green run: Sprint 13B, 25/25.

Command to run it later:
```
docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d --build
cd frontend && npx playwright install chromium && npm run test:e2e
docker compose -f docker-compose.yml -f docker-compose.e2e.yml down -v
```
