# Sprint 15 — Testing

Consolidation run. Every command below was **actually executed**; results are
verbatim. Classification: **PRE-EXISTING** / **NEW** / **REGRESSION**.

## Baseline → Sprint 15

| Suite | Sprint AI 2 baseline | Sprint 15 |
|---|---|---|
| Backend | 494 / 0 / 0 / 1 skip | **509 / 0 / 0 / 1 skip** |
| Frontend | 47 / 0 | **48 / 0** |
| MCP | 59 / 0 | **59 / 0** |
| Security scan | 0 / 0 | **0 / 0** |
| Backend coverage | (not measured) | **75.8 %** line (81.5 % excl. generated mappers) |

**+15 backend tests** — `OrderServiceTest` (9), `ActuatorSecurityIntegrationTest`
(6). **+1 frontend test** — AI-card XSS rendering. No test removed, disabled, or
skipped (the single skip is the opt-in `RealProviderManualTest`, unchanged).

## Unit tests

### Backend — `./mvnw -o test`
```
Tests run: 509, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```
`Skipped: 1` = `RealProviderManualTest` (`@EnabledIfEnvironmentVariable(AI_REAL_PROVIDER_TEST=true)`) — intentional, never in CI.

**NEW `OrderServiceTest` (9)** — closes the OrderService coverage gap (§8 priority #1). Covers the 404 / 422 guard branches:
- `getOrThrow` unknown id → `ResourceNotFoundException(ORDER_NOT_FOUND)`
- `createOrder`: blocked customer → `BusinessRuleException(CUSTOMER_BLOCKED)` (no reservation, no save); inactive customer → `CUSTOMER_INACTIVE`; product not found → `PRODUCT_NOT_FOUND` (no reservation); inactive product → `PRODUCT_INACTIVE`
- `cancelOrder`: SHIPPED order → `ORDER_CANNOT_CANCEL` (no transition); PENDING order → transitions to `CANCELLED`
- `updateStatus`: unknown order → 404; delegates target status to `OrderStatusTransitionService`

`OrderService` line coverage 10.5 % → **34.6 %** (the happy-path reservation flow stays covered by `OrderControllerIntegrationTest`).

### Frontend — `npx vitest run`
```
Test Files  7 passed (7)
     Tests  48 passed (48)
```
**NEW** — `AiInsightsCard.test.tsx › XSS: malicious AI text is rendered as inert text, never as HTML` (§16): `<script>` / `<img onerror>` in the AI summary + insight fields + recommendation → asserts literal text, no live `<script>`/`img[onerror]` node, HTML-escaped in the DOM.

### MCP — `npm test`
```
ℹ tests 59
ℹ pass 59
ℹ fail 0
```
`npm run type-check` → exit 0. `npm run build` → exit 0. No MCP source changed.

## Integration tests

All `*IntegrationTest` classes run inside the 509-test backend suite against a
real PostgreSQL (`commerce_insight` for tests; the demo/e2e stacks use their own
DBs).

**NEW `ActuatorSecurityIntegrationTest` (6)** — §34/§35 (see `SPRINT_15_SECURITY.md` §12):
health/info public; `/actuator/metrics` → 401 anon / 403 STAFF / 200 ADMIN;
AI meters reachable by name; `/actuator/{env,beans,configprops,mappings,threaddump,heapdump}`
→ 404; unknown `/api` path → 404 enveloped (not 500).

**PRE-EXISTING, re-verified** — `SecurityHardeningIntegrationTest` 9/9 (unaffected
by the `/actuator/**` ADMIN rule), `AiAnalyticsControllerIntegrationTest` 8/8,
`AnalyticsControllerIntegrationTest`, `OrderControllerIntegrationTest`,
`CustomerControllerIntegrationTest`, all import/export/customer/inventory ITs.

## Backend coverage (JaCoCo 0.8.12 — **NEW**, report only, no gate)

```
./mvnw -o test        → target/jacoco.exec
./mvnw -o jacoco:report → target/site/jacoco/index.html  (also produced by `test`)
```

| Area | Line coverage |
|---|---|
| **Overall** | **75.8 %** (3806 / 5022) |
| Overall excl. generated `*MapperImpl` | **81.5 %** (3447 / 4231) |
| auth / security | 85.7 % |
| analytics (core) | 100 % |
| analytics.ai + llm | 94.7 % |
| customer | 95.7 % |
| product / category | 95.4 % |
| export | 100 % |
| dataimport | 70.7 % |
| order (excl. mappers) | 53.8 % (`OrderService` 34.6 %) |
| inventory (excl. mappers) | 57.6 % (`InventoryService` 46.7 %) |

**Interpretation:** business/security/analytics/AI paths are well covered.
Remaining gaps are (a) MapStruct-generated `*MapperImpl` classes (0 hand-written
logic), (b) the `OrderService`/`OrderInventoryService` happy-path reservation
machinery (covered end-to-end by `OrderControllerIntegrationTest`), (c)
`InventoryTransactionService` / `StockAdjustmentSpecification`. Per §8 no
arbitrary target is imposed; the high-value guard/conflict paths were the
addition.

## E2E — Playwright

### Security suite (Sprint 12C) — **executed, PASS**
```
docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d   (reused the Sprint-15 image, no rebuild)
E2E_BASE_URL=http://localhost:5173 npx playwright test
→ 32 passed, 2 failed  (1.1 m)
```
The **25 security tests** (`auth.spec.ts` ×10, `rbac.spec.ts` ×5,
`token-refresh.spec.ts` ×2, `rate-limit-and-features.spec.ts` ×8) **all passed**
against the `e2e` stack (`commerce_insight_e2e`, `e2e-*@commerceinsight.test`
seed users): 9.1 login, 9.2 protected routes, 9.3 RBAC, 9.4 403, 9.5 401→refresh,
9.6 concurrent 401, 9.7 refresh failure, 9.8 429, 9.9 import UX, 9.10 order UX.

### Dashboard / AI suite — **executed, PASS on the demo stack**
```
docker compose -f docker-compose.yml -f docker-compose.demo.yml up -d
E2E_ADMIN_EMAIL=demo-admin@commerceinsight.demo … npx playwright test dashboard.spec.ts
→ 9 passed (14.6 s)
```
The **2 failures** in the combined e2e-stack run above are the two dashboard tests
that assert **non-zero seeded revenue / KPIs**
(`ADMIN lands … sees non-zero KPI`, `overview API returns real seeded totals`) —
they need the 600-order Sprint 13C **demo** dataset and fail on the thin `e2e`
seed by design (the spec file documents "run against the demo stack"). On the
demo stack: **9/9**. This is an **environment mismatch, not a REGRESSION.**

### `npx playwright test --list` → 34 tests / 5 files.

## Build

| Command | Result |
|---|---|
| `./mvnw -o test` | 509 / 0 / 0 / 1 — BUILD SUCCESS |
| `./mvnw -o package` | *(covered by `mvn … verify` in CI; local `test` phase green)* |
| frontend `npx tsc --noEmit` (app + all) | exit 0 |
| frontend `npx vitest run` | 48 / 0 |
| frontend `npm run lint` | exit 0 — 0 errors, **17 pre-existing** advisory warnings (see below) |
| frontend `npm run build` | exit 0, no >500 kB warning |
| mcp `npm run type-check` / `npm test` / `npm run build` | 0 / 59-0 / 0 |
| `node scripts/security-check.mjs` | 0 / 0 |

## Lint — the 17 pre-existing warnings (§33)

`npm run lint` exits **0** (they are warnings, not errors) and each is documented
in `frontend/eslint.config.js`.

| # | Rule | Files | Class | Action |
|---|---|---|---|---|
| 4 | `react-refresh/only-export-components` | `components/ui/button.tsx`, `components/ui/form.tsx`, `providers/QueryProvider.tsx`, `providers/ThemeProvider.tsx` | **C** — the shadcn/ui + provider+hook colocation convention (each file exports a component *and* a `cva` variant / hook / `queryClient`). | Not fixed — fixing means splitting shared UI/provider primitives into extra files, an unrelated cross-cutting refactor explicitly discouraged by §33 / §3. HMR-only ergonomics, not correctness. |
| 13 | `@typescript-eslint/no-explicit-any` | 3 Recharts render-prop callbacks (`OrderStatusChart`, `PaymentMethodChart`, `RevenueTrendChart`) + RHF field adapters in `CreateOrderForm`, `OrderDetailPage`, `CreateCustomerPage`, `EditCustomerPage`, `CustomerGroupsPage`, `CategoriesPage` | **B** — legitimate exception: Recharts 2.x does not export usable payload types for its tooltip/label render props; the RHF adapters bridge loosely-typed 3rd-party callbacks. | Not fixed — `tsc --strict` already forbids *implicit* any; explicit `any` here is a deliberate escape hatch (documented in `eslint.config.js`). No functional risk. |

Sprint 15 introduced **0** new lint warnings. Target "preferably 0 warnings" not
met for these 17 — kept and documented per §47 rather than churned.

## Known limitations

- **`RealProviderManualTest`** — the 1 backend skip; opt-in only.
- **Import bulk-throughput** not benchmarked (fixtures failed validation) — import
  correctness covered by the `*ImportService` suite. See `SPRINT_15_PERFORMANCE.md` §4.
- **Dashboard KPI E2E** needs the demo dataset — 2 tests fail on the thin `e2e`
  seed (pass on the demo stack).
- **JaCoCo** = report only, no coverage gate.
- **17 advisory lint warnings** retained (table above).
- **`.env.example`** is untracked on disk (removed from Git upstream in `f2a0788`);
  it is secret-free — the user may want to `git add` it for reproducibility.
