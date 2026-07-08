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
   * Check if the current user has one of the given roles.
   * ADMIN > MANAGER > STAFF hierarchy.
   */
  const hasRole = useCallback(
    (...roles: Role[]): boolean => {
      if (!store.user) return false
      return roles.includes(store.user.role)
    },
    [store.user]
  )

  /**
   * Check if user has minimum role (hierarchy-aware).
   * isAtLeast('MANAGER') → true for MANAGER and ADMIN
   */
  const isAtLeast = useCallback(
    (minimumRole: Role): boolean => {
      if (!store.user) return false
      const hierarchy: Role[] = ['STAFF', 'MANAGER', 'ADMIN']
      const userIdx = hierarchy.indexOf(store.user.role)
      const minIdx = hierarchy.indexOf(minimumRole)
      return userIdx >= minIdx
    },
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
