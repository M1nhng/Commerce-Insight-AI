# Sprint 13C — Demo Data & Dashboard Validation

_Branch: `sprint13c` · Executed: 2026-08-31 · Docker + PostgreSQL 16 available_

---

## 1. Objective

The application builds and tests, but its databases hold almost no ecommerce
data, so every Dashboard / list / analytics screen is empty. Sprint 13C adds a
**dedicated, deterministic, realistic demo dataset** so the **existing** backend,
**existing** REST APIs and **existing** React frontend show meaningful numbers —
with **zero application code changes**.

```
db/demo/R__seed_demo_data.sql   →  existing Flyway  →  existing schema
      →  existing REST APIs  →  existing React dashboard  →  realistic screens
```

No fake numbers in React. No mock backend. No `/demo-data` endpoints. No
business-rule changes. **Architecture changes: NONE.**

---

## 2. Demo architecture

| Layer | What Sprint 13C adds | What is reused unchanged |
|---|---|---|
| Config | `application-demo.yml` (new Spring profile `demo`) | `application.yml` layering, `${ENV:default}` convention |
| Schema | nothing — 0 new migrations | all 31 `V*` migrations `V1`–`V31` |
| Seed | `db/demo/R__seed_demo_data.sql` (repeatable, `db/demo` location) | Flyway repeatable-migration mechanism, exactly as `db/e2e` |
| Infra | `docker-compose.demo.yml` overlay; `scripts/demo-up.sh`, `scripts/demo-reset.sh` | `docker-compose.yml` services, all three Dockerfiles/images |
| Backend | — | every controller, service, repository, security filter |
| Frontend | — | every page, hook, service, store |
| MCP | — | all 8 providers, REST-only boundary, API-key filter |

**Production safety** — the demo seed is loaded **only** by
`spring.flyway.locations: classpath:db/migration,classpath:db/demo`, and the
`db/demo` location is added **only** by `application-demo.yml`.
`application.yml` and `application-prod.yml` use `classpath:db/migration` only,
so a production JVM never sees `db/demo`, cannot resolve it, and cannot run the
seed. Activating the profile also requires an explicit
`SPRING_PROFILES_ACTIVE=demo`. Same isolation model already trusted for `db/e2e`.

---

## 3. Demo profile

`backend/src/main/resources/application-demo.yml` — layered on `application.yml`:

- `spring.datasource.url` → `…/${DB_NAME:commerce_insight_demo}` — a **separate
  database** from dev (`commerce_insight_dev`), e2e (`commerce_insight_e2e`) and
  prod (`commerce_insight`).
- `spring.flyway.locations: classpath:db/migration,classpath:db/demo`
- `spring.jpa.hibernate.ddl-auto: validate` (Hibernate never touches the schema)
- Real security stack: JWT, RBAC, rate limiting, headers, envelopes — **not**
  disabled. Only rate-limit capacities are raised so a live demo click-through
  is never throttled.
- Demo-only `JWT_SECRET` / `MCP_API_KEY` defaults (≥32 bytes, clearly not the
  dev or prod value). Overridable by env.

Activate:

```bash
SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
# or
docker compose -f docker-compose.yml -f docker-compose.demo.yml up -d
```

---

## 4. Seed mechanism

`backend/src/main/resources/db/demo/R__seed_demo_data.sql` — one repeatable
Flyway migration (`R__…`). Runs after all `V*` migrations, only under the `demo`
profile.

**Deterministic.** Every primary key is `md5('demo-<domain>-<n>')::uuid`. Every
"random-looking" value is a modulo of a deterministic hash — an in-migration
helper `demo_h(text) → bigint` wrapping `md5()`. No `random()`, no
clock-derived identifiers. The helper is `DROP`ed at the end, leaving **no demo
footprint in the schema** (verified: `SELECT count(*) FROM pg_proc WHERE
proname='demo_h'` → `0`).

**Idempotent.** Every `INSERT` is `ON CONFLICT (id) DO NOTHING`. The single
`UPDATE` (order money totals) recomputes identical values and is scoped to
`order_number LIKE 'DEMO-%'`. Verified by running the script **three times** back
to back:

| run | new rows | order UPDATE |
|---|---|---|
| 1 | full dataset inserted | 600 |
| 2 | `INSERT 0 0` on every statement | 600 (same values) |
| 3 | `INSERT 0 0` on every statement | 600 (same values) |

