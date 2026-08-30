# Sprint 12A — Backend Security Audit

**Date:** 2026-08-30
**Scope:** `backend/` Spring Boot application only. MCP server, frontend, and infrastructure are read for context but not changed by this sprint.
**Method:** Full read of the security layer, every REST controller, the exception layer, configuration, and the existing test suite. Findings below are transcribed **from source**, not assumed.

> This document is the Phase 1 deliverable. The **Findings → Remediation** table at the end maps every gap to the workstream that addresses it, and the **Accepted-Risk Register** records the issues deliberately left unchanged (per the "conservative + document" authorization decision for this sprint).

---

## 1. Authentication

| Aspect | Current state | File |
|---|---|---|
| Library | `io.jsonwebtoken:jjwt` **0.12.6** (api/impl/jackson). Not changed. | `pom.xml` |
| Algorithm | HMAC-SHA (HS256), key via `Keys.hmacShaKeyFor(...)` | `security/JwtTokenUtil.java` |
| Access token TTL | `app.jwt.access-token-expiration-ms` — **900 000 ms (15 min)** default; 300 000 in test profile | `application.yml`, `application-test.yml` |
| Refresh token TTL | `app.jwt.refresh-token-expiration-days` — **7 days** default; 1 in test | `application.yml` |
| Access token claims | `sub` (user UUID), `iss` (`commerce-insight-ai`), `iat`, `exp`, `roles` (`["ROLE_ADMIN"]`), `email` (display only). **No `jti`, no token-type claim, no audience.** | `JwtTokenUtil.generateAccessToken` |
| Signing key source | `app.jwt.secret` → `${JWT_SECRET:<committed dev default>}`. Tries Base64 decode, **falls back to raw `String.getBytes()`**. No explicit minimum-length check in code (jjwt throws `WeakKeyException` lazily if < 256 bit). | `JwtTokenUtil.getSigningKey` |
| Token parsing / validation | `Jwts.parser().verifyWith(key).requireIssuer("commerce-insight-ai").build().parseSignedClaims(token)` — verifies **signature + issuer**; `parseSignedClaims` enforces **expiration**. `isTokenValid` additionally checks `sub == userDetails.getUsername()`. Catches `JwtException`/`IllegalArgumentException` → returns `false`. | `JwtTokenUtil` |
| Roles handling | The `roles` **claim is not used for authorization**. `JwtAuthenticationFilter` re-loads the user from the DB every request (`UserDetailsServiceImpl.loadUserByUsername(userId)`) and derives authorities from the **current DB role** (`ROLE_<role>`). Consistent and safe (a role change takes effect on the next request). | `JwtAuthenticationFilter`, `UserDetailsServiceImpl` |
| Invalid / expired token | `JwtAuthenticationFilter` catches every exception, clears the `SecurityContext`, and **continues the chain**. The request then fails `anyRequest().authenticated()` → the entry point returns **401**. `GlobalExceptionHandler` also maps `ExpiredJwtException`→401 `TOKEN_EXPIRED` and `JwtException`→401 `TOKEN_INVALID` for exceptions thrown outside the filter. | `JwtAuthenticationFilter`, `GlobalExceptionHandler` |
| Locked / disabled user with a live token | `UserDetailsServiceImpl` sets `accountLocked` / `disabled` flags, but `JwtAuthenticationFilter` builds the `Authentication` from `userDetails.getAuthorities()` **without checking `isEnabled()` / `isAccountNonLocked()`**. A user locked/deactivated *after* a token was issued keeps access until the token expires (≤ 15 min). | `JwtAuthenticationFilter` |
| Password hashing | `BCryptPasswordEncoder(12)` — single bean, used by `DaoAuthenticationProvider` and `AuthService.register`. | `config/SecurityConfig.java` |
| Login lockout | 5 failed attempts → `user.locked = true`; `AccountUnlockScheduler` auto-unlocks after ~15 min. | `auth/service/AuthService.java`, `user/scheduler/AccountUnlockScheduler.java` |

