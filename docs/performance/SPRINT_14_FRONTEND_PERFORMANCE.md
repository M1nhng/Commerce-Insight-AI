# Sprint 14 — Frontend Performance & Production Polish

Status legend: **PASS** = executed & verified · **NOT RUN** = not executed this sprint ·
**PRE-EXISTING** = present before Sprint 14 · **KNOWN LIMITATION** = accepted, tracked.

---

## 1. Problem

1. `npm run build` emitted a single `dist/assets/index-*.js` of **1,388.77 kB**
   (gzip 382.73 kB) and Vite's *"Some chunks are larger than 500 kB"* warning.
   Every route — including `/login` — downloaded the entire app plus Recharts up
   front. All routes used static `import` in `src/router/index.tsx`; there was no
   `React.lazy` / route-level code splitting anywhere.
2. The dashboard breadcrumb (header) read **"Dashboard"** while the page `<h1>`
   read **"Ecommerce Analytics"** — and on `/` the breadcrumb fell through to
   *"Commerce Insight AI"*. Three routes (`/`, `/dashboard`, `/analytics`) render
   the one `AnalyticsPage` with a hard-coded title.
3. Two pre-existing backend defects tracked since Sprint 13A:
   `GET /api/v1/orders/{id}` returned **500** for any id (a
   `MultipleBagFetchException`), and one customer-address integration test was
   `@Disabled` because its final assertion targeted a non-existent route.
4. `.github/workflows/cd.yml` was a Sprint-0 `echo TODO` placeholder.

Already resolved by Sprint 13A (verified, no work needed): ESLint 9 flat config
(`frontend/eslint.config.js`), a complete `ci.yml`, `dependency-audit.yml`, and
`frontend/Dockerfile` already carrying `ARG VITE_API_BASE_URL` + `ENV`.

---

## 2. Baseline (before Sprint 14)

| Metric | Value |
|---|---|
| Build command | `tsc --noEmit -p tsconfig.app.json && vite build` |
| JS files in `dist/assets` | **1** |
| `index-*.js` (entry, all code) | **1,388.77 kB** raw / **382.73 kB** gzip |
| `index-*.css` | 18.93 kB / 4.57 kB gzip |
| Vite ">500 kB" warning | **present** |
| `npm run lint` | exit **0**, 0 errors, **17 warnings** (advisory, pre-existing) |
| `npx vitest run` | **33 passed** |
| Backend `./mvnw -o test` | 432 tests, 430 passed, **2 skipped** |
| MCP `npm test` | 49 passed |
| `node scripts/security-check.mjs` | 0 errors / 0 warnings |

---

## 3. Changes

### 3.1 Route-level code splitting — `src/router/index.tsx`
Every **page-level** component is now `React.lazy(() => import(...))` (named
exports adapted with `.then(m => ({ default: m.X }))`). Kept **static** (bootstrap
/ auth-critical, per brief): `ProtectedRoute`, `RoleGuard`, `AppShell`,
`LoginPage`, all providers, the Zustand auth store, the shared Axios client.
Route paths, guards and permissions are **unchanged**.

