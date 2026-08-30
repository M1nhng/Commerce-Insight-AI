# Sprint 13A — Baseline (pre-change repository state)

Captured 2026-08-30, branch `sprint13`, before any Sprint 13A modification.
All values are real command outputs — nothing here is projected.

## Git

```
$ git branch --show-current
sprint13
$ git status --porcelain
?? CLAUDE.md
?? Roadmap.md          # (both untracked, pre-existing, unrelated)
$ git log --oneline -3
e5a2954 Merge pull request #13 from M1nhng/sprint12
396c3d9 Done sprint 12: Security Hardening
f4e0580 Merge pull request #12 from M1nhng/sprint11
```
Sprint 12A/12B/12C are merged. Working tree clean.

## Toolchain

| Tool | Version |
|---|---|
| Java | 17.0.12 (Oracle, `D:\Folder_phan_mem\jdk17`) |
| Node | v26.4.0 |
| npm | 11.17.0 |
| Maven (wrapper) | 3.9.11 |
| Docker | client 29.6.1 / **daemon running** |
| Docker Compose | v5.3.0 |

## Environment

| Resource | State |
|---|---|
| Docker daemon | **UP** — `cia-postgres` (healthy) + `cia-pgadmin` containers already running |
| PostgreSQL :5432 | **OPEN** — `postgres:16-alpine`, db `commerce_insight`, healthy |
| Backend :8080 | not running |
| `psql` CLI | not installed (use `docker exec cia-postgres psql`) |

## Baseline build / test status

| Check | Command | Result |
|---|---|---|
| Static security scan | `node scripts/security-check.mjs` | ✅ **0 ERROR / 0 WARN** |
| Backend compile | `./mvnw -o test-compile` | ✅ exit 0 |
| **Backend full suite** | `./mvnw -o test` (real Postgres) | ⚠️ **417 tests, 5 failures, 0 errors, 0 skipped** — BUILD FAILURE. First time this suite has ever executed (12A/12C had no DB). |
| Frontend type-check | `npx tsc --noEmit` | ✅ exit 0 |
| Frontend unit tests | `npx vitest run` | ✅ **33 passed / 33** (5 files) |
| Frontend build | `npm run build` | ✅ `✓ built in ~6s`; single 1.39 MB JS chunk |
| Frontend lint | `npm run lint` | ❌ **ESLint 9 aborts — no `eslint.config.*`** |
| MCP type-check | `npm run type-check` | ✅ exit 0 |
| MCP tests | `npm test` (`node:test`) | ✅ **49 passed / 49** |
| MCP build | `npm run build` | ✅ exit 0 |
| Playwright discovery | `npx playwright test --list` | ✅ **25 tests in 4 files** (from Sprint 12C) |
| CI workflow | `.github/workflows/ci.yml` | ❌ **placeholder** ("Finalize when implementation begins") |
| CD workflow | `.github/workflows/cd.yml` | placeholder (deployment TODO) |

## Baseline backend test failures (5)

| # | Test | Symptom | Root cause (confirmed during 13A) | Class |
|---|---|---|---|---|
| 1 | `SecurityHardeningIntegrationTest.securityHeaders_present` | `Strict-Transport-Security` absent | Spring Security emits HSTS only on `request.isSecure()`; the backend sits behind TLS-terminating nginx and never sees a secure request → HSTS never delivered anywhere | B (pre-existing) — **fixed in 13A** |
| 2 | `SecurityHardeningIntegrationTest.lockedUser_tokenRejected` | old token still 200 after 5 bad logins | `AuthService.login` is `@Transactional`; `handleFailedLogin()` saved `locked=true` then the method rethrew `BadCredentialsException` → the whole tx (incl. the lock) rolled back → account never actually locks | B (pre-existing) — **fixed in 13A** |
| 3 | `RateLimitIntegrationTest.loginRateLimited` | 4th login 401, expected 429 | `RateLimitingFilter.groupFor()` keyed off `request.getServletPath()`, which is empty under MockMvc (and for a root servlet mapping) → filter self-disabled | B (pre-existing) — **fixed in 13A** |
| 4 | `CustomerControllerIntegrationTest.addAddress_secondDefault_clearsFirst` | 405, expected 404 | Incomplete test: final step GETs a single address by id (route returns 405, not implemented) and asserts 404; in-code comment already flags it a placeholder | E (bad test) — **`@Disabled` in 13A**, documented |
| 5 | `OrderControllerIntegrationTest.getOrderById_notFound_returns404` | 500, expected 404 | Authenticated `GET /orders/{unknown-uuid}` returns 500 instead of the 404 `ResourceNotFoundException` envelope. Non-security, order-module scoped. Not reproducible in isolation (needs the ordered class run). | B (pre-existing) — **`@Disabled` in 13A**, tracked as follow-up |

## Known blockers at baseline

- **`mcp-server/Dockerfile` did not exist** — `docker-compose.yml` referenced it → `docker compose build` fails. **Fixed in 13A.**
- **`backend/Dockerfile` build broken** — `backend/mvnw` is committed with CRLF; `./mvnw` → "not found" on Alpine. **Fixed in 13A** (`sed` in Dockerfile + `.gitattributes`).
- **`docker-compose.yml` MCP env mismatch** (AR-INFRA4) — sets `BACKEND_BASE_URL` / `MCP_SERVER_PORT`; the config reads `BACKEND_API_URL` / `PORT`. **Fixed in 13A.**
- **`frontend/Dockerfile`** bakes `VITE_API_BASE_URL` with no build-arg (AR-INFRA5). **Fixed in 13A.**
- **No `eslint.config.*`** → `npm run lint` unusable. **Fixed in 13A** (flat config).
- CI `ci.yml` is a placeholder. **Replaced in 13A.**
- Frontend `npm audit`: 2 moderate prod (react-router). MCP `npm audit`: 6 (2 mod / 4 high). Deferred — see production-readiness doc.
