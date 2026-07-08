/**
 * router/RoleGuard.tsx — RBAC guard for role-restricted routes.
 *
 * Usage:
 *   <RoleGuard roles={['ADMIN']}> — only ADMIN can access
 *   <RoleGuard roles={['ADMIN', 'MANAGER']}> — ADMIN or MANAGER
 *
 * Renders a 403 Forbidden page if the user lacks the required role.
 */
import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@/store/auth.store'
import type { Role } from '@/types/api.types'
import { ShieldAlert } from 'lucide-react'

interface RoleGuardProps {
  /** The roles allowed to access this route. */
  roles: Role[]
  /** Optional: redirect path instead of showing forbidden page */
  redirectTo?: string
  children: React.ReactNode
}

export function RoleGuard({ roles, redirectTo, children }: RoleGuardProps) {
  const user = useAuthStore((s) => s.user)

  if (!user) return <Navigate to="/login" replace />

  const hasRole = roles.includes(user.role)

  if (!hasRole) {
    if (redirectTo) return <Navigate to={redirectTo} replace />
    return <ForbiddenPage />
  }

  return <>{children}</>
}

// ── 403 Forbidden Page ────────────────────────────────────────────────────

function ForbiddenPage() {
  return (
    <div className="flex min-h-screen items-center justify-center" style={{ background: 'var(--bg-base)' }}>
      <div className="text-center animate-fade-in">
        <div className="flex justify-center mb-6">
          <div className="flex h-20 w-20 items-center justify-center rounded-2xl"
               style={{ background: 'var(--error-bg)', border: '1px solid var(--error)' }}>
            <ShieldAlert className="h-10 w-10" style={{ color: 'var(--error)' }} />
          </div>
        </div>
        <h1 className="text-heading-1 mb-2" style={{ color: 'var(--text-primary)' }}>
          Access Denied
        </h1>
        <p className="text-body-md mb-8 max-w-sm" style={{ color: 'var(--text-secondary)' }}>
          You don&apos;t have permission to view this page.
          Contact your administrator to request access.
        </p>
        <a
          href="/dashboard"
          className="inline-flex items-center gap-2 rounded-lg px-6 py-2.5 text-body-md font-medium transition-all duration-200"
          style={{
            background: 'var(--accent-500)',
            color: '#ffffff',
          }}
        >
          Back to Dashboard
        </a>
      </div>
    </div>
  )
}
