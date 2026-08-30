# Sprint 12B — Frontend Security & Security UX Hardening

Companion to `SPRINT_12A_SECURITY_AUDIT.md` (backend). Scope: **frontend only** — no
backend, auth-architecture, state-management, or MCP changes.

## 1. Pre-implementation audit (as-found)

| Area | As-found | Risk |
|---|---|---|
| **Auth flow** | Zustand `useAuthStore` (persisted → `localStorage['cia-auth']`) is source of truth; `AuthProvider.initialize()` validates via `GET /auth/me`; `useAuth()` wraps store + nav + role helpers. | Sound. Keep. |
| **Token storage** | `localStorage` keys `access_token` / `refresh_token`, **plus** a duplicate copy inside the `cia-auth` persist blob. Raw `localStorage.*` reads scattered across `axios.ts` (3) and `auth.store.ts` (8). Stray `localStorage.removeItem('user')` (legacy key). | Divergence: refresh wrote new tokens to the two flat keys but not to the persist blob → a reload could load a stale access token. No single choke-point. |
| **Refresh flow** | `axios.ts` response interceptor: `isRefreshing` bool + `failedQueue[]`. On 401 (non-auth-endpoint, not `_retry`): raw `axios.post('/auth/refresh')`, then retry. On failure: clear the two flat keys + `window.location.href='/login'`. | (a) `cia-auth` blob **not** cleared → hard-logout could rehydrate `isAuthenticated:true`. (b) Query cache never cleared → sensitive data visible after logout. (c) Queued requests never marked `_retry` → a second 401 wave could re-enter refresh. (d) No `X-Request-Id` capture. (e) 401 detection is status-only. |
| **Axios client** | One shared `apiClient`, `baseURL = ${VITE_API_BASE_URL}/api/v1`. Request interceptor attached `Authorization` to **every** request incl. `/auth/login`. | Minor — login/register/refresh got a pointless bearer header. |
| **Route guard** | `ProtectedRoute` waits on `isInitializing`, redirects to `/login` with `state.from`. | OK. |
| **Role guard** | `RoleGuard` (only on `/admin`) used flat `roles.includes(user.role)` — **not** hierarchy-aware. `useAuth().isAtLeast` (used by ~10 pages) had its **own** inline `['STAFF','MANAGER','ADMIN']` hierarchy. | Two divergent role-check impls. |
| **Error handling** | No central normalizer. Each hook: `err?.response?.data?.error?.message ?? 'Failed to X'` → `toast.error`. `exportService` had its own status switch (incl. 429). `auth.store` had `extractErrorMessage`. `download.ts` had a good leak-guard regex. | A raw 5xx `error.message` could surface server internals. No 413/415/429/`Retry-After` handling outside export. |
| **Logging** | **No `console.*` anywhere** in `src/`. No logger abstraction. | Already clean. |
| **Sensitive flows** | Login (`LoginForm`): show/hide toggle, `autoComplete`, disabled-while-loading — but password **not cleared** after success, and a **DEV default-credentials hint** was rendered. Import upload, order mutations (global `mutations.retry:0`), export blobs, inventory adjust/transfer. | Password lingered in the field; real admin creds printed in dev UI. |
| **Query config** | `queries.retry: 1`, `mutations.retry: 0`. | Blind retry of a 401/403/404/409/413/415/429 query. |
| **Env** | `VITE_API_BASE_URL`, `VITE_MCP_SERVER_URL`, `VITE_APP_NAME`, feature flags. **No** secret `VITE_*`. No `VITE_MCP_API_KEY` anywhere. | Clean. |
| **XSS** | No `dangerouslySetInnerHTML` / `innerHTML` in `src/`. | Clean. |
| **Tests** | **None.** No test runner, no `eslint.config.*` (ESLint 9 → lint aborts). | Pre-existing. |

## 2. Changes (as-built)

### New modules
- `src/lib/authTokens.ts` — the only place that knows the token key names.
  `getAccessToken / getRefreshToken / hasTokens / setTokens / clearTokens`.
  `clearTokens()` also drops `cia-auth` and the legacy `user` key. All wrapped
  in try/catch (private-mode safe).
- `src/lib/apiError.ts` — `normalizeApiError(err): { status, code, message,
  requestId, retryAfterSeconds, fieldErrors }` + `getErrorMessage`,
  `isAuthError`, `isForbiddenError`, `isRateLimited`. Safe copy per status
  (400/401/403/404/409/413/415/429/5xx/network); `LEAK_PATTERN` scrubs stack
  traces, `Exception`, `jdbc:`, `org.springframework`, `com.commerceinsight`,
  `Bearer …`, JWTs, `Authorization:`, PEM. Backend message shown verbatim only
  for 400/409/422 or a `TRUSTED_CODE`. Parses numeric **and** HTTP-date
  `Retry-After`. Captures `X-Request-Id`.
