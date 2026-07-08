/**
 * features/dashboard/DashboardPlaceholder.tsx
 *
 * Placeholder dashboard shown after login.
 * Will be replaced with the full Dashboard in Sprint 4.
 * Shows user info and logout button to verify auth works.
 */
import { useAuth } from '@/hooks/useAuth'
import { BarChart3, LogOut, User, Shield, Loader2 } from 'lucide-react'
import { getInitials } from '@/lib/utils'

export function DashboardPlaceholder() {
  const { user, logout, isLoading, isAdmin, isManager } = useAuth()

  return (
    <div
      className="min-h-screen p-8"
      style={{ background: 'var(--bg-base)' }}
    >
      <div className="mx-auto max-w-2xl animate-fade-in">

        {/* Header */}
        <div className="mb-8 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div
              className="flex h-10 w-10 items-center justify-center rounded-xl"
              style={{
                background: 'linear-gradient(135deg, var(--accent-600) 0%, var(--accent-400) 100%)',
              }}
            >
              <BarChart3 className="h-5 w-5 text-white" />
            </div>
            <div>
              <h1 className="text-heading-4" style={{ color: 'var(--text-primary)' }}>
                Commerce Insight AI
              </h1>
              <p className="text-caption" style={{ color: 'var(--text-muted)' }}>
                Dashboard
              </p>
            </div>
          </div>

          <button
            onClick={logout}
            disabled={isLoading}
            className="flex items-center gap-2 rounded-lg px-4 py-2 text-body-sm font-medium transition-all duration-200 hover:opacity-80"
            style={{
              background: 'var(--bg-surface)',
              border: '1px solid var(--border-default)',
              color: 'var(--text-secondary)',
            }}
          >
            {isLoading
              ? <Loader2 className="h-4 w-4 animate-spin" />
              : <LogOut className="h-4 w-4" />
            }
            Sign out
          </button>
        </div>

        {/* Welcome card */}
        <div
          className="rounded-2xl p-8 mb-6"
          style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--border-default)',
          }}
        >
          <div className="flex items-center gap-4 mb-6">
            <div
              className="flex h-16 w-16 items-center justify-center rounded-2xl text-heading-3 font-bold text-white"
              style={{
                background: 'linear-gradient(135deg, var(--accent-600) 0%, var(--accent-400) 100%)',
              }}
            >
              {user ? getInitials(user.firstName + ' ' + user.lastName) : '?'}
            </div>
            <div>
              <h2 className="text-heading-3" style={{ color: 'var(--text-primary)' }}>
                Welcome back, {user?.firstName}!
              </h2>
              <p className="text-body-sm" style={{ color: 'var(--text-secondary)' }}>
                {user?.email}
              </p>
            </div>
          </div>

          {/* User info grid */}
          <div className="grid grid-cols-2 gap-4">
            <InfoRow icon={User} label="Full Name" value={`${user?.firstName} ${user?.lastName}`} />
            <InfoRow icon={Shield} label="Role" value={user?.role ?? '—'} accent />
            <InfoRow label="User ID" value={user?.id?.slice(0, 8) + '...'} mono />
            <InfoRow
              label="Status"
              value={user?.active ? 'Active' : 'Inactive'}
              success={user?.active}
            />
          </div>
        </div>

        {/* Auth success banner */}
        <div
          className="rounded-xl px-5 py-4"
          style={{
            background: 'var(--success-bg)',
            border: '1px solid var(--success)',
          }}
        >
          <p className="text-body-sm font-medium" style={{ color: 'var(--success)' }}>
            ✓ Authentication module complete — Sprint 3 done!
          </p>
          <p className="text-caption mt-1" style={{ color: 'var(--text-secondary)' }}>
            JWT access token stored · Refresh token rotation active ·
            {isAdmin ? ' Admin access' : isManager ? ' Manager access' : ' Staff access'}
          </p>
        </div>
      </div>
    </div>
  )
}

function InfoRow({
  icon: Icon,
  label,
  value,
  accent,
  success,
  mono,
}: {
  icon?: React.ElementType
  label: string
  value: string | undefined
  accent?: boolean
  success?: boolean
  mono?: boolean
}) {
  return (
    <div
      className="rounded-lg px-4 py-3"
      style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-subtle)' }}
    >
      <div className="flex items-center gap-2 mb-1">
        {Icon && <Icon className="h-3.5 w-3.5" style={{ color: 'var(--text-muted)' }} />}
        <span className="text-caption" style={{ color: 'var(--text-muted)' }}>{label}</span>
      </div>
      <span
        className={mono ? 'text-code' : 'text-body-sm font-medium'}
        style={{
          color: accent
            ? 'var(--accent-400)'
            : success
            ? 'var(--success)'
            : 'var(--text-primary)',
        }}
      >
        {value ?? '—'}
      </span>
    </div>
  )
}
