/**
 * router/ProtectedRoute.tsx — Authentication guard for private routes.
 *
 * Behavior:
 * - While app is initializing (checking stored token): show full-screen loader
 * - If authenticated: render children
 * - If not authenticated: redirect to /login with `from` state for redirect-back
 */
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/store/auth.store'
import { AppLoader } from '@/components/ui/AppLoader'

export function ProtectedRoute() {
  const location = useLocation()
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const isInitializing = useAuthStore((s) => s.isInitializing)

  // Show loader while checking stored tokens on startup
  if (isInitializing) {
    return <AppLoader />
  }

  if (!isAuthenticated) {
    return (
      <Navigate
        to="/login"
        state={{ from: location }}
        replace
      />
    )
  }

  return <Outlet />
}