### Refresh token storage & rotation

- Table `refresh_tokens` (Flyway `V2__create_users_and_refresh_tokens.sql`): `token_hash VARCHAR(64)`, `user_id`, `family_id`, `expires_at`, `revoked`, `revoked_at`, `created_at`.
- Plain token = `UUID.randomUUID().toString()` (opaque, **not a JWT**). Stored as **SHA-256 hex** (`RefreshTokenService.hashToken`, unsalted). Plain value is returned to the client in `AuthResponse.refreshToken` and never persisted.
- **Rotation:** every `/auth/refresh` revokes the presented token and issues a new one **in the same `family_id`**.
- **Reuse detection:** presenting an already-revoked token → `revokeAllByFamilyId(...)` (whole family nuked) → `422 REFRESH_TOKEN_REUSE_DETECTED`.
- **Logout:** revokes **all** the user's refresh tokens.
- Assessment: solid. Unsalted SHA-256 is acceptable for a 122-bit random UUID (no meaningful precomputation/brute-force surface). **Not changed** this sprint.

---

## 2. Authorization matrix (from source)

Global: `config/SecurityConfig.java` → stateless, CSRF disabled, `@EnableMethodSecurity(prePostEnabled = true)`, `.anyRequest().authenticated()`. **No `RoleHierarchy` bean** — every `@PreAuthorize` must enumerate roles explicitly. Authorities: `ROLE_ADMIN` / `ROLE_MANAGER` / `ROLE_STAFF` (`UserDetailsServiceImpl`) or synthetic `ROLE_MCP_SERVICE` (`McpApiKeyFilter`). Only `@PreAuthorize` is used (no `@Secured` / `@RolesAllowed` / `hasAuthority`).

**Public (SecurityConfig `permitAll`):** `POST /api/v1/auth/{login,register,refresh}`, `/actuator/health`, `/actuator/info`, `/swagger-ui**`, `/v3/api-docs**`, `/swagger-resources/**`.

