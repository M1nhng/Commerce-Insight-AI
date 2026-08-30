/**
 * router/RoleGuard.tsx — RBAC guard for role-restricted routes.
 *
 * Usage:
 *   <RoleGuard roles={['ADMIN']}>            — ADMIN only
 *   <RoleGuard roles={['MANAGER']}>          — MANAGER or higher (ADMIN)
 *
 * Hierarchy-aware via lib/roles.ts (ADMIN > MANAGER > STAFF). This is UI gating
 * only — the backend remains authoritative and will still return 403.
 * Renders the shared PermissionDenied surface when the user lacks the role.
 */
import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@/store/auth.store'
import type { Role } from '@/types/api.types'
import { hasRole } from '@/lib/roles'
import { PermissionDenied } from '@/components/security/PermissionDenied'

interface RoleGuardProps {
  /** Allowed roles. Listing a role also admits every higher role. */
  roles: Role[]
  /** Optional: redirect instead of showing the permission-denied page. */
  redirectTo?: string
  children: React.ReactNode
}

export function RoleGuard({ roles, redirectTo, children }: RoleGuardProps) {
  const user = useAuthStore((s) => s.user)

  if (!user) return <Navigate to="/login" replace />

  if (!hasRole(user.role, roles)) {
    if (redirectTo) return <Navigate to={redirectTo} replace />
    return <PermissionDenied />
  }

  return <>{children}</>
}
