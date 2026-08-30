/**
 * hooks/useAuth.ts — Primary auth hook for components.
 *
 * Provides a clean, component-friendly API over the Zustand auth store.
 * Components should use this hook instead of calling useAuthStore directly.
 */
import { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/store/auth.store'
import type { LoginRequest, RegisterRequest, Role } from '@/types/api.types'
import { isAtLeast as roleIsAtLeast } from '@/lib/roles'

export function useAuth() {
  const navigate = useNavigate()
  const store = useAuthStore()

  const login = useCallback(
    async (data: LoginRequest) => {
      await store.login(data)
      navigate('/dashboard', { replace: true })
    },
    [store, navigate]
  )

  const register = useCallback(
    async (data: RegisterRequest) => {
      await store.register(data)
      navigate('/dashboard', { replace: true })
    },
    [store, navigate]
  )

  const logout = useCallback(async () => {
    await store.logout()
    navigate('/login', { replace: true })
  }, [store, navigate])

  /**
   * Exact role membership (no hierarchy): hasRole('MANAGER') is false for ADMIN.
   * For "this role or higher" use `isAtLeast`.
   */
  const hasRole = useCallback(
    (...roles: Role[]): boolean => {
      if (!store.user) return false
      return roles.includes(store.user.role)
    },
    [store.user]
  )

  /**
   * Hierarchy-aware minimum-role check. Single source of truth in lib/roles.ts
   * (ADMIN > MANAGER > STAFF), mirroring the backend Sprint 12A RoleHierarchy.
   * UI gating only — the backend still enforces authorization.
   */
  const isAtLeast = useCallback(
    (minimumRole: Role): boolean => roleIsAtLeast(store.user?.role, minimumRole),
    [store.user]
  )

  return {
    // State
    user: store.user,
    isAuthenticated: store.isAuthenticated,
    isLoading: store.isLoading,
    isInitializing: store.isInitializing,
    error: store.error,

    // Actions
    login,
    register,
    logout,
    clearError: store.clearError,
    refreshCurrentUser: store.refreshCurrentUser,

    // Role checks
    hasRole,
    isAtLeast,
    isAdmin: store.user?.role === 'ADMIN',
    isManager: store.user?.role === 'MANAGER' || store.user?.role === 'ADMIN',
  }
}
