/**
 * providers/AuthProvider.tsx — Application startup auth initializer.
 *
 * Runs once on app mount to:
 * 1. Check localStorage for a stored access token
 * 2. Validate it by calling GET /auth/me
 * 3. If valid → restore user session into Zustand store
 * 4. If invalid/expired → clear tokens, user will see login page
 *
 * All protected routes wait for initialization to complete (isInitializing).
 */
import { useEffect, type ReactNode } from 'react'
import { useAuthStore } from '@/store/auth.store'

interface AuthProviderProps {
  children: ReactNode
}

export function AuthProvider({ children }: AuthProviderProps) {
  const initialize = useAuthStore((s) => s.initialize)

  useEffect(() => {
    // Run the token validation check exactly once on mount
    initialize()
  }, [initialize])

  return <>{children}</>
}
