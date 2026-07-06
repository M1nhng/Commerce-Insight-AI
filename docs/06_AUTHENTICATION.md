# 06 — Authentication Design
# Commerce Insight AI

> **Document Type**: Security Architecture
> **Version**: 1.0.0
> **Status**: Approved
> **Last Updated**: 2026-07-06
> **Owner**: Chief Solution Architect

---

## Table of Contents

1. [Purpose](#1-purpose)
2. [Authentication Flow](#2-authentication-flow)
3. [JWT Strategy](#3-jwt-strategy)
4. [Refresh Token Strategy](#4-refresh-token-strategy)
5. [RBAC Design](#5-rbac-design)
6. [Permission Matrix](#6-permission-matrix)
7. [Spring Security Architecture](#7-spring-security-architecture)
8. [Security Best Practices](#8-security-best-practices)
9. [Threat Model](#9-threat-model)
10. [Password Policy](#10-password-policy)
11. [Future Improvements](#11-future-improvements)

---

## 1. Purpose

This document defines the complete authentication and authorization architecture for Commerce Insight AI. It covers the JWT strategy, refresh token rotation, role-based access control (RBAC), the Spring Security filter chain, and the security threat model.

All Spring Security configurations, JWT utilities, and authorization rules MUST strictly follow this design.

---

## 2. Authentication Flow

### 2.1 Login Flow

```
Client                     Spring Boot API                    Database
  │                              │                                │
  │  POST /api/v1/auth/login     │                                │
  │  { email, password }         │                                │
  │ ──────────────────────────► │                                │
  │                              │  SELECT user WHERE email=?    │
  │                              │ ─────────────────────────────►│
  │                              │  ◄─── User entity + roles ───│
  │                              │                                │
  │                              │  BCrypt.verify(password)      │
  │                              │  Generate Access Token (15m)  │
  │                              │  Generate Refresh Token (7d)  │
  │                              │  Store refresh token hash →DB │
  │                              │ ─────────────────────────────►│
  │                              │                                │
  │  200 OK                      │                                │
  │  { accessToken, refreshToken,│                                │
  │    expiresIn, user }         │                                │
  │ ◄────────────────────────── │                                │
```

### 2.2 Authenticated Request Flow

```
Client                     JWT Filter                  Controller/Service
  │                           │                               │
  │  GET /api/v1/products      │                               │
  │  Authorization: Bearer {accessToken}                       │
  │ ─────────────────────────►│                               │
  │                           │  Extract JWT from header      │
  │                           │  Validate signature & expiry  │
  │                           │  Extract userId, roles         │
  │                           │  Set SecurityContext          │
  │                           │ ─────────────────────────────►│
  │                           │                               │  Process request
  │                           │                               │  Check @PreAuthorize
  │  200 OK { data }          │                               │
  │ ◄──────────────────────────────────────────────────────── │
```

### 2.3 Token Refresh Flow

```
Client                     Spring Boot API                    Database
  │                              │                                │
  │  POST /api/v1/auth/refresh   │                                │
  │  { refreshToken }            │                                │
  │ ──────────────────────────► │                                │
  │                              │  Validate refresh token       │
  │                              │  Compare hash with DB record  │
  │                              │ ─────────────────────────────►│
  │                              │  ◄─── Valid token record ────│
  │                              │                                │
  │                              │  Invalidate old refresh token │
  │                              │  Generate new Access Token    │
  │                              │  Generate new Refresh Token   │
  │                              │  Store new refresh token hash │
  │                              │ ─────────────────────────────►│
  │                              │                                │
  │  200 OK                      │                                │
  │  { accessToken, refreshToken}│                                │
  │ ◄────────────────────────── │                                │
```

### 2.4 Logout Flow

```
Client                     Spring Boot API                    Database
  │                              │                                │
  │  POST /api/v1/auth/logout    │                                │
  │  Authorization: Bearer {token}                               │
  │ ──────────────────────────► │                                │
  │                              │  Extract userId from JWT      │
  │                              │  Delete all refresh tokens    │
  │                              │  for this user                │
  │                              │ ─────────────────────────────►│
  │                              │                                │
  │  204 No Content              │                                │
  │ ◄────────────────────────── │                                │
```

---

## 3. JWT Strategy

### 3.1 Access Token Specification

| Property | Value |
|----------|-------|
| Algorithm | HMAC-SHA256 (HS256) |
| Expiry | 15 minutes |
| Storage (client) | In-memory (JavaScript variable) |
| Transport | `Authorization: Bearer {token}` header |
| Issuer | `commerce-insight-ai` |

### 3.2 Access Token Payload

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "iss": "commerce-insight-ai",
  "iat": 1720000000,
  "exp": 1720000900,
  "roles": ["ROLE_ADMIN"],
  "email": "admin@example.com"
}
```

**Field Definitions:**

| Field | Description |
|-------|-------------|
| `sub` | User UUID (primary key) |
| `iss` | Issuer identifier |
| `iat` | Issued-at timestamp (Unix epoch) |
| `exp` | Expiration timestamp (Unix epoch) |
| `roles` | Array of granted roles |
| `email` | User email (for display, not for lookup) |

### 3.3 Token Signing Key Management

- The JWT secret key is a minimum **256-bit** randomly generated string
- Stored exclusively in environment variables (`JWT_SECRET`)
- Never committed to source control
- Rotated on any suspected compromise
- In production: loaded from a secrets manager (e.g., AWS Secrets Manager, HashiCorp Vault)

### 3.4 Token Validation Rules

The JWT filter validates the following on every request:

1. Token is present in the `Authorization` header
2. Token starts with `Bearer ` prefix
3. Token is signed with the correct secret key
4. Token has not expired
5. Token `sub` corresponds to an existing, active user in the database
6. Token `roles` are used to populate the `SecurityContext`

---

## 4. Refresh Token Strategy

### 4.1 Refresh Token Specification

| Property | Value |
|----------|-------|
| Format | Cryptographically secure random UUID (128-bit) |
| Expiry | 7 days |
| Storage (client) | `HttpOnly`, `Secure` cookie OR secure local storage |
| Storage (server) | SHA-256 hash stored in `refresh_tokens` table |
| Rotation | On every use (Refresh Token Rotation) |
| Reuse Detection | Enabled — revokes entire family on reuse |

### 4.2 Refresh Token Database Schema (Logical)

```
refresh_tokens
─────────────
id            UUID  PK
token_hash    TEXT  NOT NULL  (SHA-256 of the actual token)
user_id       UUID  FK → users.id
family_id     UUID  (token family for reuse detection)
expires_at    TIMESTAMPTZ
created_at    TIMESTAMPTZ
revoked_at    TIMESTAMPTZ  (nullable — null = active)
revoked       BOOLEAN DEFAULT FALSE
```

### 4.3 Refresh Token Rotation Logic

1. Client sends refresh token
2. Server looks up token hash in DB
3. If found and not revoked and not expired → **valid**
4. If found but already revoked → **reuse detected** — revoke entire family, return 401
5. If not found → **invalid token** — return 401
6. On success: mark old token as revoked, issue new token pair

### 4.4 Absolute Session Expiry

Even with active refresh token rotation, a session cannot persist indefinitely:

- Maximum absolute session lifetime: **30 days**
- Enforced by tracking the initial login timestamp (`family_created_at`)
- After 30 days, even a valid refresh token is rejected and user must re-login

---

## 5. RBAC Design

### 5.1 Role Hierarchy

```
ADMIN
  └── Full system access
      Can manage users, roles, system settings
      Can view audit logs
      Inherits all MANAGER permissions

MANAGER
  └── Business operations access
      Can view and manage all business data
      Cannot manage users or system settings
      Inherits all STAFF permissions

STAFF
  └── Day-to-day operations access
      Can manage orders and inventory
      Limited read access to analytics
      Cannot access admin or financial data
```

### 5.2 Role Assignment Rules

- A user has exactly **one primary role**
- Roles are stored in a `user_roles` join table to support future multi-role scenarios
- Role changes take effect on the user's **next login** (tokens are not re-issued mid-session)
- Only `ADMIN` users can assign or change roles

---

## 6. Permission Matrix

### 6.1 Module-Level Permissions

| Module | ADMIN | MANAGER | STAFF |
|--------|:-----:|:-------:|:-----:|
| **Dashboard** | R | R | R (limited) |
| **Products** | CRUD | CRUD | R, U |
| **Categories** | CRUD | CRUD | R |
| **Customers** | CRUD | R | R |
| **Orders** | CRUD | CRUD | R, U (status only) |
| **Inventory** | CRUD | CRUD | R, U |
| **Analytics** | R | R | R (limited) |
| **Import** | ✓ | ✓ | ✗ |
| **Export** | ✓ | ✓ | ✗ |
| **AI Assistant** | ✓ | ✓ | ✓ |
| **User Management** | CRUD | ✗ | ✗ |
| **Audit Logs** | R | ✗ | ✗ |
| **System Settings** | CRUD | ✗ | ✗ |

**Legend**: R = Read, C = Create, U = Update, D = Delete, CRUD = Full access, ✓ = Permitted, ✗ = Denied

### 6.2 API-Level Permission Annotations

```
Endpoint Pattern                         Required Role
─────────────────────────────────────────────────────
GET    /api/v1/products/**              ADMIN, MANAGER, STAFF
POST   /api/v1/products                 ADMIN, MANAGER
PUT    /api/v1/products/{id}            ADMIN, MANAGER
DELETE /api/v1/products/{id}            ADMIN

GET    /api/v1/orders/**               ADMIN, MANAGER, STAFF
POST   /api/v1/orders                  ADMIN, MANAGER
PATCH  /api/v1/orders/{id}/status      ADMIN, MANAGER, STAFF
DELETE /api/v1/orders/{id}             ADMIN

GET    /api/v1/analytics/**            ADMIN, MANAGER
GET    /api/v1/analytics/summary        ADMIN, MANAGER, STAFF

POST   /api/v1/import/**               ADMIN, MANAGER
GET    /api/v1/export/**               ADMIN, MANAGER

GET    /api/v1/admin/**                ADMIN
POST   /api/v1/admin/**                ADMIN
PUT    /api/v1/admin/**                ADMIN
DELETE /api/v1/admin/**                ADMIN
```

---

## 7. Spring Security Architecture

### 7.1 Security Filter Chain Order

```
Incoming Request
      │
      ▼
[1] CorsFilter               — Allow/Deny cross-origin requests
      │
      ▼
[2] SessionManagementFilter  — STATELESS session policy
      │
      ▼
[3] JwtAuthenticationFilter  — Extract, validate JWT; populate SecurityContext
      │
      ▼
[4] UsernamePasswordAuthFilter — (Disabled — using JWT)
      │
      ▼
[5] ExceptionTranslationFilter — Convert AccessDeniedException → 403
      │
      ▼
[6] FilterSecurityInterceptor — Enforce @PreAuthorize / HttpSecurity rules
      │
      ▼
Controller / Method
```

### 7.2 JwtAuthenticationFilter Logic

```
doFilterInternal(request, response, chain):
  1. Extract "Authorization" header
  2. If header is null or does not start with "Bearer " → chain.doFilter() (skip)
  3. Extract token = header.substring(7)
  4. Try {
       userId = jwtUtil.extractSubject(token)
       if userId != null AND SecurityContextHolder is empty:
         user = userDetailsService.loadUserByUsername(userId)
         if jwtUtil.isTokenValid(token, user):
           auth = UsernamePasswordAuthenticationToken(user, null, user.authorities)
           auth.details = WebAuthenticationDetailsSource.buildDetails(request)
           SecurityContextHolder.context.authentication = auth
     } catch (JwtException | UsernameNotFoundException) {
       → log warning, clear context, do not throw (let filter chain handle 401)
     }
  5. chain.doFilter()
```

### 7.3 UserDetails Implementation

The `UserDetails` implementation wraps the `User` entity and exposes:
- `getUsername()` → returns user UUID (used as JWT subject)
- `getPassword()` → returns BCrypt-hashed password
- `getAuthorities()` → returns `List<GrantedAuthority>` from user roles
- `isAccountNonExpired()` → true (unless account expiry is added)
- `isAccountNonLocked()` → reflects `user.locked` field
- `isEnabled()` → reflects `user.active` field

---

## 8. Security Best Practices

### 8.1 Password Security

| Practice | Implementation |
|----------|---------------|
| Hashing | BCrypt with strength factor 12 |
| Storage | Only the hash is stored; plaintext never persisted |
| Comparison | BCrypt.matches() — constant-time comparison |
| Transmission | HTTPS only; password in request body, never in URL |

### 8.2 JWT Security

| Practice | Implementation |
|----------|---------------|
| Short-lived access tokens | 15-minute expiry |
| Refresh token rotation | New token on each refresh |
| Reuse detection | Token family invalidation on reuse |
| Secure key storage | Environment variable / secrets manager |
| Token revocation | Logout invalidates all refresh tokens |

### 8.3 API Security

| Practice | Implementation |
|----------|---------------|
| HTTPS enforced | All production traffic over TLS 1.2+ |
| CORS strict policy | Only allowed origins from `app.cors.allowed-origins` |
| Rate limiting | 100 requests/minute per IP (future: per user) |
| Input validation | Bean Validation on all request DTOs |
| SQL injection prevention | JPA/Hibernate parameterized queries only |
| XSS prevention | Jackson HTML escaping; Content-Security-Policy header |
| CSRF protection | Disabled (stateless API; no cookies for session) |
| Security headers | X-Content-Type-Options, X-Frame-Options, HSTS |

### 8.4 Audit Trail

| Event | Logged Fields |
|-------|--------------|
| Login success | userId, email, IP, timestamp |
| Login failure | email (attempted), IP, reason, timestamp |
| Logout | userId, timestamp |
| Refresh token use | userId, familyId, timestamp |
| Suspicious reuse | userId, familyId, IP, timestamp |
| Role change | adminId, targetUserId, oldRole, newRole, timestamp |
| Data modification | userId, entity, entityId, action, timestamp |

---

## 9. Threat Model

### 9.1 Identified Threats

| Threat | Attack Vector | Mitigation |
|--------|--------------|------------|
| **JWT Theft** | XSS attack steals access token from JS memory | Short token expiry (15m), HTTPS only, Content-Security-Policy |
| **Refresh Token Theft** | Cookie theft, network interception | HttpOnly cookie, Secure flag, Refresh Token Rotation |
| **Token Replay** | Attacker replays stolen refresh token | Rotation + reuse detection → family revocation |
| **Brute Force Login** | Automated password attacks | Rate limiting, account lockout after N failures |
| **Credential Stuffing** | Leaked credential lists | BCrypt strength 12, breach detection (future) |
| **Privilege Escalation** | Manipulating JWT role claims | Server-side role validation via `@PreAuthorize` |
| **SQL Injection** | Malicious input in query params | JPA parameterized queries, Bean Validation |
| **IDOR** | Accessing another user's resources by ID | All reads scoped to authenticated user's organization |
| **Expired Token Use** | Client sends expired access token | JWT expiry validation in filter |

### 9.2 Security Assumptions

1. The JWT signing key is never exposed
2. The database is not accessible from the public internet
3. All production communication is over HTTPS
4. The MCP server authenticates with the backend via an API key, not a user JWT

---

## 10. Password Policy

| Rule | Requirement |
|------|-------------|
| Minimum length | 8 characters |
| Maximum length | 128 characters |
| Complexity | At least one uppercase, one lowercase, one digit |
| Special characters | Recommended but not required |
| Common passwords | Rejected against a blocklist of 10,000 common passwords |
| History | Cannot reuse last 5 passwords |
| Expiry | No forced expiry (NIST SP 800-63B compliant) |
| Reset | Secure email link, 1-hour expiry, single use |

---

## 11. Future Improvements

| Improvement | Priority | Notes |
|-------------|----------|-------|
| OAuth 2.0 / OIDC (Google, GitHub) | Medium | Social login for convenience |
| MFA / TOTP | High | Time-based OTP for admin accounts |
| Redis-backed token blacklist | Medium | Instant logout propagation |
| Device fingerprinting | Low | Detect token use from unusual devices |
| Rate limiting per user (not just IP) | High | Prevent authenticated abuse |
| API key authentication | Medium | For programmatic access (non-human clients) |
| Breach detection (HaveIBeenPwned) | Low | Alert on known-compromised passwords |
