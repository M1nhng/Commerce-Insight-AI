# Demo Guide — Commerce Insight AI

How to run the full stack locally with a large, realistic, deterministic dataset
and click through every feature. Current as of Sprint 16.

> The dataset seeding, isolation and volume details live in
> [`SPRINT_13C_DEMO_DATA.md`](SPRINT_13C_DEMO_DATA.md) — this guide is the
> operator's walkthrough.

---

## 1. Prerequisites

- **Docker + Docker Compose v2** (`docker compose version` → 2.x)
- ~4 GB free RAM for the four containers
- Free TCP ports: `5173` (frontend), `8080` (backend), `3001` (MCP), `5432` (Postgres)
- Linux / macOS / WSL / Git Bash for `scripts/*.sh` — Windows users can run the
  Compose commands directly (see §4)

Java, Node and Maven are **not** needed — every image builds inside Docker.

---

## 2. Start the demo

```bash
./scripts/demo-up.sh
```

What it does:

1. `docker compose -f docker-compose.yml -f docker-compose.demo.yml up -d`
   (add `--build` the first time, or run `./scripts/demo-up.sh --build`)
2. Waits (up to ~5 min) for `GET http://localhost:8080/actuator/health` to report
   `"status":"UP"` — first boot runs 31 Flyway migrations **plus** the demo seed
3. Prints the URLs and the login line

First run builds three images and can take several minutes. Subsequent runs start
in well under a minute.

---

## 3. Verify health

```bash
docker compose -f docker-compose.yml -f docker-compose.demo.yml ps
```

Expect four services, all `running` / `healthy`:
`cia-postgres`, `cia-backend`, `cia-frontend`, `cia-mcp-server`.

Manual checks:

```bash
curl -s http://localhost:8080/actuator/health          # {"status":"UP"}
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:5173/   # 200
curl -s http://localhost:3001/health                   # {"status":"UP",...}
```

---

## 4. Windows (no Git Bash)

```powershell
docker compose -f docker-compose.yml -f docker-compose.demo.yml up -d --build
docker compose -f docker-compose.yml -f docker-compose.demo.yml ps
# ... use the app ...
docker compose -f docker-compose.yml -f docker-compose.demo.yml down -v   # reset
```

---

## 5. URLs

| Service | URL |
|---|---|
| Frontend | http://localhost:5173 |
| API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Backend health | http://localhost:8080/actuator/health |
| MCP health | http://localhost:3001/health |

---

## 6. Demo users

> **DEMO ONLY — never reuse these credentials in production.** They exist only as
> BCrypt hashes in the demo-only seed and as plaintext in the docs. No password
> (plaintext or hash) appears in any frontend source file.

| Role | Email | Password |
|---|---|---|
| ADMIN | `demo-admin@commerceinsight.demo` | `DemoAdmin!2024` |
| MANAGER | `demo-manager@commerceinsight.demo` | `DemoManager!2024` |
| STAFF | `demo-staff@commerceinsight.demo` | `DemoStaff!2024` |

### Role differences

- **ADMIN** — everything, including user management (`/admin`), stock-adjustment
  approval, and `DELETE` on every domain.
- **MANAGER** — create/update across products, categories, customers, customer
  groups, orders, inventory; cannot reach ADMIN-only screens or approvals.
- **STAFF** — read across the catalogue and operations; order/inventory
  operational actions; no customer-group management, no deletes, no admin.

A wrong-role navigation shows a "Permission denied" page and keeps the session —
it does **not** log you out.

---

## 7. Deterministic dataset

After a fresh seed the demo database contains (see
[`SPRINT_13C_DEMO_DATA.md`](SPRINT_13C_DEMO_DATA.md) §5 for the full table):

| Domain | Count |
|---|---:|
| Demo users (ADMIN / MANAGER / STAFF) | 3 |
| Customer groups | 5 |
| Customer segments (reference only) | 6 |
| Warehouses | 4 |
| Categories (2 levels) | 14 |
| Products (1 inactive) | 80 |
| Inventory rows | 80 |
| Customers (`ACTIVE` / `INACTIVE` / `BLOCKED`) | 200 |
| Orders (11 months, recency-weighted) | 600 |
| Order items | 1,793 |
| Payments | 600 |
| Import jobs (`COMPLETED` / `PARTIAL_SUCCESS` / `FAILED`) | 20 |

Every id is `md5('demo-<domain>-<n>')::uuid`; every "random" choice is a modulo of
a deterministic hash. No `random()`, no clock-derived ids — the same dataset every
time.

---

## 8. Recommended demo flow

1. **Login** — `http://localhost:5173`, ADMIN credentials.
2. **Dashboard** (`/dashboard`) — revenue trend, order-status donut, top products,
   payment-method split, KPI tiles. The **AI Business Insights** card shows
   "temporarily unavailable" unless a key is configured (§9).
3. **Products** (`/products`) — 80 products, category names, price tiers; open one
   for the detail view.
