# Sprint 12C — End-to-End Security Verification & Production Readiness

Companion to `SPRINT_12A_SECURITY_AUDIT.md` (backend) and
`SPRINT_12B_FRONTEND_SECURITY.md` (frontend). Sprint 12C is **verification +
infrastructure**: no auth rewrite, no business-logic change, no new
architecture.

Date of this run: 2026-08-30. Executor environment: Windows, **Docker Desktop
not running**, no local Postgres on :5432.

---

## 1. Scope

- Make the 12A + 12B security flow executable as an integrated stack.
- Provide a reproducible Docker E2E environment (`e2e` profile, seed users,
  low rate limits).
- Add a Playwright E2E suite for the critical security flows (§9.1–9.10).
- Add a zero-dependency static security scanner + a CI-style verify script.
- Run everything that does **not** need the integrated stack; give exact
  commands for the rest.

## 2. Environment

| Piece | State this run |
|---|---|
| Docker / Docker Compose | Compose v5.3.0 present; **daemon not running** → all container work NOT RUN |
| Local Postgres :5432 | closed → backend `@SpringBootTest` integration suite NOT RUN |
| Backend :8080 | not running → live header/CORS/correlation checks NOT RUN |
| Node / npm | available → frontend + MCP checks RUN |
| JDK 17 + `mvnw` | available → backend compile + no-DB unit tests RUN |
| Playwright browsers | not installed → E2E NOT RUN |

## 3. Architecture (unchanged — verified by inspection)

- One Axios `apiClient`, one `QueryClient`, one Zustand auth store, one
  `JwtTokenUtil`, one role hierarchy (`frontend/src/lib/roles.ts` /
  backend `RoleHierarchy`). Sprint 12C added **no** second instance of any.
- `docker-compose.yml` already defines `postgres`, `backend`, `frontend`
  (nginx), `mcp-server`, `pgadmin` — all reused, none duplicated.

## 4. Test setup (created this sprint)

| File | Purpose |
|---|---|
| `backend/src/main/resources/application-e2e.yml` | `e2e` Spring profile — real security, disposable DB `commerce_insight_e2e`, Flyway `db/migration` + `db/e2e`, rate limiting **on** with low thresholds (login 4/60s, refresh 6/60s, import/export 3/60s), test-only secrets (never a prod value, ≥32 bytes) |
| `backend/src/main/resources/db/e2e/R__seed_e2e_users.sql` | Repeatable, idempotent RBAC seed — loaded **only** via the extra Flyway location in `application-e2e.yml`, never in dev/test/prod |
| `docker-compose.e2e.yml` | Overlay on the base compose: `SPRING_PROFILES_ACTIVE=e2e`, separate DB + volume, tightened healthchecks, corrected MCP env vars |
| `frontend/playwright.config.ts` | `testDir: e2e/`, `baseURL` from `E2E_BASE_URL` (default `http://localhost:5173`), trace/screenshot on failure, serial, 1 worker |
| `frontend/e2e/helpers.ts` | `USERS` (env-overridable), `loginAs`, `corruptAccessToken` / `corruptAllTokens` (real backend 401s), `countRequests` |
| `frontend/e2e/*.spec.ts` | 4 spec files, 10 scenario groups (§6) |
| `scripts/security-check.mjs` | Zero-dep static scanner (secrets / FE leaks / BE leaks / MCP boundary) |
| `scripts/verify.sh` | CI-style sequence; `--with-e2e` adds the Docker parts |

## 5. Test users

Seeded by `R__seed_e2e_users.sql` (e2e profile only). Passwords are
**test-only** and safe to publish; override via env in CI
(`E2E_STAFF_PASSWORD`, …). Consumed by `frontend/e2e/helpers.ts`.

| Email | Password | Role | Flags |
|---|---|---|---|
| `e2e-staff@commerceinsight.test` | `E2eStaff!234` | STAFF | — |
| `e2e-manager@commerceinsight.test` | `E2eManager!234` | MANAGER | — |
| `e2e-admin@commerceinsight.test` | `E2eAdmin!234` | ADMIN | — |
| `e2e-locked@commerceinsight.test` | `E2eLocked!234` | STAFF | `locked=true` |
| `e2e-disabled@commerceinsight.test` | `E2eDisabled!234` | STAFF | `active=false` |

