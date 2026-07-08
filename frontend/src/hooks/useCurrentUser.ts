/**
 * hooks/useCurrentUser.ts — TanStack Query hook for current user data.
 *
 * Provides automatic re-fetching and cache invalidation for the user profile.
 * Complements the Zustand store (which handles auth state persistence).
 */
import { useQuery } from '@tanstack/react-query'
import { authService } from '@/services/auth.service'
import { useAuthStore } from '@/store/auth.store'

export const CURRENT_USER_QUERY_KEY = ['auth', 'me'] as const

/**
 * useCurrentUser — fetch and cache the current user's profile.
 *
 * Enabled only when the user is authenticated (has a token).
 * On success: syncs the user into the Zustand store.
 */
export function useCurrentUser() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const setUser = useAuthStore((s) => s.setUser)

  return useQuery({
    queryKey: CURRENT_USER_QUERY_KEY,
    queryFn: async () => {
      const user = await authService.getCurrentUser()
      setUser(user) // Keep Zustand in sync
      return user
    },
    enabled: isAuthenticated,
    staleTime: 5 * 60 * 1000,  // 5 minutes — user data rarely changes
    retry: 1,
    refetchOnWindowFocus: false,
  })
}