4. **Categories** (`/categories`) — 14, two levels (5 parents + 9 children).
5. **Customers** (`/customers`) — 200; filter by status and by group; open a
   customer for addresses + group. **Customer groups** at `/customers/groups` (5).
6. **Inventory** (`/inventory`) — 80 rows across 4 warehouses; the low-stock view
   shows the seeded low + zero-stock items. **Warehouses** at `/warehouses` (4).
7. **Orders** (`/orders`) — 600; filter by status and date; open one for customer,
   line items, payment, status timeline, shipping/billing snapshot. Try
   `/orders/new` to build an order from line items (MANAGER/ADMIN).
8. **Analytics** (`/analytics`) — the full analytics page: ~10.8 B VND revenue,
   600 orders, an upward 11-month trend, status/product/payment breakdowns. All
   endpoints respond in well under 30 ms on this dataset.
9. **AI Insights** (`/dashboard` card, or the analytics page) — with AI disabled
   it degrades gracefully; with a key it returns a structured summary + insights +
   recommendations (§9).
10. **Import** (`/import/jobs`) — 20 jobs; open a `FAILED` / `PARTIAL_SUCCESS` job
    to see per-row errors. Download a template from `/import`.
11. **Export** (`/export`) — export products / customers / orders / analytics as
    XLSX or PDF; the file downloads through the browser.
12. **Logout** — clears the session, returns to `/login`.

---

## 9. Enabling AI insights (optional)

The demo runs fine **without** this. To see a live AI response, supply a key to
the backend container and restart it:

```bash
# edit docker-compose.demo.yml backend.environment, or pass -e on an up:
#   AI_INSIGHTS_ENABLED=true
#   AI_PROVIDER=openai
#   AI_MODEL=gpt-4o-mini
#   OPENAI_API_KEY=sk-...            # never commit this
docker compose -f docker-compose.yml -f docker-compose.demo.yml up -d backend
```

Local, no external key — point at Ollama:

```
AI_INSIGHTS_ENABLED=true
AI_PROVIDER=openai
AI_BASE_URL=http://host.docker.internal:11434/v1
AI_MODEL=llama3.1
AI_API_KEY=
```

Anthropic:

```
AI_INSIGHTS_ENABLED=true
AI_PROVIDER=anthropic
AI_MODEL=claude-3-5-haiku-latest
ANTHROPIC_API_KEY=...
```

With AI misconfigured or the provider unreachable, the card just shows
"temporarily unavailable" — nothing else on the dashboard is affected.

---

## 10. Reset the demo

```bash
./scripts/demo-reset.sh              # down -v (demo only) + rebuild + reseed
./scripts/demo-reset.sh --keep-images  # skip the image rebuild (faster)
```

`demo-reset.sh` targets **only** the `docker-compose.demo.yml` overlay and the
`cia-postgres-demo-data` volume. It cannot touch `dev` (`commerce_insight_dev`),
`e2e` (`commerce_insight_e2e`) or `prod` (`commerce_insight`) — those live on
different databases and volumes.

Local, no Docker: `DROP DATABASE commerce_insight_demo; CREATE DATABASE
commerce_insight_demo;` then rerun the backend with `-Dspring-boot.run.profiles=demo`.

After a reset the row counts in §7 are reproduced exactly.

---

## 11. Troubleshooting

| Symptom | Fix |
|---|---|
| `demo-up.sh` times out waiting for backend | First boot is slow (migrations + seed). Check `docker compose ... logs backend`. Re-run — the DB volume persists. |
| Port already in use (`5173` / `8080` / `3001` / `5432`) | Stop the conflicting process, or override the port env vars (`FRONTEND_PORT`, `BACKEND_PORT`, `MCP_PORT`, `DB_PORT`) before `up`. |
| Frontend loads but every API call fails | The frontend image bakes `VITE_API_BASE_URL` at build time; the demo overlay expects the browser to reach the API at `http://localhost:8080`. Rebuild the frontend if you changed that. |
| Analytics page shows an error envelope | Should not happen post-Sprint 13D. If it does, confirm the backend is the current image (`docker compose ... build backend`). |
| Login returns 429 | Rate limiting is on (real security path); the demo raises the caps high, but a script hammering `/auth/login` can still trip it. Wait for `Retry-After`. |
| AI card always "temporarily unavailable" | Expected unless you set `AI_INSIGHTS_ENABLED=true` + a key (§9). |
| Want a clean slate | `./scripts/demo-reset.sh`. |

---

## 12. Shut down

```bash
# stop, keep data
docker compose -f docker-compose.yml -f docker-compose.demo.yml stop

# stop and remove containers, keep the seeded volume
docker compose -f docker-compose.yml -f docker-compose.demo.yml down

# stop and wipe the seeded volume (next up re-seeds)
docker compose -f docker-compose.yml -f docker-compose.demo.yml down -v
```