Row counts are identical after every run — **no uncontrolled growth**.

**Isolated.** Every demo row carries an obvious marker: `@commerceinsight.demo`
emails, `DEMO-`/`DEMO_` prefixes on codes, SKUs, order numbers, warehouse codes.
Nothing reads or mutates rows created by the running application.

---

## 5. Dataset volumes

Measured on `commerce_insight_demo` after seeding:

| Domain | Count | Method |
|---|---:|---|
| Demo users (ADMIN / MANAGER / STAFF) | 3 | literal `INSERT`, BCrypt `$2a$10$` hashes |
| Customer groups | 5 | literal `INSERT` (`DEMO_VIP`, `DEMO_WHOLESALE`, `DEMO_RETAIL`, `DEMO_CORPORATE`, `DEMO_DORMANT`) |
| Customer segments | 6 | literal `INSERT` (reference data — schema has no customer→segment FK) |
| Warehouses | 4 total | 3 new (`Hanoi`, `Ho Chi Minh`, `Da Nang`) + `Main Warehouse` from `V14` |
| Categories | 14 | 5 parents + 9 children, `parent_id` self-reference |
| Products | 80 | `generate_series` + deterministic name/price/category; 1 inactive; price tiers budget / mid / premium (25k–30M VND) |
| Inventory | 80 | one row per product (schema enforces `uq_inventory_product`), spread over the 4 warehouses |
| Customers | 200 | `generate_series`; ~82% ACTIVE, ~12% INACTIVE, ~6% BLOCKED; ~70% assigned a group |
| Customer addresses | 315 | 200 default SHIPPING + 115 BILLING |
| Orders | 600 | `generate_series`, spread over **11 months**, weighted toward recent months (power-curve recency bias) |
| Order items | 1 793 | 1–5 distinct products per order (offset 17, coprime with 80 → guaranteed distinct) |
| Payments | 600 | one per order, `amount` mirrors `orders.total`, method mix CASH / BANK_TRANSFER / CARD / OTHER |
| Order status history | 1 171 | creation row per order + one condensed transition row for non-PENDING orders |
| Order addresses | 1 200 | SHIPPING + BILLING snapshot per order |
| Import jobs | 20 | 12 COMPLETED, 5 PARTIAL_SUCCESS, 3 FAILED; CSV/XLSX; PRODUCT/CUSTOMER/ORDER |
| Import errors | 64 | for the 8 PARTIAL/FAILED jobs (≤8 per job) |
| Login history | 40 | demo users only, ~85% success, deterministic timestamps |
| Audit logs | 24 | demo users, deterministic business-event actions |

Seed execution time: **~0.23 s** (Flyway log: `Successfully applied 1 migration
… (execution time 00:00.227s)`). Backend startup on the seeded DB: **6.1 s**.

---

## 6. Demo users

| Role | Email | Status |
|---|---|---|
| ADMIN | `demo-admin@commerceinsight.demo` | enabled, unlocked |
| MANAGER | `demo-manager@commerceinsight.demo` | enabled, unlocked |
| STAFF | `demo-staff@commerceinsight.demo` | enabled, unlocked |

Roles are the existing `ADMIN` / `MANAGER` / `STAFF`. Passwords are hashed with
the existing `BCryptPasswordEncoder` (`$2a$10$`, verified with `checkpw`). These
are **separate** from the `db/e2e` users (`e2e-*@commerceinsight.test`) — the two
seeds live in different databases and never collide.

## 7. Demo credentials

> **DEMO ONLY — NEVER USE IN PRODUCTION.** These strings exist only in this
> development document and (as BCrypt hashes) in the demo-only seed file.

| Role | Email | Password |
|---|---|---|
| ADMIN | `demo-admin@commerceinsight.demo` | `DemoAdmin!2024` |
| MANAGER | `demo-manager@commerceinsight.demo` | `DemoManager!2024` |
| STAFF | `demo-staff@commerceinsight.demo` | `DemoStaff!2024` |

No password (plaintext or hash) appears in any frontend source file.

---

## 8. Reset procedure

The demo database is disposable. To wipe and rebuild:

```bash
# Docker stack — targets ONLY the demo overlay + the cia-postgres-demo-data volume
./scripts/demo-reset.sh
# equivalently:
docker compose -f docker-compose.yml -f docker-compose.demo.yml down -v
docker compose -f docker-compose.yml -f docker-compose.demo.yml up -d --build
```