| Module | METHOD | PATH | Required role(s) | Source |
|---|---|---|---|---|
| **product** | GET | `/api/v1/products`, `/api/v1/products/{id}` | any authenticated (`isAuthenticated()`) | method |
| | POST / PUT | `/api/v1/products`, `/api/v1/products/{id}` | ADMIN, MANAGER | method |
| | DELETE | `/api/v1/products/{id}` | ADMIN | method |
| **category** | GET | `/api/v1/categories`, `/categories/tree`, `/categories/{id}` | any authenticated | method |
| | POST / PUT | `/api/v1/categories`, `/categories/{id}` | ADMIN, MANAGER | method |
| | DELETE | `/api/v1/categories/{id}` | ADMIN | method |
| **inventory** | GET | `/api/v1/inventory`, `/inventory/{id}`, `/inventory/product/{productId}` | ADMIN, MANAGER, STAFF | method |
| | GET | `/api/v1/inventory/low-stock`, `/inventory/{id}/transactions` | ADMIN, MANAGER | method |
| | PATCH / POST | `/api/v1/inventory/{id}/adjust`, `/inventory/transfer` | ADMIN, MANAGER | method |
| **stock-adjustments** | GET | `/api/v1/stock-adjustments`, `/stock-adjustments/{id}` | ADMIN, MANAGER | method |
| | POST | `/api/v1/stock-adjustments` | ADMIN, MANAGER, STAFF | method |
| | PATCH | `/api/v1/stock-adjustments/{id}/approve`, `/reject` | ADMIN | method |
| **warehouses** | GET | `/api/v1/warehouses`, `/warehouses/{id}` | ADMIN, MANAGER, STAFF | method |
| | POST / PUT | `/api/v1/warehouses`, `/warehouses/{id}` | ADMIN, MANAGER | method |
| | DELETE | `/api/v1/warehouses/{id}` | ADMIN | method |
| **customer** | GET / POST / PUT | `/api/v1/customers`, `/customers/{id}` | ADMIN, MANAGER | method |
| | DELETE | `/api/v1/customers/{id}` | ADMIN | method |
| | PATCH | `/api/v1/customers/{id}/status` | ADMIN, MANAGER | method |
| | (all address sub-routes) | `/api/v1/customers/{id}/addresses/**` | ADMIN, MANAGER | method |
| **customer-groups** | GET / POST / PUT | `/api/v1/customer-groups`, `/customer-groups/{id}` | ADMIN, MANAGER | method |
| | DELETE | `/api/v1/customer-groups/{id}` | ADMIN | method |
| **order** | GET | `/api/v1/orders`, `/orders/{id}` | any authenticated | method |
| | POST / PATCH / POST | `/api/v1/orders`, `/orders/{id}/status`, `/orders/{id}/cancel` | ADMIN, MANAGER | method |
| **analytics** | GET | `/api/v1/analytics/**` (overview, revenue, orders, products/top, customers, payments) | any authenticated | **class-level** `@PreAuthorize("isAuthenticated()")` |
| **import** | POST | `/api/v1/import/{products,customers,orders}` | MANAGER, ADMIN | method |
| | GET | `/api/v1/import/jobs`, `/import/jobs/{id}`, `/import/jobs/{id}/errors` | STAFF, MANAGER, ADMIN | method |
| | GET | `/api/v1/import/templates/{type}` | **NONE — filter chain only** (any authenticated incl. `ROLE_MCP_SERVICE`) | — |
| **export** | GET | `/api/v1/export/**` (products, customers, orders, analytics/*) | STAFF, MANAGER, ADMIN | class-level |
| **auth** | POST/GET | `/api/v1/auth/{logout,me,verify}` | any authenticated (`isAuthenticated()`) | method |
| **user** | ALL | `/api/v1/users/**` | ADMIN | class-level `@PreAuthorize("hasRole('ADMIN')")` |
| **admin** | — | (no REST controller — `AuditLog` is not HTTP-exposed) | — | — |

### Cross-module inconsistencies observed

1. **"Read a business entity" has three different bars:** product/category/order/analytics GET = any authenticated (incl. MCP); inventory/warehouse GET = STAFF+; customer/customer-group GET = MANAGER+.
2. **Analytics has no role floor.** All financial analytics readable by any authenticated principal, including `ROLE_MCP_SERVICE`. The same numbers via `/api/v1/export/analytics/*` require STAFF+.
3. **STAFF can bulk-export customer PII it cannot list.** `GET /api/v1/customers` = MANAGER+, but `GET /api/v1/export/customers` (full table incl. addresses) = STAFF+ (class-level on `ExportController`).
4. **`GET /api/v1/import/templates/{type}` has no `@PreAuthorize`.** Only handler in a security-relevant controller relying purely on the filter chain. Low severity (static CSV headers).
5. **MANAGER can bypass the stock-adjustment approval workflow** via a direct `PATCH /inventory/{id}/adjust` (ADMIN+MANAGER), while `stock-adjustments/{id}/approve` is ADMIN-only.
6. **No `RoleHierarchy`** — a dropped role in an annotation = silent lockout; no defense-in-depth.

---

## 3. MCP authentication (`security/McpApiKeyFilter.java`)

| Aspect | Current state |
|---|---|
| Header | `X-MCP-API-KEY` |
| Comparison | `providedKey.equals(configuredApiKey)` — **plain `String.equals`, not constant-time** (timing side-channel). |
| Configured key | `@Value("${app.mcp.api-key}")` → `${MCP_API_KEY:mcp-dev-secret-key-change-in-production}`. **Committed default.** `application-prod.yml` does **not** override it. |
| Path scope | `shouldNotFilter` skips everything not under `/api/`. |
| Success | Sets `UsernamePasswordAuthenticationToken("mcp-service", null, [ROLE_MCP_SERVICE])`. Reaches only `isAuthenticated()` endpoints (product/category/order GET, **all analytics**, `/import/templates`, `/auth/me|verify|logout`). Cannot reach any `hasRole`/`hasAnyRole` route. |
| Wrong key | `log.warn("Invalid MCP API key received from IP: {}", remoteAddr)` then **`filterChain.doFilter` anyway (fails open)** — request proceeds unauthenticated (or as the JWT user if a Bearer token is also present). |
| Missing key | No-op, chain continues. |
| Rate limiting / lockout on bad keys | None. |

---

## 4. Error handling

`exception/GlobalExceptionHandler.java` — single `@RestControllerAdvice`. Envelope: `ApiResponse.error(ErrorResponse.of(code, message))` → `{ "success": false, "error": { "code", "message", "details"? }, "timestamp" }`. `server.error.include-message/-stacktrace/-exception` all `never`/`false`.

| Status | Trigger | Code | Notes |
|---|---|---|---|
| 400 | `MethodArgumentNotValidException`, `BindException`, `MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException`, `HttpMessageNotReadableException` | `VALIDATION_ERROR` | body-validation includes `details[]` |
| **401** | **unauthenticated request** | *(none)* | **`HttpStatusEntryPoint(401)` → empty body, NOT the envelope** ⚠️ |
| 401 | `BadCredentialsException` / `DisabledException` / `LockedException` / `ExpiredJwtException` / `JwtException` | `INVALID_CREDENTIALS` / `ACCOUNT_DISABLED` / `ACCOUNT_LOCKED` / `TOKEN_EXPIRED` / `TOKEN_INVALID` | enveloped |
| 403 | `AccessDeniedException` (method-security denial, propagated) | `ACCESS_DENIED` | enveloped |
| 404 | `ResourceNotFoundException`, `NoResourceFoundException` | `<domain code>` / `RESOURCE_NOT_FOUND` | |
| 405 | `HttpRequestMethodNotSupportedException` | `"METHOD_NOT_ALLOWED"` (string literal) | |
| 409 | `DuplicateResourceException` | `<domain code>` | |
| 422 | `BusinessRuleException`, `ImportException`, `ExportException` (422 case) | `<domain code>` / `IMPORT_VALIDATION_FAILED` | |
| **429** | — | — | **no handler exists** ⚠️ |
| 500 | any other `Exception` | `INTERNAL_ERROR` | hard-coded generic message; full trace logged server-side only |

**Gaps (currently fall through to the 500 catch-all):** `ConstraintViolationException` (param `@Validated`), `DataIntegrityViolationException` (DB unique/FK), `MaxUploadSizeExceededException` (import uploads), `HttpMediaTypeNotSupportedException`, `MissingServletRequestPartException`.

**401 vs 403:** unauthenticated → 401 (bare status); authenticated-but-wrong-role → 403 (enveloped). Response-shape split brain: 401 has no body, everything else does.

---

## 5. Sensitive data

- **DTOs are clean.** `AuthResponse` returns `accessToken` + opaque `refreshToken` (by design, body not cookie). Neither `UserResponse` exposes `passwordHash`. `AuthMapper` has no `passwordHash` target. `User` entity is never returned raw.
- No DTO / response references `jwtSecret`, `mcpApiKey`, or DB credentials.
- **Logging:** no secret values are logged. But **PII (email) is logged at INFO** in `AuthService` (`"New user registered: {email} ({id})"`, `"User logged in: {email} ({id})"`, `"Account locked ... {email}"`). Refresh-token reuse logs `userId` + `familyId` only. `McpApiKeyFilter` logs the client IP, not the key. No request-body logging.
- `server.error.*` disabled → no stack traces to clients. `ex.getMessage()` returned verbatim only for developer-authored domain exceptions (no SQL/JDBC content) — spot-checked OK.

---

## 6. Configuration & secrets

| Property | Default (committed) | Prod override | Risk |
|---|---|---|---|
| `app.jwt.secret` | `CommerceInsightAIDevSecretKeyMustBe256BitsLongForHMACSHA256OK` | `${JWT_SECRET}` in base only — **`application-prod.yml` has no `app.*` block** | ⚠️ prod silently uses the committed dev secret if `JWT_SECRET` is unset |
| `app.jwt.issuer` | `commerce-insight-ai` (no env placeholder) | — | low |
| `app.mcp.api-key` | `mcp-dev-secret-key-change-in-production` | `${MCP_API_KEY}` in base only | ⚠️ same as above |
| `app.cors.allowed-origins` | `localhost:5173,5174,3000` (+`4173` in dev) | `${CORS_ALLOWED_ORIGINS}` | empty env → `[""]` invalid origin → fails **closed** (acceptable) |
| `app.cors.allowed-headers`, `app.cors.allow-credentials` | present in YAML | — | **dead keys** — `CorsConfig` hard-codes `List.of("*")` and `setAllowCredentials(true)` and never reads them |
| DB password | `postgres` | `${SPRING_DATASOURCE_PASSWORD}` (required, no default) | low |
| Swagger UI | `permitAll` in all profiles | — | ⚠️ exposed in prod |
| Actuator | `health,info,metrics`; `/actuator/{health,info}` permitAll | `health` details `never` | `/actuator/info` public in prod |

---

## 7. CORS (`config/CorsConfig.java`)

- `CorsConfigurationSource` bean registered for `/api/**`, consumed by `SecurityConfig.cors(cors -> {})`. No `@CrossOrigin` anywhere.
- `setAllowedOrigins(<exact list>)` — **no wildcard**. Split on `,` with **no `.trim()`**.
- `setAllowedMethods(GET,POST,PUT,PATCH,DELETE,OPTIONS)`.
- `setAllowedHeaders(List.of("*"))` — hard-coded.
- `setExposedHeaders(Authorization, Content-Disposition, X-Total-Count)`.
- `setAllowCredentials(true)` — hard-coded. Legal only because origins are an explicit list. Comment says "for refresh token in HttpOnly cookie, future" — **tokens currently go in the body**, so `true` is not needed today.
- `setMaxAge(3600)`.

---

## 8. Security headers

`config/SecurityConfig.java` `.headers(...)` explicitly sets:

| Header | Value |
|---|---|
| `X-Frame-Options` | `DENY` |
| `X-Content-Type-Options` | `nosniff` |
| `Strict-Transport-Security` | `max-age=31536000 ; includeSubDomains` (no `preload`) |
| `Referrer-Policy` | `strict-origin-when-cross-origin` |

**Missing:** `Content-Security-Policy`, `Permissions-Policy`, explicit `Cache-Control: no-store` on auth responses. Spring's default `X-XSS-Protection: 0` is emitted. `docker/nginx/nginx.conf` is a documented placeholder and sets **no** security headers at the edge.

---

## 9. Rate limiting

**None.** No `bucket4j` / `resilience4j` / gateway / custom throttle filter; no dependency in `pom.xml`. Only mitigations: login lockout (5 attempts), refresh rotation + reuse detection. `/auth/login`, `/register`, `/refresh` and the import/export endpoints have no per-IP or per-endpoint limit.

---

## 10. Audit logging

- `admin/domain/AuditLog.java` → `audit_logs` (Flyway `V9`): `user_id, action, entity_type, entity_id, old_value jsonb, new_value jsonb, ip_address, user_agent, created_at`. Append-only.
- `admin/service/AuditLogService.java` — `@Async` + `@Transactional(REQUIRES_NEW)`, failures swallowed. Sole writer. Actions wired: `USER_LOGIN`, `USER_LOGIN_FAILED`, `USER_LOGOUT`, `USER_CREATED` (+ user-management actions elsewhere).
- **Gaps:** `user_agent` column exists on the entity but is **never populated** (no overload accepts it). Constants `ACTION_TOKEN_REFRESH` / `ACTION_REFRESH_TOKEN_REUSE` are **defined but never called**. The `login_history` table (Flyway `V12`, with 4 indexes) is **fully orphaned** — no entity, repository, or writer. No MDC / request-id / correlation-id filter anywhere.

---

## 11. Existing tests (behaviour this sprint must not break)

- Infra: `@SpringBootTest(RANDOM_PORT)` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`, **real local Postgres** `localhost:5432/commerce_insight`, per-module `db/cleanup_*_test.sql` via `@Sql(BEFORE_TEST_CLASS)`. `spring-security-test` present; `@WithMockUser` not actually used.
- `AuthControllerIntegrationTest` — 18 ordered tests. **401 assertions check status only, never the body** → adding a 401 envelope is safe. Asserts exact `error.code` + status for register 409/400, refresh 422 (`REFRESH_TOKEN_INVALID`, `REFRESH_TOKEN_REUSE_DETECTED`).
- `UserControllerIntegrationTest` — STAFF → 403, no token → 401 (status only).
- `OrderControllerIntegrationTest` — one 401, one 403 (status only).
- Other `*ControllerIntegrationTest` — boot full context (real `SecurityFilterChain` + `GlobalExceptionHandler`), authenticate with a real JWT, assert the envelope + `error.code` for 400/404/409/422.
- **No** CORS test, **no** `JwtAuthenticationFilter` / `McpApiKeyFilter` test, **no** `X-MCP-API-KEY` test.

**Constraint:** keep every existing status code and `error.code` string for 400/404/405/409/422 stable.

---

## 12. Findings → Remediation

| # | Finding | Severity | Remediation | Workstream |
|---|---|---|---|---|
| F1 | Unauthenticated 401 returns an empty body (not the envelope) | Medium | `RestAuthenticationEntryPoint` + `RestAccessDeniedHandler` emitting `ApiResponse`/`ErrorResponse` (`AUTHENTICATION_REQUIRED` / `ACCESS_DENIED`) | WS2 |
| F2 | No rate limiting on auth / import / export | High | bucket4j in-memory `RateLimitingFilter`, per-IP (auth) / per-principal (import,export), 429 + `Retry-After` + `RATE_LIMIT_EXCEEDED`. Per-account brute-force stays covered by the existing 5-attempt lockout. | WS5 |
| F3 | MCP key compared with `String.equals`; wrong key fails open | Medium | `MessageDigest.isEqual`; wrong key → 401 `MCP_INVALID_API_KEY`, chain stopped | WS4 |
| F4 | Prod silently inherits committed dev `JWT_SECRET` / `MCP_API_KEY` | High | `SecretsValidator` — refuse to start in `prod` with a dev default or < 256-bit secret | WS7 |
| F5 | Access token missing token-type; weak-secret only caught lazily | Low/Med | `typ="access"` + `jti` claims; explicit ≥ 32-byte key check + `@PostConstruct` validation | WS3 |
| F6 | Locked/deactivated user keeps access until token expiry | Medium | `JwtAuthenticationFilter` rejects `!isEnabled()` / `!isAccountNonLocked()` | WS3 |
| F7 | `ConstraintViolationException`, `DataIntegrityViolationException`, `MaxUploadSizeExceededException`, media-type / multipart errors → 500 | Medium | Explicit `@ExceptionHandler`s → 400 / 409 / 413 / 415 | WS8 (exception handlers) |
| F8 | No CSP / Permissions-Policy; no `no-store` on auth; nginx sets nothing | Medium | `SecurityConfig.headers` CSP + Permissions-Policy + HSTS preload; auth `no-store` filter; nginx edge header block | WS8 |
| F9 | `GET /api/v1/import/templates/{type}` unannotated | Low | `@PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")` | WS9 |
| F10 | No `RoleHierarchy` — dropped-role = silent lockout | Low | `RoleHierarchy` bean (`ADMIN > MANAGER > STAFF`) + method expression handler | WS9 |
| F11 | `audit_logs.user_agent` never populated | Low | `AuditLogService` overload + plumb `User-Agent` from `AuthController` | WS10 |
| F12 | `TOKEN_REFRESH` / `REFRESH_TOKEN_REUSE_DETECTED` audit events never persisted | Medium | Wire `AuditLogService.log(...)` in `AuthService.refresh` and `RefreshTokenService` reuse branch | WS10 |
| F13 | `login_history` table orphaned | Low | `LoginHistory` entity + repo + `@Async` `LoginHistoryService`; write on every login success/failure branch | WS10 |
| F14 | No request correlation id in logs | Low | `RequestCorrelationFilter` (MDC `requestId`, `X-Request-Id` echo) + log pattern | WS6 |
| F15 | Unconditional `X-Forwarded-For` trust for audit IP | Low | `ClientIpResolver` — honour XFF only from `app.security.trusted-proxies` | WS6 |
| F16 | PII (email) logged at INFO on register / login / lockout | Low | Log user id only | WS10 |
| F17 | Dead `app.cors.allowed-headers` / `allow-credentials` keys; no origin `.trim()` | Low | Bind from properties, `.trim()`, reject `*`, default `allow-credentials: false` | WS7 |
| F18 | Swagger UI + `/actuator/info` reachable in prod | Low | `springdoc.*.enabled: false` + actuator `health` only in `application-prod.yml` | WS7 |

---

## 13. Accepted-Risk Register (documented, NOT changed this sprint)

The sprint decision is **conservative** authorization: keep every current effective role level so the 8 MCP providers and the Sprint 11B Export UI keep working unchanged. The following are recorded and deferred.

| ID | Issue | Why deferred | Suggested future action |
|---|---|---|---|
| AR1 | Read-level inconsistency (product/order/analytics GET = any authenticated; inventory = STAFF+; customer = MANAGER+) | Tightening product/order/analytics reads to STAFF+ would break `ProductToolsProvider`, `OrderToolsProvider`, `AnalyticsToolsProvider` (they authenticate as `ROLE_MCP_SERVICE`, which is not in any role hierarchy). Needs a coordinated MCP + frontend regression pass. | Introduce an `MCP_SERVICE` allow-list on read routes, then standardise reads to STAFF+. |
| AR2 | Analytics has no role floor and is reachable by `ROLE_MCP_SERVICE` | Same MCP dependency as AR1; analytics is the primary MCP use case. | Decide the intended floor (STAFF+?) and add `MCP_SERVICE` explicitly. |
| AR3 | `STAFF` can `GET /api/v1/export/customers` (PII bulk dump) but not `GET /api/v1/customers` | Raising it to MANAGER+ changes the Sprint 11A contract and the Sprint 11B Export UI (STAFF currently sees the Customers export card). Product decision required. | Align export/customers + export/analytics/customers with the list-API role (MANAGER+). |
| AR4 | `MANAGER` can `PATCH /inventory/{id}/adjust` directly, bypassing the ADMIN-only approval workflow | Changing it alters inventory business behaviour, explicitly out of scope for a hardening sprint. | Route manager adjustments above a threshold through `stock-adjustments`. |
| AR5 | Refresh-token hash is unsalted SHA-256 | Acceptable for a 122-bit random UUID; changing the hash scheme risks breaking active sessions on deploy. | Optional: HMAC-SHA-256 with a server pepper on the next auth-touching sprint. |

---

## 14. Sprint 12A behaviour changes (call-outs)

1. **Wrong `X-MCP-API-KEY` → 401** (`MCP_INVALID_API_KEY`) instead of silently continuing. The MCP server always sends the correct key; a wrong key is always attack or misconfig.
2. **Locked / deactivated user's still-valid access token stops working** on the next request (≤ 15 min window closed).
3. **Prod refuses to start** when `app.jwt.secret` / `app.mcp.api-key` are the committed dev defaults or the secret is < 256-bit.
4. New access tokens carry `typ` + `jti`; tokens minted before deploy are rejected (15-min TTL → self-heals).
5. 401 responses now carry the standard JSON envelope (previously empty body). Frontend token-refresh logic keys off status 401 — unaffected.
6. Error code note: the (truncated) sprint brief's example used `"code": "UNAUTHORIZED"`; this implementation uses the pre-existing `ErrorCode.AUTHENTICATION_REQUIRED` to avoid duplicate codes.
