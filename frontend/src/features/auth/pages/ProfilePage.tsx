/**
 * features/auth/pages/ProfilePage.tsx
 *
 * Current user's profile page.
 *
 * Shows:
 * - Large avatar with initials
 * - Full name, email, role badge
 * - Account status (active/locked/failed attempts)
 * - Last login time
 * - "Refresh profile" button using TanStack Query
 */
import { useCurrentUser } from '@/hooks/useCurrentUser'
import { useAuthStore } from '@/store/auth.store'
import {
  User,
  Mail,
  Shield,
  Calendar,
  Clock,
  RefreshCw,
  Lock,
  CheckCircle2,
  AlertTriangle,
} from 'lucide-react'
import { getInitials } from '@/lib/utils'
import { format } from 'date-fns'
import type { Role } from '@/types/api.types'

// ── Role badge ─────────────────────────────────────────────────────────────

const ROLE_CONFIG: Record<Role, { label: string; bg: string; text: string }> = {
  ADMIN:   { label: 'Administrator', bg: 'var(--error-bg)',   text: 'var(--error)'   },
  MANAGER: { label: 'Manager',       bg: 'var(--info-bg)',    text: 'var(--info)'    },
  STAFF:   { label: 'Staff',         bg: 'var(--success-bg)', text: 'var(--success)' },
}

// ── Component ─────────────────────────────────────────────────────────────

export function ProfilePage() {
  const user = useAuthStore((s) => s.user)
  const { refetch, isFetching } = useCurrentUser()

  if (!user) return null

  const initials = getInitials(user.fullName || `${user.firstName} ${user.lastName}`)
  const role = ROLE_CONFIG[user.role] ?? ROLE_CONFIG.STAFF

  const formatDate = (dateStr: string | null) => {
    if (!dateStr) return '—'
    try {
      return format(new Date(dateStr), 'PPP p')
    } catch {
      return '—'
    }
  }

  return (
    <div className="mx-auto max-w-2xl animate-fade-in">

      {/* ── Page Header ─────────────────────────────────────────────── */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1
            className="text-heading-2"
            style={{ color: 'var(--text-primary)' }}
          >
            My Profile
          </h1>
          <p
            className="text-body-sm mt-1"
            style={{ color: 'var(--text-secondary)' }}
          >
            Your account information and settings
          </p>
        </div>

        <button
          onClick={() => refetch()}
          disabled={isFetching}
          className="flex items-center gap-2 rounded-lg px-4 py-2 text-body-sm font-medium transition-all duration-200 hover:opacity-80 disabled:opacity-50"
          style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--border-default)',
            color: 'var(--text-secondary)',
          }}
        >
          <RefreshCw
            className={`h-4 w-4 ${isFetching ? 'animate-spin' : ''}`}
          />
          {isFetching ? 'Refreshing...' : 'Refresh'}
        </button>
      </div>

      {/* ── Profile Card ────────────────────────────────────────────── */}
      <div
        className="rounded-2xl overflow-hidden"
        style={{
          background: 'var(--bg-surface)',
          border: '1px solid var(--border-default)',
        }}
      >
        {/* Avatar banner */}
        <div
          className="h-24 w-full"
          style={{
            background: 'linear-gradient(135deg, var(--accent-700) 0%, var(--accent-500) 50%, var(--accent-400) 100%)',
          }}
        />

        <div className="px-6 pb-6">
          <div className="flex items-end gap-4 -mt-10 mb-6">
            <div
              className="flex h-20 w-20 shrink-0 items-center justify-center rounded-2xl text-heading-3 font-bold text-white"
              style={{
                background: 'linear-gradient(135deg, var(--accent-600) 0%, var(--accent-400) 100%)',
                boxShadow: '0 0 0 4px var(--bg-surface)',
              }}
            >
              {initials}
            </div>

            <div className="pb-1">
              <h2
                className="text-heading-3"
                style={{ color: 'var(--text-primary)' }}
              >
                {user.fullName || `${user.firstName} ${user.lastName}`}
              </h2>

              {/* Role badge */}
              <span
                className="mt-1 inline-flex items-center gap-1 rounded-md px-2.5 py-0.5 text-caption font-semibold"
                style={{ background: role.bg, color: role.text }}
              >
                <Shield className="h-3 w-3" />
                {role.label}
              </span>
            </div>
          </div>

          {/* ── Info Grid ─────────────────────────────────────────── */}
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <ProfileField
              icon={User}
              label="First Name"
              value={user.firstName}
            />
            <ProfileField
              icon={User}
              label="Last Name"
              value={user.lastName}
            />
            <ProfileField
              icon={Mail}
              label="Email Address"
              value={user.email}
              className="sm:col-span-2"
            />
            <ProfileField
              icon={Shield}
              label="User ID"
              value={user.id}
              mono
            />
            <ProfileField
              icon={Clock}
              label="Last Login"
              value={formatDate(user.lastLoginAt)}
            />
            <ProfileField
              icon={Calendar}
              label="Member Since"
              value={formatDate(user.createdAt)}
            />
          </div>
        </div>
      </div>

      {/* ── Account Status Card ─────────────────────────────────────── */}
      <div
        className="mt-4 rounded-2xl p-5"
        style={{
          background: 'var(--bg-surface)',
          border: '1px solid var(--border-default)',
        }}
      >
        <h3
          className="text-heading-4 mb-4"
          style={{ color: 'var(--text-primary)' }}
        >
          Account Status
        </h3>

        <div className="flex flex-wrap gap-3">
          {/* Active status */}
          <StatusBadge
            icon={user.active ? CheckCircle2 : AlertTriangle}
            label={user.active ? 'Active Account' : 'Inactive Account'}
            ok={user.active}
          />

          {/* Lock status */}
          <StatusBadge
            icon={Lock}
            label={user.locked ? 'Account Locked' : 'Account Unlocked'}
            ok={!user.locked}
          />
        </div>
      </div>
    </div>
  )
}

// ── Sub-components ────────────────────────────────────────────────────────

function ProfileField({
  icon: Icon,
  label,
  value,
  mono,
  className,
}: {
  icon: React.ElementType
  label: string
  value: string
  mono?: boolean
  className?: string
}) {
  return (
    <div
      className={`rounded-xl px-4 py-3 ${className ?? ''}`}
      style={{
        background: 'var(--bg-elevated)',
        border: '1px solid var(--border-subtle)',
      }}
    >
      <div className="mb-1 flex items-center gap-1.5">
        <Icon className="h-3.5 w-3.5" style={{ color: 'var(--text-muted)' }} />
        <span
          className="text-caption"
          style={{ color: 'var(--text-muted)' }}
        >
          {label}
        </span>
      </div>
      <p
        className={mono ? 'text-code' : 'text-body-sm font-medium'}
        style={{ color: 'var(--text-primary)', wordBreak: 'break-all' }}
      >
        {value || '—'}
      </p>
    </div>
  )
}

function StatusBadge({
  icon: Icon,
  label,
  ok,
}: {
  icon: React.ElementType
  label: string
  ok: boolean
}) {
  return (
    <span
      className="inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-body-sm font-medium"
      style={{
        background: ok ? 'var(--success-bg)' : 'var(--error-bg)',
        color: ok ? 'var(--success)' : 'var(--error)',
        border: `1px solid ${ok ? 'var(--success)' : 'var(--error)'}`,
      }}
    >
      <Icon className="h-4 w-4" />
      {label}
    </span>
  )
}