## 6. E2E scenarios (written; execution pending stack)

| # | Scenario | File | Real backend? |
|---|---|---|---|
| 9.1 | Login STAFF/MANAGER/ADMIN, invalid pw, duplicate submit, pw cleared, no token in DOM | `auth.spec.ts` | yes |
| 9.2 | Unauth → `/login`; no protected-content flash; authed loads route | `auth.spec.ts` | yes |
| 9.3 | STAFF/MANAGER blocked from `/admin`; ADMIN allowed; nav item hidden | `rbac.spec.ts` | yes |
| 9.4 | Real 403 (`GET /users` as STAFF) → `ACCESS_DENIED` envelope, **no refresh**, no logout, no stack trace | `rbac.spec.ts` | yes |
| 9.5 | Corrupt access token → **exactly one** real `POST /auth/refresh` → original retried → Products renders; refresh token rotated | `token-refresh.spec.ts` | yes |
| 9.6 | Corrupt access token + reload of a multi-query page → **exactly one** refresh (single-flight), stays authenticated | `token-refresh.spec.ts` | yes |
| 9.7 | Corrupt both tokens → tokens + `cia-auth` cleared, one redirect to `/login?session=expired&redirect=…`, no loop, no residual data | `auth.spec.ts` | yes |
| 9.8 | 8 rapid bad logins → `429` + `Retry-After`, safe "too many requests" alert, no logout, no retry loop | `rate-limit-and-features.spec.ts` | yes |
| 9.9 | STAFF import → safe `403` (no stack); STAFF sees no uploader; client rejects `.txt`; rate-limited upload → `429`, no infinite retry | `rate-limit-and-features.spec.ts` | yes |
| 9.10 | MANAGER opens orders list; STAFF `POST /orders` → safe `403`, still authed; **409 conflict = `test.fixme`** (needs commerce seed) | `rate-limit-and-features.spec.ts` | yes |

Run: `docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d --build`
then `cd frontend && npx playwright install chromium && npm run test:e2e`.

## 7. Backend integration scenarios (suite exists from 12A; execution pending Postgres)

`backend/src/test/java/com/commerceinsight/security/`:
`SecurityHardeningIntegrationTest` (enveloped 401/403, headers incl. CSP,
`X-Request-Id` echo, CORS allowed/denied, locked-user token rejection,
`import/templates` auth), `RateLimitIntegrationTest` (429 + `Retry-After`),
`LoginHistoryIntegrationTest` (login_history rows + `TOKEN_REFRESH` audit).
Plus the existing ~386-test module suite (migrations, JWT, RBAC, import).

Run: `docker compose up -d postgres && cd backend && ./mvnw test`.

## 8. MCP verification

| Check | Result |
|---|---|
| `npm run type-check` | ✅ exit 0 |
| `npm run build` (`tsc`) | ✅ exit 0 |
| `npm test` (`node --test`) | ✅ **49 pass / 0 fail** (import + export tool suites incl. 401/403/404/429/500/network → safe-message mapping) |
| 7 providers initialize / `/health` 200 / correct `X-MCP-API-KEY` / wrong key → 401 / no key in output / no DB / no LLM | ⚠️ **NOT RUN** — needs the running stack. Static scan confirms no DB driver / LLM SDK import and no key echo in tool output (§9). `mcp-server/src/config` reads `BACKEND_API_URL` + `PORT` — **base `docker-compose.yml` sets `BACKEND_BASE_URL` + `MCP_SERVER_PORT`, which the config ignores** (pre-existing infra mismatch). `docker-compose.e2e.yml` sets the correct names. |

## 9. Static security checks — `node scripts/security-check.mjs`

**RUN. Result: 0 ERROR, 0 WARN — exit 0.**

Scans `backend/src`, `frontend/src`, `frontend/e2e`, `mcp-server/src` for:
committed private keys / JWTs / `Authorization: Bearer` literals / JDBC creds;
credential literals in non-test code; `console.*` / `dangerouslySetInnerHTML`
/ `innerHTML` / `document.cookie` / raw token `localStorage` outside
`authTokens.ts` in shipped frontend code; `printStackTrace` /
`include-stacktrace: always` in backend; DB-driver or LLM-SDK imports and
API-key echoing in the MCP server. Allowlist: `application.yml` (documented
12A dev defaults), `application-e2e.yml` + `R__seed_e2e_users.sql` (labelled
test-only), `authTokens.ts` / `auth.store.ts` (the approved abstraction).

