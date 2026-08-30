/**
 * services/axios.ts — Shared Axios instance + authentication interceptors.
 *
 * Responsibilities:
 * 1. Attach `Authorization: Bearer <accessToken>` to authenticated requests
 *    (never to login / register / refresh).
 * 2. Capture the backend correlation id (`X-Request-Id`) for troubleshooting.
 * 3. On 401: attempt a token refresh EXACTLY ONCE per request, with a
 *    single-flight guarantee — many concurrent 401s share ONE refresh call.
 * 4. On refresh success: retry the original request once.
 * 5. On refresh failure / 401-after-retry: clear auth + query cache and send
 *    the user to /login (once).
 * 6. 403 and 429 are NOT authentication failures — they never refresh and
 *    never log the user out. They are rejected for the feature layer to
 *    surface via the shared error normalizer.
 *
 * Security: this module never logs request/response objects, Axios config,
 * Authorization headers, or tokens.
 */
import axios, {
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import toast from 'react-hot-toast'
import type { ApiResponse, AuthResponse } from '@/types/api.types'
import {
  getAccessToken,
  getRefreshToken,
  setTokens,
  clearTokens,
} from '@/lib/authTokens'
import { queryClient } from '@/providers/QueryProvider'
import { setLastRequestId } from '@/lib/requestId'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
const API_PREFIX = `${BASE_URL}/api/v1`

// ── Axios instance ──────────────────────────────────────────────────────────
export const apiClient: AxiosInstance = axios.create({
  baseURL: API_PREFIX,
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' },
})

/** Requests that must never carry an Authorization header. */
const NO_AUTH_PATHS = ['/auth/login', '/auth/register', '/auth/refresh']

function isNoAuthPath(url: string | undefined): boolean {
  if (!url) return false
  return NO_AUTH_PATHS.some((p) => url.includes(p))
}

// ── Request interceptor — attach JWT ───────────────────────────────────────
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (!isNoAuthPath(config.url)) {
      const token = getAccessToken()
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`
      }
    } else if (config.headers) {
      // Defensive: strip any stale header set as an instance default.
      delete config.headers.Authorization
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ── Single-flight refresh ──────────────────────────────────────────────────
/** The in-flight refresh, or null. Guarantees at most one refresh HTTP call. */
let refreshPromise: Promise<string> | null = null
/** Ensures the "session expired" toast + redirect only fire once. */
let authFailureHandled = false

/**
 * Perform the refresh exactly once; concurrent callers await the same promise.
 * Uses a bare axios call (not apiClient) so it can never recurse through this
 * interceptor.
 */
function refreshAccessToken(): Promise<string> {
  if (refreshPromise) return refreshPromise

  refreshPromise = (async () => {
    const refreshToken = getRefreshToken()
    if (!refreshToken) throw new Error('missing_refresh_token')

    const response = await axios.post<ApiResponse<AuthResponse>>(
      `${API_PREFIX}/auth/refresh`,
      { refreshToken },
      { timeout: 15_000 }
    )
    const data = response.data?.data
    if (!data?.accessToken || !data?.refreshToken) {
      throw new Error('empty_refresh_response')
    }

    setTokens(data.accessToken, data.refreshToken)
    apiClient.defaults.headers.common.Authorization = `Bearer ${data.accessToken}`

    // Keep the persisted auth store in sync so a later reload uses the new
    // token. Dynamic import avoids a static import cycle (store → service →
    // axios). Failure here is non-fatal.
    void import('@/store/auth.store')
      .then((m) => m.useAuthStore.getState().applyRefreshedTokens(data))
      .catch(() => undefined)

    return data.accessToken
  })().finally(() => {
    refreshPromise = null
  })

  return refreshPromise
}

/**
 * Terminal auth failure: wipe local session state and route to /login once.
 * Never called for 403 or 429.
 */
function handleAuthFailure(): void {
  if (authFailureHandled) return
  authFailureHandled = true

  clearTokens()
  try {
    queryClient.clear()
  } catch {
    /* non-fatal */
  }
  void import('@/store/auth.store')
    .then((m) => m.useAuthStore.getState().forceLogout())
    .catch(() => undefined)

  const path = window.location.pathname
  if (path !== '/login') {
    toast.error('Your session has expired. Please sign in again.', {
      id: 'session-expired',
    })
    const redirect = encodeURIComponent(path + window.location.search)
    window.location.assign(`/login?session=expired&redirect=${redirect}`)
  }
}

// ── Response interceptor — correlation id + 401 refresh ────────────────────
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    const rid = response.headers?.['x-request-id']
    if (typeof rid === 'string' && rid) setLastRequestId(rid)
    return response
  },
  async (error) => {
    const rid = error.response?.headers?.['x-request-id']
    if (typeof rid === 'string' && rid) setLastRequestId(rid)

    const originalRequest = error.config as
      | (InternalAxiosRequestConfig & { _retry?: boolean })
      | undefined
    const status = error.response?.status

    const canAttemptRefresh =
      status === 401 &&
      !!originalRequest &&
      !originalRequest._retry &&
      !isNoAuthPath(originalRequest.url)

    if (!canAttemptRefresh) {
      // 401 on an auth endpoint or after a retry → terminal.
      if (
        status === 401 &&
        originalRequest &&
        !isNoAuthPath(originalRequest.url)
      ) {
        handleAuthFailure()
      }
      // 403 / 429 / 4xx / 5xx / network: pass through untouched.
      return Promise.reject(error)
    }

    originalRequest._retry = true

    try {
      const newToken = await refreshAccessToken()
      originalRequest.headers = originalRequest.headers ?? {}
      originalRequest.headers.Authorization = `Bearer ${newToken}`
      return apiClient(originalRequest)
    } catch (refreshError) {
      handleAuthFailure()
      return Promise.reject(refreshError)
    }
  }
)

/**
 * Test-only: clear the module-level single-flight / one-shot guard state so
 * each test starts clean. Never called in application code.
 * @internal
 */
export function __resetAuthInterceptorState(): void {
  refreshPromise = null
  authFailureHandled = false
}

export default apiClient
