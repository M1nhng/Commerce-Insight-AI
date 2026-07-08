/**
 * store/auth.store.ts — Zustand Auth Store
 *
 * Single source of truth for authentication state.
 * Persisted to localStorage for page reload survival.
 *
 * Architecture:
 * - State: user, tokens, loading, error
 * - Actions: login, register, logout, setUser, initialize
 * - Token storage: localStorage (access_token, refresh_token)
 * - On app start: read tokens from localStorage → validate via /auth/me
 */
import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import type { UserResponse, LoginRequest, RegisterRequest } from '@/types/api.types'
import { authService } from '@/services/auth.service'

// ── State Shape ───────────────────────────────────────────────────────────

interface AuthState {
  // Auth state
  user: UserResponse | null
  accessToken: string | null
  refreshToken: string | null
  isAuthenticated: boolean

  // UI state
  isInitializing: boolean  // True during the app startup check
  isLoading: boolean       // True during login/logout/register calls
  error: string | null
}

// ── Actions Shape ─────────────────────────────────────────────────────────

interface AuthActions {
  login: (data: LoginRequest) => Promise<void>
  register: (data: RegisterRequest) => Promise<void>
  logout: () => Promise<void>
  refreshCurrentUser: () => Promise<void>
  initialize: () => Promise<void>
  clearError: () => void
  setUser: (user: UserResponse) => void
}

type AuthStore = AuthState & AuthActions

// ── Initial State ─────────────────────────────────────────────────────────

const initialState: AuthState = {
  user: null,
  accessToken: null,
  refreshToken: null,
  isAuthenticated: false,
  isInitializing: true,
  isLoading: false,
  error: null,
}

// ── Store ─────────────────────────────────────────────────────────────────

export const useAuthStore = create<AuthStore>()(
  persist(
    (set, get) => ({
      ...initialState,

      /**
       * Login: authenticate user and store tokens + user profile.
       */
      login: async (data: LoginRequest) => {
        set({ isLoading: true, error: null })
        try {
          const response = await authService.login(data)

          // Persist tokens to localStorage for the Axios interceptor
          localStorage.setItem('access_token', response.accessToken)
          localStorage.setItem('refresh_token', response.refreshToken)

          set({
            user: response.user,
            accessToken: response.accessToken,
            refreshToken: response.refreshToken,
            isAuthenticated: true,
            isLoading: false,
            error: null,
          })
        } catch (err) {
          const message = extractErrorMessage(err)
          set({ isLoading: false, error: message, isAuthenticated: false })
          throw err
        }
      },

      /**
       * Register: create account and automatically log in.
       */
      register: async (data: RegisterRequest) => {
        set({ isLoading: true, error: null })
        try {
          const response = await authService.register(data)

          localStorage.setItem('access_token', response.accessToken)
          localStorage.setItem('refresh_token', response.refreshToken)

          set({
            user: response.user,
            accessToken: response.accessToken,
            refreshToken: response.refreshToken,
            isAuthenticated: true,
            isLoading: false,
            error: null,
          })
        } catch (err) {
          const message = extractErrorMessage(err)
          set({ isLoading: false, error: message, isAuthenticated: false })
          throw err
        }
      },

      /**
       * Logout: revoke tokens and clear all auth state.
       */
      logout: async () => {
        set({ isLoading: true })
        try {
          await authService.logout()
        } catch {
          // Ignore server errors — still clear local state
        } finally {
          localStorage.removeItem('access_token')
          localStorage.removeItem('refresh_token')
          set({
            ...initialState,
            isInitializing: false,
            isLoading: false,
          })
        }
      },

      /**
       * Refresh current user profile from /auth/me.
       * Used to re-sync user data after profile updates.
       */
      refreshCurrentUser: async () => {
        try {
          const user = await authService.getCurrentUser()
          set({ user, isAuthenticated: true })
        } catch {
          // Token invalid — will be handled by Axios interceptor
        }
      },

      /**
       * Initialize: called once on app startup.
       * Checks if a stored access token is still valid by calling /auth/me.
       */
      initialize: async () => {
        const { accessToken } = get()
        const storedToken = localStorage.getItem('access_token')

        if (!accessToken && !storedToken) {
          set({ isInitializing: false })
          return
        }

        // Sync localStorage token into store if needed
        if (storedToken && !accessToken) {
          set({ accessToken: storedToken })
        }
        if (localStorage.getItem('refresh_token')) {
          set({ refreshToken: localStorage.getItem('refresh_token') })
        }

        try {
          const user = await authService.getCurrentUser()
          set({ user, isAuthenticated: true, isInitializing: false })
        } catch {
          // Token expired or invalid — clear everything
          localStorage.removeItem('access_token')
          localStorage.removeItem('refresh_token')
          set({ ...initialState, isInitializing: false })
        }
      },

      clearError: () => set({ error: null }),

      setUser: (user: UserResponse) => set({ user }),
    }),
    {
      name: 'cia-auth',  // localStorage key
      storage: createJSONStorage(() => localStorage),
      // Only persist user profile + tokens, not UI state
      partialize: (state) => ({
        user: state.user,
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
)

// ── Selector hooks ────────────────────────────────────────────────────────

/** Returns the current authenticated user. */
export const useCurrentUser = () => useAuthStore((s) => s.user)

/** Returns true if the user is authenticated. */
export const useIsAuthenticated = () => useAuthStore((s) => s.isAuthenticated)

/** Returns the user's role. */
export const useUserRole = () => useAuthStore((s) => s.user?.role)

// ── Utilities ─────────────────────────────────────────────────────────────

function extractErrorMessage(error: unknown): string {
  if (!error || typeof error !== 'object') return 'An unknown error occurred'

  // Axios error with backend ApiResponse
  const axiosError = error as {
    response?: { data?: { error?: { message?: string }; message?: string } }
    message?: string
  }

  return (
    axiosError.response?.data?.error?.message ??
    axiosError.response?.data?.message ??
    axiosError.message ??
    'An unexpected error occurred'
  )
}