Manual cross-checks (grep): frontend `src/` has **no** `console.*`, **no**
`dangerouslySetInnerHTML`, **no** `document.cookie`; no `MCP_API_KEY` /
`JWT_SECRET` string anywhere in `frontend/src`.

## 10. Dependency checks

| Target | Command | Result |
|---|---|---|
| Frontend (prod deps) | `npm audit --omit=dev` | **2 moderate** — `react-router` / `react-router-dom` (open-redirect via backslash; arbitrary-constructor via SSR `deserializeErrors`). Only fixable by the breaking v7 major. **Deferred / accepted (AR-INFRA1)** — SPA has no SSR hydration; 12B's redirect is `encodeURIComponent`-encoded to a fixed `/login`. |
| Frontend (all) | `npm audit` | 11 total (adds dev-only vite/esbuild/vitest/playwright chain) — not shipped. |
| MCP | `npm audit` | **6 (2 moderate, 4 high)** — `brace-expansion` (via eslint, dev), `fast-uri`, `hono` / `@hono/node-server` (MCP SDK SSE transport — **unused**, server runs stdio), `ip-address`, `js-yaml` (quadratic `!!omap` — no untrusted YAML parsed). **Deferred / accepted (AR-INFRA2)** — pending an `@modelcontextprotocol/sdk` bump; no untrusted input reaches these paths. |
| Backend | — | Maven OWASP/dependency-check not configured in this project; not added (would be unrelated churn). **Deferred (AR-INFRA3).** |

`npm audit fix` (non-force) was trialled on the frontend and **reverted**: it
rewrote ~2200 lines of lockfile and broke `tsc` — unacceptable churn for a
verification sprint.

## 11. Security headers verification

⚠️ **NOT RUN live** (no running backend/nginx). Asserted by:
- `SecurityHardeningIntegrationTest` (12A) — checks `X-Frame-Options: DENY`,
  `X-Content-Type-Options: nosniff`, `Referrer-Policy`, exact CSP string on
  `/api/**`, `Permissions-Policy` present, HSTS present. Pending Postgres.
- `frontend/nginx.conf` (12B) — `X-Frame-Options: DENY`, `Permissions-Policy`,
  SPA CSP (`script-src 'self'`, no inline script), HSTS line commented pending
  TLS. Verified by inspection.
- Prod Swagger disabled: `application-prod.yml` sets
  `springdoc.api-docs.enabled: false` + `swagger-ui.enabled: false` (12A) —
  verified by inspection.

Live check command: `curl -sI http://localhost:8080/api/v1/products` and
`curl -sI http://localhost:5173/`.

## 12. CORS verification

⚠️ **NOT RUN live.** `SecurityHardeningIntegrationTest` covers allowed origin
(`http://localhost:5173` → accepted) vs disallowed origin (→ 403), and
`application*.yml` sets `app.cors.allow-credentials: false` with an explicit
origin list and a hard reject of `*` (12A `CorsConfig`). Bearer-in-header, no
cookie credentials — confirmed by inspection of `axios.ts` + `CorsConfig`.

Live check: `curl -si -X OPTIONS http://localhost:8080/api/v1/products -H "Origin: http://evil.example" -H "Access-Control-Request-Method: GET"`.

## 13. Correlation ID verification

⚠️ **NOT RUN live.** `RequestCorrelationFilter` (12A) generates a
`requestId`, echoes `X-Request-Id`, puts it in MDC (`[%X{requestId:-}]` log
pattern). `SecurityHardeningIntegrationTest` asserts the header is echoed.
Frontend `src/lib/requestId.ts` + the response interceptor capture it;
`PermissionDenied` renders it as "Reference ID" only — never a JWT/Authorization/
MCP key (`requestId.ts` shape-guards to `[A-Za-z0-9._-]{1,64}`).

Live check: `curl -si http://localhost:8080/actuator/health | grep -i x-request-id`.

## 14. Production configuration checks (`application-prod.yml`, by inspection)

