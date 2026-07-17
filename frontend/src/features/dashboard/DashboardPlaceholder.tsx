/**
 * features/dashboard/DashboardPlaceholder.tsx
 *
 * Placeholder dashboard shown after login.
 * Confirms auth session is working while the full dashboard is built in Sprint 6.
 * Lives inside AppShell (has sidebar + header already), so minimal layout here.
 */
import { useAuth } from '@/hooks/useAuth'
import { User, Shield, CheckCircle2, Lock, BarChart3 } from 'lucide-react'
import { getInitials } from '@/lib/utils'

export function DashboardPlaceholder() {
  const { user, isAdmin, isManager } = useAuth()

  return (
    <div className="animate-fade-in">

      {/* ── Welcome banner ──────────────────────────────────────────── */}
      <div
        className="mb-6 flex items-center gap-4 rounded-2xl p-6"
        style={{
          background: 'linear-gradient(135deg, rgba(99,102,241,0.12) 0%, rgba(167,139,250,0.06) 100%)',
          border: '1px solid var(--border-default)',
        }}
      >
        {/* Avatar */}
        <div
          className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl text-heading-3 font-bold text-white"
          style={{
            background: 'linear-gradient(135deg, var(--accent-600) 0%, var(--accent-400) 100%)',
            boxShadow: '0 8px 24px rgba(99,102,241,0.3)',
          }}
        >
          {user ? getInitials(`${user.firstName} ${user.lastName}`) : '?'}
        </div>

        <div>
          <h2
            className="text-heading-3"
            style={{ color: 'var(--text-primary)' }}
          >
            Welcome back, {user?.firstName}!
          </h2>
          <p
            className="text-body-sm mt-0.5"
            style={{ color: 'var(--text-secondary)' }}
          >
            {isAdmin ? 'Administrator' : isManager ? 'Manager' : 'Staff'} ·{' '}
            {user?.email}
          </p>
        </div>
      </div>

      {/* ── Info cards grid ─────────────────────────────────────────── */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4 mb-6">
        <InfoCard icon={User} label="Full Name" value={`${user?.firstName} ${user?.lastName}`} />
        <InfoCard icon={Shield} label="Role" value={user?.role ?? '—'} accent />
        <InfoCard icon={CheckCircle2} label="Status" value={user?.active ? 'Active' : 'Inactive'} success={user?.active} />
        <InfoCard icon={Lock} label="Account" value={user?.locked ? 'Locked' : 'Unlocked'} success={!user?.locked} />
      </div>

      {/* ── Sprint status banner ─────────────────────────────────────── */}
      <div
        className="rounded-xl px-5 py-4"
        style={{
          background: 'var(--success-bg)',
          border: '1px solid rgba(34,197,94,0.3)',
        }}
      >
        <div className="flex items-center gap-2 mb-1">
          <BarChart3 className="h-4 w-4" style={{ color: 'var(--success)' }} />
          <p className="text-body-sm font-semibold" style={{ color: 'var(--success)' }}>
            ✓ Sprint 5 complete — Authentication + App Shell
          </p>
        </div>
        <p className="text-caption" style={{ color: 'var(--text-secondary)' }}>
          JWT access token · Refresh token rotation · RBAC guards ·
          Responsive sidebar · Dark/light theme
        </p>
      </div>
    </div>
  )
}

// ── Sub-component ─────────────────────────────────────────────────────────

function InfoCard({
  icon: Icon,
  label,
  value,
  accent,
  success,
}: {
  icon: React.ElementType
  label: string
  value: string | undefined
  accent?: boolean
  success?: boolean
}) {
  return (
    <div
      className="rounded-xl px-4 py-4"
      style={{
        background: 'var(--bg-surface)',
        border: '1px solid var(--border-default)',
      }}
    >
      <div className="flex items-center gap-2 mb-2">
        <Icon className="h-4 w-4" style={{ color: 'var(--text-muted)' }} />
        <span className="text-caption" style={{ color: 'var(--text-muted)' }}>
          {label}
        </span>
      </div>
      <span
        className="text-body-sm font-semibold"
        style={{
          color: accent
            ? 'var(--accent-400)'
            : success === true
            ? 'var(--success)'
            : success === false
            ? 'var(--error)'
            : 'var(--text-primary)',
        }}
      >
        {value ?? '—'}
      </span>
    </div>
  )
}
