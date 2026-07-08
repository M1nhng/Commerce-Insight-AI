/**
 * services/axios.ts — Axios instance with JWT interceptors.
 *
 * Responsibilities:
 * 1. Attach Authorization: Bearer <token> to every request.
 * 2. On 401 responses: automatically attempt token refresh.
 * 3. On refresh success: retry the original failed request.
 * 4. On refresh failure: clear auth state and redirect to /login.
 * 5. Never expose raw Axios errors — normalize to ApiError.
 */
import axios, {
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import type { ApiResponse, AuthResponse } from '@/types/api.types'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

// ── Axios Instance ──────────────────────────────────────────────────────
export const apiClient: AxiosInstance = axios.create({
  baseURL: `${BASE_URL}/api/v1`,
  timeout: 15_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Token refresh state — prevent concurrent refresh calls
let isRefreshing = false
let failedQueue: Array<{
  resolve: (token: string) => void
  reject: (error: unknown) => void
}> = []

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error)
    } else if (token) {
      resolve(token)
    }
  })
  failedQueue = []
}

// ── Request Interceptor — Attach JWT ────────────────────────────────────
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // Dynamically read token on each request (not captured at startup)
    const token = localStorage.getItem('access_token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ── Response Interceptor — Handle 401 + Token Refresh ─────────────────
apiClient.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error) => {
    const originalRequest = error.config

    // Only intercept 401 that is NOT from the refresh or login endpoints
    const isAuthEndpoint =
      originalRequest?.url?.includes('/auth/login') ||
      originalRequest?.url?.includes('/auth/refresh') ||
      originalRequest?.url?.includes('/auth/register')

    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !isAuthEndpoint
    ) {
      if (isRefreshing) {
        // Queue this request until refresh completes
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            return apiClient(originalRequest)
          })
          .catch((err) => Promise.reject(err))
      }

      originalRequest._retry = true
      isRefreshing = true

      const refreshToken = localStorage.getItem('refresh_token')

      if (!refreshToken) {
        isRefreshing = false
        clearAuthAndRedirect()
        return Promise.reject(error)
      }

      try {
        const response = await axios.post<ApiResponse<AuthResponse>>(
          `${BASE_URL}/api/v1/auth/refresh`,
          { refreshToken }
        )

        const data = response.data.data
        if (!data) throw new Error('Refresh failed: empty response')

        // Store new tokens
        localStorage.setItem('access_token', data.accessToken)
        localStorage.setItem('refresh_token', data.refreshToken)

        // Update default header
        apiClient.defaults.headers.common.Authorization = `Bearer ${data.accessToken}`

        processQueue(null, data.accessToken)

        // Retry the original request
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`
        return apiClient(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError, null)
        clearAuthAndRedirect()
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  }
)

function clearAuthAndRedirect() {
  localStorage.removeItem('access_token')
  localStorage.removeItem('refresh_token')
  localStorage.removeItem('user')
  // Hard redirect to login — avoids stale React state
  window.location.href = '/login'
}

export default apiClient