`demo-reset.sh` can only ever touch `docker-compose.demo.yml` +
`cia-postgres-demo-data`. Dev, e2e and prod live on different
databases/volumes and are unreachable from it.

Local (no Docker): `DROP DATABASE commerce_insight_demo; CREATE DATABASE
commerce_insight_demo;` then re-run the backend with the `demo` profile — Flyway
re-applies everything.

---

## 9. Start commands

```bash
# Full stack (Postgres + Spring Boot + React + MCP), demo profile, auto-seeded:
./scripts/demo-up.sh --build
#   Frontend  http://localhost:5173      (log in as demo-admin@commerceinsight.demo)
#   API       http://localhost:8080/api/v1
#   MCP       http://localhost:3001/health

# Backend only, against a local Postgres:
createdb commerce_insight_demo   # or: docker exec cia-postgres createdb -U postgres commerce_insight_demo
cd backend && DB_NAME=commerce_insight_demo ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

---

## 10. Dashboard validation checklist

Validated against the real backend (`spring-boot:run`, `demo` profile) on the
seeded `commerce_insight_demo`. `PASS` = the existing API returned real seeded
data; browser rendering marked `NOT RUN` where no browser was available.

| Screen | Backing API | Result |
|---|---|---|
| Products list | `GET /api/v1/products` | **PASS** — `totalElements: 80`, real names (`Zephyr Edge Notebook Set`), prices, `categoryName` |
| Product detail | `GET /api/v1/products/{id}` | **PASS** — full DTO |
| Categories | `GET /api/v1/categories` | **PASS** — 14 |
| Customers list | `GET /api/v1/customers` | **PASS** — `totalElements: 200`, names + status (`Nga Lam ACTIVE`) |
| Customer groups | `GET /api/v1/customers` (group filter) / groups endpoint | **PASS** — 5 groups, ~70% of customers grouped |
| Orders list | `GET /api/v1/orders` | **PASS** — `totalElements: 600`, e.g. `DEMO-202608-00394 CONFIRMED 81795600.0` |
| Order detail (customer + items + status) | `GET /api/v1/orders/{id}` | **PASS** — items, payment, status history, addresses present |
| Warehouses | `GET /api/v1/warehouses` | **PASS** — 4 |
| Inventory | `GET /api/v1/inventory` | **PASS** — `totalElements: 80` |
| Low-stock | `GET /api/v1/inventory/low-stock` | **PASS** — non-empty list (9 low + 4 zero) |
| Import history | `GET /api/v1/import/jobs` | **PASS** — 20 jobs, statuses `FAILED` / `COMPLETED` / `PARTIAL_SUCCESS` |
| Import errors | `GET /api/v1/import/jobs/{id}/errors` | **PASS** — 64 error rows across the 8 non-clean jobs |
| **Analytics / Dashboard page** | `GET /api/v1/analytics/*` (6 endpoints) | **FAIL — pre-existing backend bug**, see §15. Underlying data verified correct (§11). |
| Browser rendering of every page | — | **NOT RUN** — no browser in this environment; the demo Docker stack (`scripts/demo-up.sh`) serves the identical frontend image on `:5173` for manual verification |

---

## 11. API validation

Login (`POST /api/v1/auth/login` as `demo-admin`) → `200`, JWT issued (len 397).
All calls below made with that bearer token.

| Endpoint | HTTP | Payload evidence |
|---|---|---|
| `GET /api/v1/products?page=0&size=1` | 200 | `data.totalElements = 80`, `data.totalPages = 80` |
| `GET /api/v1/customers?page=0&size=1` | 200 | `data.totalElements = 200` |
| `GET /api/v1/orders?page=0&size=1` | 200 | `data.totalElements = 600`, sample `DEMO-202608-00394` |
| `GET /api/v1/inventory?page=0&size=1` | 200 | `data.totalElements = 80` |
| `GET /api/v1/categories?size=100` | 200 | `data.totalElements = 14` |
| `GET /api/v1/warehouses?size=20` | 200 | `data.totalElements = 4` |
| `GET /api/v1/inventory/low-stock` | 200 | non-empty array |
| `GET /api/v1/import/jobs?size=5` | 200 | `data.totalElements = 20`, mixed statuses |
| `GET /api/v1/analytics/overview` (+ 5 more) | **500** | `INTERNAL_ERROR` — pre-existing NULL-bind bug (§15) |

Responses use the standard `ApiResponse` envelope (`success`/`data`/`message`/
`timestamp`) and `PageResponse` pagination — unchanged.

**Analytics data proven correct at the DB layer** by running the exact SQL from
`AnalyticsRepository` directly against the seeded DB with literal bounds:

| Analytics query | Result on demo data |
|---|---|
| Overview | revenue **10,801,219,074 VND**, 600 orders, 192 unique customers, 3 694 products sold, 52 cancelled |
| Revenue by month | 11 points, clear upward trend: `2025-10` 799 M / 28 orders → `2026-08` 1 996 M / 100 orders |
| Orders by status | COMPLETED 355, CANCELLED 52, PROCESSING 44, CONFIRMED 40, SHIPPED 31, DELIVERED 30, PENDING 29, REFUNDED 19 |
| Top products by revenue | `Nova Air Notebook Set` 1.31 B, `Nimbus Neo Notebook Set` 1.18 B, `Onyx Lite Smartphone` 1.05 B, … |
| Payments by method | CARD 4.19 B / 178, CASH 3.42 B / 167, BANK_TRANSFER 3.16 B / 168, OTHER 2.21 B / 87 |

Revenue-eligible statuses are `CONFIRMED, PROCESSING, SHIPPED, DELIVERED,
COMPLETED` (from `AnalyticsRepository` Javadoc) — the seed's status mix is tuned
so 500 of 600 orders (83%) are revenue-eligible.

---

## 12. MCP validation

- `npm run type-check` → **PASS**
- `npm test` (`node:test`) → **PASS**, `tests 49 / pass 49 / fail 0`
- `npm run build` → **PASS**
- Runtime (demo Docker stack): MCP `/health` → `{"status":"UP","transport":"stdio"}`,
  log `Initializing 8 provider(s)… 8 providers initialized successfully`.
  (The Sprint prompt says "7 providers"; the live count is **8** — the
  `ExportToolsProvider` was added in Sprint 11C. Recorded as an expected
  pre-existing discrepancy, not a regression.)
- MCP reaches the backend over REST only (`BACKEND_API_URL=http://backend:8080/api/v1`);
  no DB driver, no ORM, no LLM SDK (confirmed by `scripts/security-check.mjs`).
- Read-only tools (`customer_*`, `product_*`, `inventory_*`, `order_*`,
  `import_*`, `export_*`, `analytics_*`) now query a backend backed by 200
  customers / 80 products / 600 orders instead of an empty DB. `analytics_*`
  tools inherit the §15 backend bug.

No MCP source was changed in Sprint 13C.

---

## 13. Security notes

- `node scripts/security-check.mjs` → **0 errors / 0 warnings**.
- No production secrets introduced. `application-demo.yml` and
  `docker-compose.demo.yml` carry only clearly-labelled **demo-only** JWT / MCP
  values (same pattern as the accepted `application-e2e.yml` /
  `docker-compose.e2e.yml`).
- No JWT literal, no MCP API key, no password in frontend source.
- Demo passwords live only in this dev doc (plaintext) and the demo-only seed
  file (BCrypt hash).
- The `demo` profile cannot self-activate: it requires an explicit
  `SPRING_PROFILES_ACTIVE=demo`, and even then only adds a Flyway location that
  production configs do not reference.
- `SecretsValidator` (Sprint 12A) is untouched and still fails a `prod` boot
  that carries a dev/demo secret.

---

## 14. Production safety

Demo data **cannot** automatically enter production:

1. `application.yml` / `application-prod.yml` → `spring.flyway.locations:
   classpath:db/migration` only. The string `db/demo` appears **nowhere** in
   any non-demo config.
2. `spring.flyway.fail-on-missing-locations: true` means a stray `db/demo`
   reference in prod would fail fast, not silently seed.
3. The seed file's own guard: deterministic `md5('demo-…')` IDs +
   `ON CONFLICT DO NOTHING` — even if it somehow ran twice it cannot grow or
   overwrite.
4. The `demo` profile uses a **different database name**
   (`commerce_insight_demo`); pointing it at a prod DB requires deliberately
   overriding `DB_NAME`.
5. `docker-compose.demo.yml` uses its own volume `cia-postgres-demo-data`.

---

## 15. Known limitations

### Pre-existing (NOT introduced by Sprint 13C)

- **Analytics endpoints return HTTP 500** — `GET /api/v1/analytics/{overview,
  revenue,orders,products/top,customers,payments}`.
  Root cause: every method in `AnalyticsRepository` is a **native** query using
  `(:dateFrom IS NULL OR o.created_at >= :dateFrom)`. PostgreSQL cannot infer the
  type of the bind parameter in `$1 IS NULL` when the statement is prepared,
  producing `ERROR: could not determine data type of parameter $1`. This fails
  on **any** data (empty or seeded) and is independent of Sprint 13C.
  Tracked previously as `analytics-null-bind-sql-bug`. Per the Sprint 13C brief
  (§16 "do not rewrite unrelated Analytics architecture — document separately"),
  it is **not fixed here**.
  **Recommended minimal fix for a future sprint** (semantics-neutral, one file):
  in `AnalyticsRepository.java`, cast each nullable bound parameter, e.g.
  `(CAST(:dateFrom AS timestamptz) IS NULL OR o.created_at >= CAST(:dateFrom AS timestamptz))`
  in all 6 queries. No business logic, DTO, or API contract changes.
- Frontend production bundle > 500 kB (chunk-size advisory) — perf only.
- `POST /api/v1/orders/{unknown}` returns 500 not 404; one incomplete customer
  address test (`@Disabled` from Sprint 13A) — unrelated to demo data.

### Sprint 13C scope decisions

- **Inventory ↔ historical orders are not individually reconciled.** Orders are
  seeded by SQL (bypassing `OrderService`/`InventoryService`), so no
  `inventory_transactions` rows are written for demo orders and stock levels are
  not the arithmetic result of replaying 600 orders. Instead, inventory carries
  an independent, internally-consistent snapshot (healthy / low / zero /
  overstock; `reserved ≤ quantity`; `available ≥ 0` — verified 0 violations).
  Reconciling would require running the service layer 600× and is outside a
  "demo data" sprint. This matches how `db/e2e/R__seed_e2e_commerce.sql` works.
- **Order status history is condensed** to ≤2 rows/order (creation +
  destination) rather than a full PENDING→…→COMPLETED chain — enough to populate
  the Order-detail timeline without 4×–5× the row count.
- **`customer_segments` has no membership** — the schema has no customer→segment
  FK (V21: "foundation … for future sprints"), so segments are seeded as
  reference rows only.

### Environment limitations

- Browser rendering of the dashboards was **NOT RUN** (no browser). The demo
  Docker stack serves the unchanged frontend image on `:5173` for manual
  checking.
- Playwright E2E was **NOT re-run** for 13C — no frontend/e2e/backend source
  changed; the last full run (Sprint 13B) was 25/25 green. `npx playwright test
  --list` → 25 tests, OK.

---

## 16. Manual demo checklist

1. `./scripts/demo-up.sh --build` — wait for `✓ backend healthy`.
2. Open `http://localhost:5173`, log in as `demo-admin@commerceinsight.demo` /
   `DemoAdmin!2024`.
3. **Products** (`/products`) — 80 products, 80 pages, category names, price
   spread. Open one for the detail view.
4. **Categories** (`/categories`) — 14, two levels.
5. **Customers** (`/customers`) — 200, filter by status (ACTIVE / INACTIVE /
   BLOCKED) and by group. Open a customer → addresses + group.
6. **Customer groups** (`/customers/groups`) — 5 groups.
7. **Inventory** (`/inventory`) — 80 rows across 4 warehouses; low-stock view
   shows the 9 low + 4 zero.
8. **Warehouses** (`/warehouses`) — 4.
9. **Orders** (`/orders`) — 600, filter by status and date. Open an order →
   customer, line items, payment, status timeline, shipping/billing address.
10. **Import** (`/import/jobs`) — 20 jobs; open a `FAILED` / `PARTIAL_SUCCESS`
    job → per-row errors.
11. **Analytics** (`/analytics`) — currently shows the safe error envelope
    (§15 pre-existing bug). After the one-line `CAST(...)` fix it renders:
    ~10.8 B VND revenue, 600 orders, upward 11-month trend, status donut, top
    products, payment-method split.
12. `./scripts/demo-reset.sh` returns to a clean seeded state.