- `src/lib/roles.ts` — single `ROLE_LEVELS` (ADMIN 3 > MANAGER 2 > STAFF 1) +
  `isAtLeast` + hierarchy-aware `hasRole`. Rejects null/unknown role strings.
- `src/lib/requestId.ts` — holds the last-seen `X-Request-Id` (shape-guarded)
  for error surfaces. Single value, never a log, never paired with a token.
- `src/components/security/PermissionDenied.tsx` (+ barrel) — reusable 403
  surface (extracted from `RoleGuard.ForbiddenPage`, same design); optional
  `requestId` → "Reference ID: …". Never logs out / refreshes.

### Modified
- `src/services/axios.ts` — rewritten interceptors:
  - Request: bearer attached **only** when not `/auth/login|register|refresh`;
    strips a stale default `Authorization` on those paths.
  - Response(success): records `X-Request-Id`.
  - **Single-flight refresh** via one shared `refreshPromise` — N concurrent
    401s ⇒ exactly **one** `POST /auth/refresh` (bare `axios`, never recurses).
  - Each request retried **at most once** (`_retry` flag).
  - Refresh success → `setTokens` + sync the persist blob
    (`applyRefreshedTokens`, dynamic import to avoid a static cycle).
  - `handleAuthFailure()` (idempotent, fires once): `clearTokens()` +
    `queryClient.clear()` + `forceLogout()` + one `session-expired` toast +
    `location.assign('/login?session=expired&redirect=<path>')`; skipped when
    already on `/login`.
  - **403 / 429 pass straight through** — no refresh, no logout.
  - `__resetAuthInterceptorState()` — test-only guard reset.
- `src/store/auth.store.ts` — all token I/O via `authTokens`; new
  `forceLogout()` (no server call) and `applyRefreshedTokens()`;
  `extractErrorMessage` now delegates to `getErrorMessage`; `initialize`
  prefers the freshest stored token.
- `src/hooks/useAuth.ts` — `isAtLeast` delegates to `lib/roles`; `hasRole`
  documented as exact-match.
- `src/router/RoleGuard.tsx` — hierarchy-aware (`lib/roles.hasRole`), renders
  `<PermissionDenied/>`.
- `src/providers/QueryProvider.tsx` — `retry` predicate: never retry any 4xx;
  ≤1 retry for 5xx/network. Mutations stay `retry: 0`.
- `src/features/auth/components/LoginForm.tsx` — `resetField('password')` in a
  `finally`; double-submit blocked via `isLoading || isSubmitting`; DEV
  credential hint removed.
- Error normalizer adopted in every mutation `onError` across **orders,
  import, inventory, stock-adjustments, warehouses, products, categories,
  customers, customer-groups, customer-addresses** (`toast.error(getErrorMessage(err))`).
- `frontend/nginx.conf` — `X-Frame-Options: DENY`, `Permissions-Policy`,
  SPA `Content-Security-Policy` (`script-src 'self'`, no inline script), HSTS
  line commented pending TLS. Deploy-only.

### Test infrastructure (new)
Vitest + jsdom + Testing Library (dev-deps only). `vitest.config.ts`,
`src/test/setup.ts`, `npm test` / `npm run test:watch`. Test globs excluded
from `tsconfig.app.json` so `npm run build` is unaffected.

## 3. Behaviour changes (user-visible)

1. 401 responses now show the standard "Your session has expired…" toast +
   redirect to `/login?session=expired&redirect=…` (was a bare
   `window.location.href='/login'`).
2. After any hard logout / failed refresh the **TanStack Query cache is
   cleared** — no stale customers/orders/etc. behind the login screen.
3. `RoleGuard` is hierarchy-aware — a future `roles={['MANAGER']}` route also
   admits ADMIN (today only `/admin` uses it, ADMIN-only, unchanged).
4. Login form clears the password field after every submit; the dev
   default-credentials hint is gone.
5. Failed queries are no longer retried on any 4xx.

## 4. Accepted risks / follow-ups

- **AR-FE1** Refresh tokens remain in `localStorage` (XSS-readable). Moving to
  an HttpOnly cookie is a backend contract change — explicitly out of scope
  for 12B. Mitigated by CSP + no `dangerouslySetInnerHTML` + centralized
  access.
- **AR-FE2** `src/features/export/*` keeps its own (already leak-guarded)
  `messageForStatus`; not migrated to `apiError.ts` to avoid churn in the most
  hardened feature. It lacks `Retry-After` seconds in the 429 copy.
- **AR-FE3** `eslint.config.*` is still absent (ESLint 9) — pre-existing, not
  touched (unrelated config rewrite).
- **AR-FE4** Vite build warns that `auth.store.ts` is both statically and
  dynamically imported → the dynamic `import()` in `axios.ts` won't form its
  own chunk. Harmless: the dynamic import exists to break a *static* import
  cycle, not to code-split.
- The bundle is a single >500 kB chunk (no code-splitting) — pre-existing.
