# ADR-003: Backend Security Hardening (Sprint 12A)

**Status**: Accepted
**Date**: 2026-08-30

## Context

The backend already had a working, correctly-architected security stack (stateless JWT access + rotating opaque refresh tokens with family reuse-detection, method-level RBAC, an MCP API-key filter, an explicit CORS origin list, some security headers, a DB-backed audit log). A full audit (`SPRINT_12A_SECURITY_AUDIT.md`) surfaced concrete gaps rather than an architectural problem. Sprint 12A closes those gaps **without** rebuilding authentication, **without** changing any role level, and **without** touching business logic.

## Decisions

1. **Enveloped 401/403.** Replace `HttpStatusEntryPoint` with a `RestAuthenticationEntryPoint` + `RestAccessDeniedHandler` that emit the standard `ApiResponse`/`ErrorResponse` JSON. Reuse the pre-existing `ErrorCode.AUTHENTICATION_REQUIRED` / `ACCESS_DENIED` (the truncated sprint brief showed a literal `UNAUTHORIZED`; we do not add a duplicate code).

2. **Rate limiting: bucket4j, in-memory, no Redis.** A `OncePerRequestFilter` with per-IP (and per-email for login) token buckets on `/api/v1/auth/**`, plus config-gated limits on import/export. Redis is explicitly a non-goal; a single-instance in-memory limiter is sufficient for this deployment. Exceeded → `429 RATE_LIMIT_EXCEEDED` + `Retry-After`. Disabled in the `test` profile.

3. **MCP key: constant-time + fail-closed.** `MessageDigest.isEqual` for the comparison; a *present but wrong* key now returns `401 MCP_INVALID_API_KEY` and stops the chain (previously it fell through). A missing key is still a no-op pass-through so ordinary JWT traffic is unaffected. The MCP server always sends the correct key.

4. **Fail-fast on weak/default secrets in prod.** A `SecretsValidator` (`ApplicationRunner`) refuses to start the `prod` profile when `app.jwt.secret` / `app.mcp.api-key` are the committed dev defaults or the JWT secret is < 256 bits. Non-prod logs a warning.

5. **JWT: additive claims only.** Add `typ="access"` (verified on parse) and `jti` (log correlation, not persisted — the token stays stateless, no blacklist). Explicit ≥ 256-bit key check at startup. Tokens minted before deploy are rejected; the 15-minute TTL self-heals this.

6. **Reject tokens for locked/deactivated users.** `JwtAuthenticationFilter` now checks `isEnabled()` / `isAccountNonLocked()` (already populated by `UserDetailsServiceImpl`) before authenticating, closing the ≤ 15-minute window where a just-locked user keeps access.

7. **RoleHierarchy as defense-in-depth.** `ROLE_ADMIN > ROLE_MANAGER > ROLE_STAFF`. Existing annotations already enumerate every accepted role, so effective access is unchanged; this only protects against a future dropped-role mistake. `ROLE_MCP_SERVICE` stays outside the hierarchy.

8. **Conservative authorization.** The read-level inconsistencies across modules, the STAFF customer-PII export gap, the analytics role-floor, and the manager direct-adjust path are **documented as accepted risks**, not changed — tightening them would break the 8 MCP providers (which authenticate as `ROLE_MCP_SERVICE`) and the Sprint 11B Export UI. Only the one genuinely unannotated endpoint (`GET /api/v1/import/templates/{type}`) gets a `@PreAuthorize`.

9. **Audit completeness.** Persist the already-defined-but-unused `TOKEN_REFRESH` and `REFRESH_TOKEN_REUSE_DETECTED` events; populate the existing `audit_logs.user_agent` column; wire the orphaned `login_history` table (V12) with an `@Async` writer; add a `RequestCorrelationFilter` (`X-Request-Id` + MDC) and an `X-Forwarded-For`-aware `ClientIpResolver`.

## Consequences

**Positive:** consistent machine-readable error envelope everywhere; brute-force resistance on auth; no committed secret can reach prod; tighter token lifecycle; full login/refresh audit trail with request correlation; a documented, source-derived permission matrix.

**Negative / behaviour changes (see audit §14):** a wrong MCP key now 401s instead of falling through; a locked user's live token stops working immediately; prod won't boot with dev secrets; pre-deploy access tokens are rejected for ≤ 15 min after deploy. No Flyway migration is required (`login_history` and `audit_logs.user_agent` already exist).

**Deferred (Accepted-Risk Register in the audit):** module read-level normalisation, `MCP_SERVICE` read allow-listing, export-customer PII role alignment, manager adjustment thresholds, refresh-hash peppering.