| Requirement | State |
|---|---|
| JWT secret from env, no default | ✅ `app.jwt.secret: ${JWT_SECRET}` (no `:default`) |
| MCP key from env, no default | ✅ `app.mcp.api-key: ${MCP_API_KEY}` |
| Min JWT secret strength enforced | ✅ `JwtTokenUtil.@PostConstruct` (≥32 bytes) + `SecretsValidator` (prod: dev-default or <32 bytes → refuse to start) |
| Swagger disabled | ✅ `springdoc.*.enabled: false` |
| Actuator limited | ✅ `management.endpoints.web.exposure.include: health` |
| CORS explicit, no wildcard, credentials false | ✅ `${CORS_ALLOWED_ORIGINS}` required; `CorsConfig` throws on `*`; `allow-credentials: false` |
| Secure headers | ✅ from `SecurityConfig` (shared with dev) |
| `SecretsValidator` not weakened | ✅ untouched by 12C |

⚠️ Startup-failure behaviour (prod + missing/weak/committed-dev secret →
`IllegalStateException`) **NOT RUN** this sprint — 12A `SecretsValidator` and
`JwtTokenUtilTest.weakSecret_failsFast` (✅ passed, see §16) exercise the logic.
Command: `SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run` with no env → expect fail-fast.

## 15. Commands (CI-style — `scripts/verify.sh`)

```
node scripts/security-check.mjs
cd frontend  && npm ci && npm run type-check && npm test && npm run build && npm audit --omit=dev
cd mcp-server && npm ci && npm run type-check && npm test && npm run build && npm audit
cd backend   && ./mvnw -o test-compile
# --- needs Docker / Postgres (NOT RUN here) ---
docker compose up -d postgres && cd backend && ./mvnw test
docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d --build
cd frontend && npx playwright install chromium && npm run test:e2e
docker compose -f docker-compose.yml -f docker-compose.e2e.yml down -v
```

## 16. Actual results (this run)

| Check | Command | Result |
|---|---|---|
| Static security scan | `node scripts/security-check.mjs` | ✅ **0 ERROR / 0 WARN**, exit 0 |
| Frontend type-check | `npx tsc --noEmit` | ✅ exit 0 |
| Frontend unit tests | `npx vitest run` | ✅ **33 passed / 33** (5 files) |
| Frontend build | `npm run build` | ✅ `✓ built in ~5.9s`, exit 0 |
| Frontend audit (prod) | `npm audit --omit=dev` | ⚠️ 2 moderate (react-router) — accepted |
| MCP type-check | `npm run type-check` | ✅ exit 0 |
| MCP tests | `npm test` | ✅ **49 passed / 49** |
| MCP build | `npm run build` | ✅ exit 0 |
| MCP audit | `npm audit` | ⚠️ 6 (2 mod / 4 high) — accepted |
| Backend compile | `./mvnw -o test-compile` | ✅ exit 0 |
| Backend no-DB security tests | `./mvnw -o test -Dtest=JwtTokenUtilTest,McpApiKeyFilterTest,RateLimitingFilterTest,ClientIpResolverTest` | ✅ **20 passed / 20**, BUILD SUCCESS |
| Backend integration suite | `./mvnw test` | ⚠️ **NOT RUN** — no Postgres |
| Docker E2E stack | `docker compose … up` | ⚠️ **NOT RUN** — daemon down |
| Playwright E2E | `npm run test:e2e` | ⚠️ **NOT RUN** — stack + browsers |
| Live header / CORS / correlation | `curl …` | ⚠️ **NOT RUN** — no backend |

## 17. Known failures

None introduced by Sprint 12C. All local checks that could run, passed.

## 18. Accepted risks (carried forward + new)

**From 12A (unchanged, still accepted):**
- AR1 module read-level authorization not normalized.
- AR2 analytics has no role floor / reachable by `ROLE_MCP_SERVICE`.
- AR3 `GET /export/customers` reachable by STAFF (PII role alignment deferred).
- AR4 MANAGER can `PATCH /inventory/{id}/adjust` directly (adjustment-threshold approval deferred).
- AR5 refresh-token hash is unsalted SHA-256 (peppering deferred).

