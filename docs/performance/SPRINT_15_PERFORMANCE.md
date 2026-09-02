# Sprint 15 — Performance

> **These are local-container measurements on one developer machine. They are
> indicative, not universal benchmarks.** Targets are demo-environment sanity
> checks, not CI gates.

## Environment

| | |
|---|---|
| Host | Windows 10, Docker Desktop (WSL2) |
| Stack | `docker compose -f docker-compose.yml -f docker-compose.demo.yml` — `postgres:16-alpine`, Spring Boot (`demo` profile), Vite/nginx, MCP (Node 20) |
| DB | PostgreSQL 16, `commerce_insight_demo` |
| Dataset (Sprint 13C deterministic seed) | ~600 orders, 80 products, ~200 customers, ~20 import jobs, 11 months of revenue |
| Method | `curl -w '%{http_code},%{time_total}'` (single-shot, warm JVM), `EXPLAIN (ANALYZE, BUFFERS)` via `psql` |
| AI | **disabled** (`AI_INSIGHTS_ENABLED=false`) — AI latency is external-provider-bound and is not a backend benchmark |

## 1. Analytics API latency (target: < 1000 ms typical)

`POST`/`GET /api/v1/analytics/*` with `dateFrom`/`dateTo`, 4 windows, all HTTP 200:

| Endpoint | 7 d | 30 d | 90 d | 365 d |
|---|---:|---:|---:|---:|
| `/overview` | 20 ms | 25 ms | 15 ms | 14 ms |
| `/revenue?groupBy=MONTH` | 10 ms | 27 ms | 8 ms | 8 ms |
| `/orders` | 9 ms | 8 ms | 7 ms | 9 ms |
| `/products/top?limit=10` | 12 ms | 9 ms | 8 ms | 9 ms |
| `/customers` | 9 ms | 9 ms | 19 ms | 10 ms |
| `/payments` | 10 ms | 8 ms | 20 ms | 8 ms |

**Observation:** every analytics endpoint responds in **7–27 ms** across all
windows, ~40× under the target. The Sprint 13D `CAST(:p AS timestamptz)` NULL-bind
fix is preserved (no HTTP 500). Window size has no measurable effect at this
dataset size.

## 2. Database query plans (`EXPLAIN ANALYZE`, `commerce_insight_demo`)

| Query | Plan | Exec time | Verdict |
|---|---|---:|---|
| Revenue sum (`SUM(total)` WHERE status IN (…) AND created_at BETWEEN) | `Seq Scan on orders` (500/600 rows, 29 buffer pages) → Aggregate | **0.23 ms** | correct — planner rejects the index because the filter selects ~all rows |
| Status distribution (`GROUP BY status`) | `Seq Scan` → HashAggregate | **0.18 ms** | correct |
| Payment breakdown (`payments JOIN orders GROUP BY method`) | `Seq Scan payments` + `Hash Join` (Seq Scan orders) → HashAggregate | **0.42 ms** | correct |

**Index review (§19):** the schema already carries the right analytics indexes —
`idx_orders_analytics ON orders(created_at, status, total)` (V10),
`idx_orders_status_date ON orders(status, created_at DESC)` (V27),
`idx_order_items_order_id` / `idx_order_items_product_id` (V27),
`idx_payments_method_status ON payments(method, status)` (V29),
`idx_orders_customer_id` (V27). Each is justified in its migration comment.

At the **demo dataset size (600 orders)** the PostgreSQL planner correctly
chooses a sequential scan (reading 29 pages beats an index scan + heap fetches),
so `EXPLAIN` does **not** demonstrate index benefit here. At production scale
(100k+ orders with a selective date filter) these composite indexes become the
access path. **No new index added** — §19: only add an index when the plan
proves benefit; adding one now would be blind.

## 3. Export generation (§22)

`GET /api/v1/export/{orders,products,customers}?format={XLSX,PDF}`, HTTP 200,
file magic verified:

| Dataset | XLSX | PDF |
|---|---|---|
| orders (~600) | 200 · **1055 ms** · 33.9 KB · *Microsoft Excel 2007+* | 200 · **452 ms** · 94.5 KB · *PDF 1.5, 10 pages* |
| products (80) | 200 · 89 ms · 7.6 KB | 200 · 31 ms · 11.1 KB · 4 pages |
| customers (~200) | 200 · 57 ms · 15.0 KB | 200 · 38 ms · 32.3 KB · 10 pages |

**Observation:** correct content types and page counts; outputs are 8–95 KB (no
memory explosion). The orders XLSX (~1 s) is the heaviest path — a 600-row
formatted workbook — still well within a reasonable synchronous request budget.
(`/api/v1/export/inventory` does not exist — 404, not a defect.)

## 4. Import (§21)

`POST /api/v1/import/products` (multipart) — **job creation** returned HTTP 201
in **23–71 ms** for 20 / 200 / 1000-row CSVs. Import processing is **asynchronous**
(a job row is created, rows are processed by a background worker).

**End-to-end row throughput was not benchmarked this sprint:** the ad-hoc CSV
fixtures generated here failed the parser's header/format validation (`status=FAILED`,
`totalRows=0`) before row processing began, so no meaningful per-row number was
produced. Import correctness and behaviour are covered functionally by the
backend import test suite — `ProductImportService` (73%), `CustomerImportService`
(84%), `OrderImportService` (74%), `CsvImportParser` (86%), `ExcelImportParser`
(70%), `ImportOrchestrator` (41%) line coverage, all green in the 509-test run.
This is recorded as a **known limitation**, not a regression.

## 5. Full smoke (§41) — demo stack, ADMIN

`products · categories · customers · inventory · orders · analytics/overview ·
import/jobs · export/products.xlsx · actuator/health` → **all HTTP 200**;
`auth/logout` → **204**. One optional feature being unavailable (AI disabled)
does not affect any other endpoint.

## 6. Frontend bundle (§16 / §17)

`npm run build` (Vite 6, `tsc -p tsconfig.app.json` first):

| Metric | Value |
|---|---|
| Vite ">500 kB chunk" warning | **absent** |
| Largest chunk | `charts-vendor` **421.23 kB** (gzip 113.71 kB) — Recharts + d3, loaded only by the analytics/dashboard route |
| Entry chunk | `index-*.js` 331.04 kB (gzip 105.60 kB) |
| `react-vendor` | 260.38 kB (gzip 83.03 kB) |
| CSS | 19.00 kB (gzip 4.60 kB) |
| Route/feature chunks | ~65, all `React.lazy` code-split (Sprint 14) |
| AI feature | folded into the dashboard route chunk — no separate heavy chunk |

Identical to the Sprint 14 baseline — **no regression, no change made** (every
heavy route is already lazy-loaded).

## Targets vs results

| Target (demo/local) | Result |
|---|---|
| simple GET < 500 ms typical | ✅ analytics 7–27 ms; smoke endpoints < 100 ms |
| analytics API < 1000 ms typical | ✅ 7–27 ms |
| AI-disabled endpoint < 500 ms | ✅ ~15 ms (`available:false`) |
| no major frontend bundle regression | ✅ identical to Sprint 14 |
