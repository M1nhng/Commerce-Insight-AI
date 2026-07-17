/**
 * features/auth/components/UserAvatarMenu.tsx
 *
 * User avatar dropdown in the header.
 *
 * Contents:
 * - Avatar with initials + user name and email
 * - Link to profile page
 * - Theme toggle (Dark / Light / System)
 * - Logout button
 *
 * Built with Radix UI DropdownMenu for accessibility.
 */
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import * as DropdownMenu from '@radix-ui/react-dropdown-menu'
import {
  LogOut,
  User,
  Moon,
  Sun,
  Monitor,
  ChevronDown,
  Loader2,
  Shield,
} from 'lucide-react'
import { useAuth } from '@/hooks/useAuth'
import { useTheme } from '@/providers/ThemeProvider'
import { getInitials, cn } from '@/lib/utils'
import toast from 'react-hot-toast'
import type { Role } from '@/types/api.types'

// ── Role badge styles ────────────────────────────────────────────────────

const ROLE_STYLES: Record<Role, { bg: string; text: string; label: string }> = {
  ADMIN:   { bg: 'var(--error-bg)',   text: 'var(--error)',   label: 'Admin'   },
  MANAGER: { bg: 'var(--info-bg)',    text: 'var(--info)',    label: 'Manager' },
  STAFF:   { bg: 'var(--success-bg)', text: 'var(--success)', label: 'Staff'   },
}

// ── Component ─────────────────────────────────────────────────────────────

