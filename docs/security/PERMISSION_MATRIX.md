# Commerce Insight — API Permission Matrix

**Authoritative** as of Sprint 12A (2026-08-30). Transcribed from `@PreAuthorize` annotations and `SecurityConfig` in source — not from design docs. Regenerate from source whenever a controller's authorization changes.

## Roles

| Role | Authority | Meaning |
|---|---|---|
| ADMIN | `ROLE_ADMIN` | Full access. All destructive and user-management operations. |
| MANAGER | `ROLE_MANAGER` | Business management: create/update catalog, customers, orders, imports. |
| STAFF | `ROLE_STAFF` | Operational reads + limited operational writes (e.g. raise a stock-adjustment request). |
| *(MCP service)* | `ROLE_MCP_SERVICE` | Synthetic identity for the MCP server via `X-MCP-API-KEY`. **Not** a `Role` enum value. Reaches only `isAuthenticated()` routes. Never granted `ROLE_*` business roles. |

`RoleHierarchy` (added Sprint 12A): `ROLE_ADMIN > ROLE_MANAGER > ROLE_STAFF`. Defense-in-depth only — existing annotations already enumerate every accepted role, so effective access is unchanged. `ROLE_MCP_SERVICE` is outside the hierarchy.

## Public endpoints (no authentication)

| METHOD | PATH |
|---|---|
| POST | `/api/v1/auth/login` |
| POST | `/api/v1/auth/register` |
| POST | `/api/v1/auth/refresh` |
| GET | `/actuator/health`, `/actuator/info` *(info disabled in prod — Sprint 12A)* |
| GET | `/swagger-ui/**`, `/v3/api-docs/**`, `/swagger-resources/**` *(disabled in prod — Sprint 12A)* |

Everything else requires authentication. Unauthenticated → **401** (`AUTHENTICATION_REQUIRED`, enveloped). Authenticated but insufficient role → **403** (`ACCESS_DENIED`, enveloped).

## Matrix

Legend: `A` = ADMIN, `M` = MANAGER, `S` = STAFF, `auth` = any authenticated principal (includes `ROLE_MCP_SERVICE`).

### Auth — `/api/v1/auth`
| METHOD | PATH | Roles |
|---|---|---|
| POST | `/logout` | auth |
| GET | `/me` | auth |
| GET | `/verify` | auth |

### Products — `/api/v1/products`
| METHOD | PATH | Roles |
|---|---|---|
| GET | `/`, `/{id}` | auth |
| POST | `/` | A, M |
| PUT | `/{id}` | A, M |
| DELETE | `/{id}` | A |

### Categories — `/api/v1/categories`
| METHOD | PATH | Roles |
|---|---|---|
| GET | `/`, `/tree`, `/{id}` | auth |
| POST | `/` | A, M |
| PUT | `/{id}` | A, M |
| DELETE | `/{id}` | A |

### Inventory — `/api/v1/inventory`
| METHOD | PATH | Roles |
|---|---|---|
| GET | `/`, `/{id}`, `/product/{productId}` | A, M, S |
| GET | `/low-stock`, `/{id}/transactions` | A, M |
| PATCH | `/{id}/adjust` | A, M |
| POST | `/transfer` | A, M |

### Stock adjustments — `/api/v1/stock-adjustments`
| METHOD | PATH | Roles |
|---|---|---|
| GET | `/`, `/{id}` | A, M |
| POST | `/` | A, M, S |
| PATCH | `/{id}/approve`, `/{id}/reject` | A |

### Warehouses — `/api/v1/warehouses`
| METHOD | PATH | Roles |
|---|---|---|
| GET | `/`, `/{id}` | A, M, S |
| POST | `/` | A, M |
| PUT | `/{id}` | A, M |
| DELETE | `/{id}` | A |

### Customers — `/api/v1/customers`
| METHOD | PATH | Roles |
|---|---|---|
| GET | `/`, `/{id}` | A, M |
| POST | `/` | A, M |
| PUT | `/{id}` | A, M |
| DELETE | `/{id}` | A |
| PATCH | `/{id}/status` | A, M |
| GET/POST/PUT/DELETE/PATCH | `/{id}/addresses/**` | A, M |

### Customer groups — `/api/v1/customer-groups`
| METHOD | PATH | Roles |
|---|---|---|
| GET | `/`, `/{id}` | A, M |
| POST | `/` | A, M |
| PUT | `/{id}` | A, M |
| DELETE | `/{id}` | A |

### Orders — `/api/v1/orders`
| METHOD | PATH | Roles |
|---|---|---|
| GET | `/`, `/{id}` | auth |
| POST | `/` | A, M |
| PATCH | `/{id}/status` | A, M |
| POST | `/{id}/cancel` | A, M |

### Analytics — `/api/v1/analytics`
| METHOD | PATH | Roles |
|---|---|---|
| GET | `/overview`, `/revenue`, `/orders`, `/products/top`, `/customers`, `/payments` | auth *(class-level `isAuthenticated()`; see Accepted-Risk AR2)* |

### Import — `/api/v1/import`
| METHOD | PATH | Roles |
|---|---|---|
| POST | `/products`, `/customers`, `/orders` | A, M |
| GET | `/jobs`, `/jobs/{id}`, `/jobs/{id}/errors` | A, M, S |
| GET | `/templates/{type}` | A, M, S *(annotation added Sprint 12A — F9)* |

### Export — `/api/v1/export`
| METHOD | PATH | Roles |
|---|---|---|
| GET | `/products`, `/customers`, `/orders` | A, M, S *(class-level; see Accepted-Risk AR3)* |
| GET | `/analytics/{revenue,orders,products,customers,payments}` | A, M, S *(class-level)* |

### Users — `/api/v1/users`
| METHOD | PATH | Roles |
|---|---|---|
| GET | `/`, `/{id}` | A |
| POST | `/` | A |
| PUT | `/{id}` | A |
| PATCH | `/{id}/role`, `/{id}/unlock` | A |
| DELETE | `/{id}` | A |

*(class-level `@PreAuthorize("hasRole('ADMIN')")`)*

### Admin / Audit
No HTTP surface. `audit_logs` and `login_history` are written asynchronously and read only via direct DB access.

## Rate limits (Sprint 12A — `app.rate-limit.*`)

| Route group | Limit (default) | Key |
|---|---|---|
| `POST /api/v1/auth/login` | 5 / min **and** 20 / hour | client IP |
| `POST /api/v1/auth/register` | 5 / hour | client IP |
| `POST /api/v1/auth/refresh` | 30 / min | client IP |
| `POST /api/v1/import/**` | 10 / min | authenticated principal (fallback: IP) |
| `GET /api/v1/export/**` | 10 / min | authenticated principal (fallback: IP) |

Exceeded → **429** `RATE_LIMIT_EXCEEDED` (enveloped) + `Retry-After`. Disabled in the `test` profile
(`app.rate-limit.enabled=false`). Per-account brute-force is additionally bounded by the existing
5-failed-attempt account lockout.
