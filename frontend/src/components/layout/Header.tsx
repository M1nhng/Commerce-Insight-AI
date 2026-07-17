/**
 * components/layout/Header.tsx
 *
 * Top application header (64px height).
 *
 * Design spec (09_UI_UX.md §5.4):
 * Left:   [≡ Sidebar Toggle] [Breadcrumb path]
 * Right:  [UserAvatarMenu → Profile / Theme / Logout]
 */
import { useLocation } from 'react-router-dom'
import { Menu } from 'lucide-react'
import { UserAvatarMenu } from '@/features/auth/components/UserAvatarMenu'

// ── Route label map ──────────────────────────────────────────────────────

const ROUTE_LABELS: Record<string, string> = {
  '/dashboard':  'Dashboard',
  '/products':   'Products',
  '/categories': 'Categories',
  '/customers':  'Customers',
  '/orders':     'Orders',
  '/inventory':  'Inventory',
  '/analytics':  'Analytics',
  '/ai':         'AI Assistant',
  '/import':     'Import',
  '/export':     'Export',
  '/settings':   'Settings',
  '/admin':      'Admin',
  '/profile':    'My Profile',
}

function usePageTitle(): string {
  const { pathname } = useLocation()
  // Match exact or prefix
  const match = Object.keys(ROUTE_LABELS).find(
    (key) => pathname === key || pathname.startsWith(key + '/')
  )
  return match ? ROUTE_LABELS[match] : 'Commerce Insight AI'
}

// ── Props ─────────────────────────────────────────────────────────────────

interface HeaderProps {
  onMobileMenuOpen: () => void
}

// ── Component ─────────────────────────────────────────────────────────────

export function Header({ onMobileMenuOpen }: HeaderProps) {
  const pageTitle = usePageTitle()

  return (
    <header
      className="flex h-16 shrink-0 items-center gap-4 px-4 lg:px-6"
      style={{
        background: 'var(--bg-surface)',
        borderBottom: '1px solid var(--border-default)',
      }}
    >
      {/* ── Mobile Sidebar Toggle ─────────────────────────────────── */}
      <button
        onClick={onMobileMenuOpen}
        className="flex h-8 w-8 items-center justify-center rounded-lg transition-colors duration-150 lg:hidden"
        style={{ color: 'var(--text-secondary)' }}
        aria-label="Open navigation menu"
      >
        <Menu className="h-5 w-5" />
      </button>

      {/* ── Page Title / Breadcrumb ───────────────────────────────── */}
      <div className="flex-1 min-w-0">
        <h1
          className="text-heading-4 truncate"
          style={{ color: 'var(--text-primary)' }}
        >
          {pageTitle}
        </h1>
      </div>

      {/* ── Right — User Avatar Menu ──────────────────────────────── */}
      <div className="flex items-center gap-2">
        <UserAvatarMenu />
      </div>
    </header>
  )
}