export function UserAvatarMenu() {
  const navigate = useNavigate()
  const { user, logout, isLoading } = useAuth()
  const { theme, setTheme } = useTheme()
  const [open, setOpen] = useState(false)

  const handleLogout = async () => {
    setOpen(false)
    try {
      await logout()
      toast.success('Signed out successfully')
    } catch {
      toast.error('Logout failed. Please try again.')
    }
  }

  if (!user) return null

  const initials = getInitials(user.fullName || `${user.firstName} ${user.lastName}`)
  const roleStyle = ROLE_STYLES[user.role] ?? ROLE_STYLES.STAFF

  return (
    <DropdownMenu.Root open={open} onOpenChange={setOpen}>
      {/* ── Trigger — Avatar button ────────────────────────────────── */}
      <DropdownMenu.Trigger asChild>
        <button
          className={cn(
            'flex items-center gap-2 rounded-lg px-2 py-1.5',
            'transition-all duration-150 outline-none',
            'hover:opacity-80',
          )}
          style={{
            background: open ? 'var(--bg-elevated)' : 'transparent',
            border: '1px solid',
            borderColor: open ? 'var(--border-strong)' : 'var(--border-default)',
          }}
          aria-label="Open user menu"
          aria-expanded={open}
        >
          {/* Avatar circle */}
          <div
            className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-caption font-bold text-white"
            style={{
              background: 'linear-gradient(135deg, var(--accent-600) 0%, var(--accent-400) 100%)',
            }}
          >
            {initials}
          </div>

          {/* Name — hidden on mobile */}
          <span
            className="hidden sm:block text-body-sm font-medium max-w-[120px] truncate"
            style={{ color: 'var(--text-primary)' }}
          >
            {user.firstName}
          </span>

          <ChevronDown
            className={cn(
              'hidden sm:block h-3.5 w-3.5 shrink-0 transition-transform duration-200',
              open && 'rotate-180'
            )}
            style={{ color: 'var(--text-muted)' }}
          />
        </button>
      </DropdownMenu.Trigger>

      {/* ── Dropdown Content ───────────────────────────────────────── */}
      <DropdownMenu.Portal>
        <DropdownMenu.Content
          align="end"
          sideOffset={8}
          className="z-50 w-64 rounded-xl p-1 shadow-2xl outline-none animate-fade-in"
          style={{
            background: 'var(--bg-elevated)',
            border: '1px solid var(--border-default)',
            boxShadow: '0 16px 48px rgba(0,0,0,0.5)',
          }}
        >
          {/* ── User Info Header ──────────────────────────────────── */}
          <div
            className="px-3 py-3 mb-1 rounded-lg"
            style={{ background: 'var(--bg-overlay)' }}
          >
            <div className="flex items-center gap-3">
              {/* Large avatar */}
              <div
                className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-body-sm font-bold text-white"
                style={{
                  background: 'linear-gradient(135deg, var(--accent-600) 0%, var(--accent-400) 100%)',
                }}
              >
                {initials}
              </div>

              <div className="min-w-0">
                <p
                  className="text-body-sm font-semibold truncate"
                  style={{ color: 'var(--text-primary)' }}
                >
                  {user.fullName || `${user.firstName} ${user.lastName}`}
                </p>
                <p
                  className="text-caption truncate"
                  style={{ color: 'var(--text-secondary)' }}
                >
                  {user.email}
                </p>
              </div>
            </div>

            {/* Role badge */}
            <div className="mt-2">
              <span
                className="inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-caption font-medium"
                style={{
                  background: roleStyle.bg,
                  color: roleStyle.text,
                }}
              >
                <Shield className="h-3 w-3" />
                {roleStyle.label}
              </span>
            </div>
          </div>

          {/* ── Profile Link ─────────────────────────────────────── */}
          <DropdownMenu.Item asChild>
            <button
              onClick={() => { setOpen(false); navigate('/profile') }}
              className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-body-sm outline-none transition-colors duration-100"
              style={{ color: 'var(--text-secondary)' }}
              onMouseEnter={(e) => {
                e.currentTarget.style.background = 'var(--bg-overlay)'
                e.currentTarget.style.color = 'var(--text-primary)'
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = 'transparent'
                e.currentTarget.style.color = 'var(--text-secondary)'
              }}
            >
              <User className="h-4 w-4 shrink-0" />
              My Profile
            </button>
          </DropdownMenu.Item>

          {/* ── Separator ────────────────────────────────────────── */}
          <DropdownMenu.Separator
            className="my-1 h-px"
            style={{ background: 'var(--border-subtle)' }}
          />

          {/* ── Theme Toggle ─────────────────────────────────────── */}
          <div className="px-3 py-1.5">
            <p
              className="mb-1.5 text-caption font-semibold tracking-wide"
              style={{ color: 'var(--text-muted)' }}
            >
              APPEARANCE
            </p>
            <div
              className="flex rounded-lg p-0.5"
              style={{ background: 'var(--bg-overlay)' }}
            >
              {(
                [
                  { value: 'dark',   icon: Moon,    label: 'Dark'   },
                  { value: 'light',  icon: Sun,     label: 'Light'  },
                  { value: 'system', icon: Monitor, label: 'System' },
                ] as const
              ).map(({ value, icon: Icon, label }) => (
                <button
                  key={value}
                  onClick={() => setTheme(value)}
                  className="flex flex-1 items-center justify-center gap-1 rounded-md py-1.5 text-caption font-medium transition-all duration-150"
                  style={
                    theme === value
                      ? {
                          background: 'var(--bg-elevated)',
                          color: 'var(--accent-400)',
                          boxShadow: '0 1px 4px rgba(0,0,0,0.3)',
                        }
                      : { color: 'var(--text-muted)' }
                  }
                  aria-pressed={theme === value}
                  aria-label={`Switch to ${label} theme`}
                >
                  <Icon className="h-3.5 w-3.5" />
                  <span className="hidden sm:inline">{label}</span>
                </button>
              ))}
            </div>
          </div>

          {/* ── Separator ────────────────────────────────────────── */}
          <DropdownMenu.Separator
            className="my-1 h-px"
            style={{ background: 'var(--border-subtle)' }}
          />

          {/* ── Logout ───────────────────────────────────────────── */}
          <DropdownMenu.Item asChild>
            <button
              onClick={handleLogout}
              disabled={isLoading}
              className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-body-sm outline-none transition-all duration-100 disabled:opacity-50"
              style={{ color: 'var(--error)' }}
              onMouseEnter={(e) => {
                if (!isLoading) e.currentTarget.style.background = 'var(--error-bg)'
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = 'transparent'
              }}
            >
              {isLoading
                ? <Loader2 className="h-4 w-4 shrink-0 animate-spin" />
                : <LogOut className="h-4 w-4 shrink-0" />
              }
              {isLoading ? 'Signing out...' : 'Sign out'}
            </button>
          </DropdownMenu.Item>
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  )
}