**From 12B (unchanged, still accepted):**
- AR-FE1 refresh token remains in `localStorage` (moving to HttpOnly cookie = backend contract change, out of scope).
- AR-FE2 `src/features/export/*` keeps its own (leak-guarded) error mapper; not migrated to `apiError.ts`.
- AR-FE3 no `eslint.config.*` (ESLint 9) → `npm run lint` aborts; pre-existing, untouched.
- AR-FE4 no separate `tsconfig` for Vitest tests (excluded from the app build instead).

**New in 12C:**
- AR-INFRA1 frontend `react-router` 2 moderate advisories — deferred (needs breaking v7; no SSR exposure).
- AR-INFRA2 MCP transitive advisories (`hono`, `js-yaml`, `fast-uri`, `ip-address`, `brace-expansion`) — deferred (needs MCP SDK bump; unused/stdio paths).
- AR-INFRA3 no backend dependency-vulnerability scanner wired (OWASP dependency-check) — deferred.
- AR-INFRA4 base `docker-compose.yml` passes `BACKEND_BASE_URL` / `MCP_SERVER_PORT` to `mcp-server`, but its config reads `BACKEND_API_URL` / `PORT`. Pre-existing; corrected in `docker-compose.e2e.yml`. One-line fix for the base file recommended but not applied (would touch dev behaviour).
- AR-INFRA5 `frontend/Dockerfile` bakes `VITE_API_BASE_URL` at build time with no build-arg; the E2E overlay relies on the `http://localhost:8080` source default. Fine for the documented topology; a build-arg is the clean long-term fix.

## 19. Deferred items

- Execute the backend integration suite against Postgres.
- Execute the Playwright E2E suite against the Docker stack; wire it into
  `.github/workflows/ci.yml` (currently a placeholder).
- E2E 9.10 **409 business-conflict** (`test.fixme`) — needs a commerce E2E
  seed (customer + product + stock) to deterministically provoke an oversell.
- Live header / CORS / correlation-id `curl` checks against a running backend.
- Prod fail-fast smoke (`SPRING_PROFILES_ACTIVE=prod` with no secrets).
- Optional: `npx playwright install` in CI; backend OWASP dependency-check.

## 20. Final acceptance checklist

Legend: ✅ RUN & PASS · ⚠️ NOT RUN (environment) · ➖ verified by inspection/existing test

| Criterion | Status |
|---|---|
| Docker integration environment defined | ✅ (`docker-compose.e2e.yml`) |
| PostgreSQL healthcheck | ➖ present in base + overlay |
| Backend starts with test config | ⚠️ NOT RUN (no Docker) |
| Flyway migrations succeed | ⚠️ NOT RUN |
| Backend integration tests execute | ⚠️ NOT RUN (no Postgres) |
| Backend security tests pass | ✅ 20/20 no-DB; integration ⚠️ NOT RUN |
| Frontend Vitest passes | ✅ 33/33 |
| Frontend TypeScript passes | ✅ |
| Frontend production build passes | ✅ |
| MCP tests pass | ✅ 49/49 |
| MCP build passes | ✅ |
| MCP health 200 / 7 providers | ⚠️ NOT RUN |
| Playwright configured | ✅ (`playwright.config.ts` + `e2e/`) |
| Login / protected-route / RBAC / 403 / 401-refresh / concurrent-401 / refresh-failure / 429 / import / order E2E | ⚠️ NOT RUN (written, stack required) |
| X-Request-Id verified | ➖ existing integration test + inspection; ⚠️ live NOT RUN |
| CORS verified | ➖ existing integration test + inspection; ⚠️ live NOT RUN |
| Security headers verified | ➖ existing integration test + inspection; ⚠️ live NOT RUN |
| Production secret validation verified | ➖ `SecretsValidator` + `JwtTokenUtilTest` (✅); ⚠️ prod boot NOT RUN |
| Secret scan passes | ✅ 0 findings |
| Frontend leak scan passes | ✅ 0 findings |
| MCP leak scan passes | ✅ 0 findings |
| Dependency audit performed | ✅ (frontend + MCP; findings accepted/deferred) |
| No new secrets committed | ✅ (scan clean; e2e values are labelled test-only) |
| No authentication rebuild | ✅ |
| No business-logic rewrite | ✅ (0 Java main-code changes; only new yml + seed sql) |
| Documentation created | ✅ (this file) |
| Known limitations documented | ✅ (§17–19) |
| Actual command outputs recorded | ✅ (§16) |
