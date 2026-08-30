/**
 * lib/authTokens.ts — Centralized access/refresh token storage.
 *
 * The ONLY place in the app that knows the localStorage key names for the JWT
 * pair. Every other module (Axios client, auth store) goes through these
 * helpers so token access is auditable and consistent.
 *
 * Sprint 12B notes:
 * - Storage stays in localStorage to preserve compatibility with the existing
 *   backend contract (refresh tokens are returned in the JSON body, not a
 *   cookie). This sprint does NOT change that contract.
 * - Tokens are never logged, never placed in URLs/query params, and never
 *   exposed to React component state directly.
 * - `clearTokens()` also drops the persisted Zustand auth blob so a hard
 *   redirect after a failed refresh cannot rehydrate a stale session.
 */

const ACCESS_TOKEN_KEY = 'access_token'
const REFRESH_TOKEN_KEY = 'refresh_token'

/** Zustand persist key (see store/auth.store.ts `name`). */
const PERSISTED_AUTH_KEY = 'cia-auth'

/** Legacy key written by older builds — cleared defensively, never written. */
const LEGACY_USER_KEY = 'user'

function safeGet(key: string): string | null {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

function safeSet(key: string, value: string): void {
  try {
    localStorage.setItem(key, value)
  } catch {
    /* storage unavailable (private mode / quota) — non-fatal */
  }
}

function safeRemove(key: string): void {
  try {
    localStorage.removeItem(key)
  } catch {
    /* non-fatal */
  }
}

export function getAccessToken(): string | null {
  return safeGet(ACCESS_TOKEN_KEY)
}

export function getRefreshToken(): string | null {
  return safeGet(REFRESH_TOKEN_KEY)
}

export function hasTokens(): boolean {
  return !!getAccessToken() && !!getRefreshToken()
}

export function setTokens(accessToken: string, refreshToken: string): void {
  safeSet(ACCESS_TOKEN_KEY, accessToken)
  safeSet(REFRESH_TOKEN_KEY, refreshToken)
}

/**
 * Wipe every trace of the session from browser storage: the token pair, the
 * persisted Zustand auth slice, and any legacy keys. Safe to call repeatedly.
 */
export function clearTokens(): void {
  safeRemove(ACCESS_TOKEN_KEY)
  safeRemove(REFRESH_TOKEN_KEY)
  safeRemove(PERSISTED_AUTH_KEY)
  safeRemove(LEGACY_USER_KEY)
}