### 3.2 Single Suspense boundary — `src/components/layout/AppShell.tsx`
`<Outlet/>` is wrapped in one `<Suspense fallback={<AppLoader/>}>` (the existing
initialization loader — no new spinner component). `ProtectedRoute` /
`RoleGuard` still resolve **before** any lazy element mounts, so lazy loading
never bypasses authentication or RBAC (covered by
`e2e/dashboard.spec.ts` → *"a lazy-loaded protected route still redirects an
unauthenticated visitor"* and by the unchanged `guards.test.tsx`).

### 3.3 Vendor chunking — `vite.config.ts`
Added `build.rollupOptions.output.manualChunks` splitting the two heaviest vendor
groups out of the entry chunk:
* `charts-vendor` — `recharts` + its transitive `d3-*`, `victory-vendor`,
  `internmap`, `decimal.js-light` (only the dashboard/analytics route needs it).
* `react-vendor` — `react`, `react-dom`, `react-router(-dom)`, `scheduler`
  (shared by every route).

The `chunkSizeWarningLimit` was **not** touched — the warning is gone because the
chunks are genuinely smaller, not because the threshold was raised.

### 3.4 Dashboard title consistency — `AnalyticsPage.tsx` + `Header.tsx` + router
* `AnalyticsPage` takes an optional `title?: string` prop (default `"Dashboard"`)
  and renders it as the `<h1>` instead of the literal `"Ecommerce Analytics"`.
* Router passes `title="Dashboard"` for `/` and `/dashboard`, `title="Analytics"`
  for `/analytics`.
* `Header.tsx` `ROUTE_LABELS` gains `'/': 'Dashboard'`.
* Result: header breadcrumb == in-page `<h1>` on every route the page serves.
  No API, query-key, chart, date-filter or functional change.

### 3.5 Backend defects — see `docs/analytics/` sibling notes and §7 below
* `OrderRepository.findByIdWithDetails` now fetch-joins **one** collection
  (`items`) plus the non-collection `payment` / `customer`; `addresses` and
  `statusHistory` initialise lazily inside the existing
  `@Transactional(readOnly = true)` `OrderService.findById`. The full
  `OrderResponse` is byte-for-byte the same; the not-found path now reaches
  `ResourceNotFoundException(ORDER_NOT_FOUND)` → **404** envelope.
* `OrderControllerIntegrationTest.getOrderById_notFound_returns404` re-enabled &
  strengthened (asserts `ORDER_NOT_FOUND`, no stack-trace leak); new
  `getOrderById_malformedId_returns400`.
* `CustomerControllerIntegrationTest.addAddress_secondDefault_clearsFirst`
  re-enabled; its final step now verifies "one default per type" via the address
  **list** endpoint (the route that exists) instead of a non-existent
  single-address GET.

---

## 4. Bundle comparison (after Sprint 14)

`npm run build` (`vite v6.4.3`):

| Metric | Before | After |
|---|---|---|
| JS files in `dist/assets` | 1 | **68** |
| Entry `index-*.js` | 1,388.77 kB / 382.73 kB gz | **331.06 kB / 105.59 kB gz** |
| `react-vendor-*.js` | — | 260.38 kB / 83.03 kB gz |
| `charts-vendor-*.js` (dashboard only) | — | 421.23 kB / 113.71 kB gz |
| Largest single chunk | 1,388.77 kB | **421.23 kB** (`charts-vendor`) |
| `index-*.css` | 18.93 kB | 18.93 kB (unchanged) |
| Vite ">500 kB" warning | **present** | **gone** |
| `/login` first load (JS) | ~1,389 kB (entry) | ~591 kB (`react-vendor` + entry) — **no charts** |
| Per-feature route chunks (Products, Orders, Customers, Inventory, …) | in entry | separate 2–40 kB chunks, fetched on navigation |

Total raw JS across all chunks rose marginally (~1.39 MB → ~1.41 MB) from
chunk-boundary overhead — expected and immaterial; what matters is that no route
loads more than it needs and the largest chunk is well under the 500 kB gate.

---

## 5. Lazy-loaded routes

All of: `/` & `/dashboard` & `/analytics` (`AnalyticsPage`), `/profile`,
`/products`, `/categories`, `/inventory`, `/warehouses`, `/customers` (+`/new`,
`/groups`, `/:id`, `/:id/edit`), `/orders` (+`/new`, `/:id`), `/import`
(+`/jobs`, `/jobs/:id`), `/export`, `/admin` (`DashboardPlaceholder`, still
behind `RoleGuard roles={['ADMIN']}`).

Not lazy (intentional): `/login` (`LoginPage`) and the `*` fallback — first paint
for unauthenticated visitors; `ProtectedRoute`, `RoleGuard`, `AppShell`,
providers, auth store, Axios client.

---

## 6. ESLint status — **PASS (unchanged infra)**

`frontend/eslint.config.js` (flat config, ESLint 9) already existed from Sprint
13A; no config change was needed or made.

```
npm run lint  →  exit 0
✖ 17 problems (0 errors, 17 warnings)
```

All 17 are **pre-existing** and explicitly kept advisory by the config's own
comments:
* 15 × `@typescript-eslint/no-explicit-any` — Recharts tooltip/label callbacks
  and a few RHF adapters (`OrderStatusChart`, `PaymentMethodChart`,
  `RevenueTrendChart`, `CreateOrderForm`, `OrderDetailPage`,
  `CreateCustomerPage`, `EditCustomerPage`, `CustomerGroupsPage`,
  `CategoriesPage`).
* 2 × `react-refresh/only-export-components` — `QueryProvider.tsx`,
  `ThemeProvider.tsx` (HMR ergonomics, not correctness).

Sprint 14 introduced **0** new lint findings (router, AppShell, AnalyticsPage,
Header, vite.config all clean). `tsc --noEmit` remains the source of truth for
type correctness and passes.

---

## 7. Backend defects fixed

| Endpoint / test | Before | After |
|---|---|---|
| `GET /api/v1/orders/{unknown-uuid}` | **500** `INTERNAL_ERROR` (`MultipleBagFetchException`) | **404** `{"success":false,"error":{"code":"ORDER_NOT_FOUND",…}}` — verified live on the demo stack |
| `GET /api/v1/orders/{real-uuid}` | 500 | **200** full detail (regression check — addresses & history still present) |
| `GET /api/v1/orders/not-a-uuid` | 400 (already) | **400** (new explicit test) |
| `OrderControllerIntegrationTest` | 10 tests, **1 skipped** | **11 tests, 0 skipped** |
| `CustomerControllerIntegrationTest.addAddress_secondDefault_clearsFirst` | `@Disabled` (asserted 404 on a 405 route) | **enabled, passes** — verifies via address list |
| `CustomerControllerIntegrationTest` | 28 tests, **1 skipped** | **29 tests, 0 skipped** |
| Full backend suite | 432 / 430 pass / **2 skipped** | **433 / 433 pass / 0 skipped** |

No new `@Disabled` anywhere. `grep -rn "@Disabled" backend/src/test` → **none**.

---

## 8. CI workflow

`.github/workflows/ci.yml` — **unchanged** (already complete since Sprint 13A):
`backend` (`./mvnw -B -ntp clean verify` against a service Postgres),
`frontend` (`tsc --noEmit`, `npm test`, `npm run lint`, `npm run build`),
`mcp` (type-check, `node --test`, build), `security` (`security-check.mjs`),
`e2e` (Docker `docker-compose.e2e.yml` + Playwright). No repository secrets
referenced. YAML re-validated this sprint.

---

## 9. CD workflow

`.github/workflows/cd.yml` — **rewritten** (was a placeholder). See
`docs/deployment/SPRINT_14_CICD.md` for the full description. Summary:
* Triggers: `workflow_run` after **CI succeeds on `main`** + manual
  `workflow_dispatch`. Never runs from a feature branch.
* `guard` job gates on `github.event.workflow_run.conclusion == 'success'`.
* `build-images` job builds & pushes 3 images to **GHCR**
  (`ghcr.io/<owner>/<repo>/{backend,frontend,mcp-server}:{latest,<sha>}`) using
  the built-in `GITHUB_TOKEN` (`permissions: packages: write`) — no extra secret.
  Frontend build-arg `VITE_API_BASE_URL` comes from a repo/environment
  **Variable** (public), not a secret.
* `deploy` job — `environment: production`; a guarded skeleton that runs the SSH
  release **only** when `secrets.DEPLOY_SSH_HOST` is set, otherwise self-skips
  with an explanatory `::notice::`. Uses `SPRING_PROFILES_ACTIVE=prod`. The demo
  profile / `db/demo/**` seed are never referenced.
* No secret value is hard-coded or echoed anywhere.

---

## 10. Security considerations

* `node scripts/security-check.mjs` → **0 errors / 0 warnings** (PASS).
* No new secret, JWT literal, or MCP key in frontend source, workflows, or docs.
* No change to: JWT flow, single-flight refresh, 401-retry-once, 403 / 429
  handling, `Retry-After`, `X-Request-Id`, `authTokens` abstraction,
  `PermissionDenied`, `RoleGuard` hierarchy, TanStack Query retry policy, MCP API
  key filter, `SecretsValidator`. The Axios interceptor and `lib/apiError` are
  untouched — a failing analytics/order call still normalises through them.
* Lazy loading is inside `ProtectedRoute` → a code-split page cannot render
  before auth resolves (E2E-verified).
* CD publishes images with `GITHUB_TOKEN` only; production secrets are
  referenced via `secrets.*`, never inlined; deploy is pinned to `main` + the
  `production` GitHub environment (add required reviewers in repo settings).

---

## 11. Tests

| Suite | Command | Result |
|---|---|---|
| Backend | `./mvnw -o test` | **433 run, 0 failures, 0 errors, 0 skipped** — BUILD SUCCESS |
| ↳ `OrderControllerIntegrationTest` | `-Dtest=OrderControllerIntegrationTest` | 11 / 0 / 0 / 0 |
| ↳ `CustomerControllerIntegrationTest` | `-Dtest=CustomerControllerIntegrationTest` | 29 / 0 / 0 / 0 |
| Frontend types | `npx tsc --noEmit -p tsconfig.app.json` | exit 0 |
| Frontend unit | `npx vitest run` | **33 passed** (5 files) |
| Frontend build | `npm run build` | exit 0, no >500 kB warning |
| Frontend lint | `npm run lint` | exit 0 — 0 errors, 17 pre-existing warnings |
| MCP | `npm run type-check` / `npm test` / `npm run build` | 0 / **49 passed** / 0 |
| Security | `node scripts/security-check.mjs` | **0 / 0** |

---

## 12. E2E status

| Suite | Status | Detail |
|---|---|---|
| `e2e/dashboard.spec.ts` (5 tests) | **PASS** | Run against the freshly rebuilt Sprint 13C demo stack (`docker compose -f docker-compose.yml -f docker-compose.demo.yml up -d --build backend frontend`). Covers: ADMIN lands on `/dashboard`, non-zero KPIs, chart SVG, all `/api/v1/analytics/*` → 200, no backend-internals leak; overview API real totals; forced-500 degrades to a safe per-section message; **breadcrumb == `<h1>` on `/dashboard` and `/analytics`**; **a lazy `/orders` route still redirects an unauthenticated visitor to `/login`**. |
| `playwright test --list` | **PASS** | 30 tests collected across 5 files (25 Sprint 12C + 5 dashboard). |
| Sprint 12C suite — `auth`, `rbac`, `token-refresh`, `rate-limit-and-features` (25 tests) | **NOT RUN** | Targets the separate `e2e` stack + `e2e-*@commerceinsight.test` seed users (needs a ~20-min `docker-compose.e2e.yml` rebuild); its rate-limit test expects the `e2e` cap, not the demo cap. Sprint 14 changed no auth / RBAC / refresh / rate-limit / interceptor code; `guards.test.tsx` (Vitest) covers `ProtectedRoute`/`RoleGuard`, and the new lazy-auth E2E covers the split-route guard path. Last full green run: Sprint 13B (25/25). Re-run: `docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d --build` then `E2E_BASE_URL=http://localhost:5173 npm run test:e2e`. |

---

## 13. Known limitations

* **`charts-vendor` is 421 kB raw / 114 kB gzip.** Recharts + d3 is the app's
  single largest dependency and the analytics dashboard is the default landing
  route, so it loads on first authenticated paint regardless. It no longer trips
  the 500 kB gate and no longer burdens `/login` or any non-analytics route.
  Further reduction would mean swapping the charting library — out of scope,
  tracked as a follow-up.
* Marginal total-JS increase (~20 kB across 68 chunks) from chunk-boundary
  runtime — accepted.
* Sprint 12C 25-test E2E suite **NOT RUN** this sprint (rationale above).
* Pre-existing, unrelated, unchanged: 15 `no-explicit-any` + 2 `react-refresh`
  lint warnings (advisory by config).

---

## 14. Production deployment prerequisites

To turn the `deploy` job from "skeleton" to "live" (see
`docs/deployment/SPRINT_14_CICD.md` for the full list):

1. A reachable host with Docker + Docker Compose and a checked-out
   `docker-compose.yml` + a **production overlay** (not committed; must set
   `SPRING_PROFILES_ACTIVE=prod` and pull the GHCR images).
2. Repository **secrets**: `DEPLOY_SSH_HOST`, `DEPLOY_SSH_USER`, `DEPLOY_SSH_KEY`,
   `DEPLOY_PATH`.
3. Host `.env` supplying every value `application-prod.yml` requires from the
   environment: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
   `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET` (≥ 256-bit, not a dev default —
   `SecretsValidator` aborts startup otherwise), `MCP_API_KEY`,
   `CORS_ALLOWED_ORIGINS`, optionally `TRUSTED_PROXIES`, `RATE_LIMIT_ENABLED`.
4. Repository/environment **Variable** `VITE_API_BASE_URL` = the public API
   origin (baked into the frontend image at build time).
5. On the `production` GitHub environment: required reviewers + restrict to the
   `main` branch.
