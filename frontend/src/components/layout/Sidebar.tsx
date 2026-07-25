/**
 * components/layout/Sidebar.tsx
 *
 * Responsive collapsible navigation sidebar.
 *
 * Design spec (09_UI_UX.md §5.2–5.3):
 * - Desktop: Always visible, collapsible to 64px icon-only mode
 * - Tablet: Collapsed by default, toggle expands as overlay
 * - Mobile: Hidden, accessible via hamburger as full-width drawer
 *
 * RBAC: Admin-only items hidden from STAFF/MANAGER roles.
 */
import { NavLink, useLocation } from 'react-router-dom'
import {
  BarChart3,
  LayoutDashboard,
  Package,
  Tags,
  Users,
  ShoppingCart,
  Warehouse,
  Building2,
  TrendingUp,
  Bot,
  Download,
  Upload,
  Settings,
  Shield,
  ChevronLeft,
  ChevronRight,
  X,
} from 'lucide-react'
import { cn } from '@/lib/utils'
import { useAuth } from '@/hooks/useAuth'

// ── Nav item definitions ──────────────────────────────────────────────────

interface NavItem {
  label: string
  icon: React.ElementType
  to: string
  adminOnly?: boolean
}

interface NavGroup {
  section: string
  items: NavItem[]
}

const NAV_GROUPS: NavGroup[] = [
  {
    section: 'OVERVIEW',
    items: [
      { label: 'Dashboard', icon: LayoutDashboard, to: '/dashboard' },
    ],
  },
  {
    section: 'OPERATIONS',
    items: [
      { label: 'Products', icon: Package, to: '/products' },
      { label: 'Categories', icon: Tags, to: '/categories' },
      { label: 'Customers',   icon: Users,        to: '/customers' },
      { label: 'Orders',      icon: ShoppingCart, to: '/orders' },
      { label: 'Inventory',   icon: Warehouse,    to: '/inventory' },
      { label: 'Warehouses',  icon: Building2,    to: '/warehouses' },
    ],
  },
  {
    section: 'INTELLIGENCE',
    items: [
      { label: 'Analytics', icon: TrendingUp, to: '/analytics' },
      { label: 'AI Assistant', icon: Bot, to: '/ai' },
    ],
  },
  {
    section: 'DATA',
    items: [
      { label: 'Import', icon: Download, to: '/import' },
      { label: 'Export', icon: Upload, to: '/export' },
    ],
  },
  {
    section: 'SYSTEM',
    items: [
      { label: 'Settings', icon: Settings, to: '/settings' },
      { label: 'Admin', icon: Shield, to: '/admin', adminOnly: true },
    ],
  },
]

// ── Props ─────────────────────────────────────────────────────────────────

interface SidebarProps {
  collapsed: boolean
  onToggle: () => void
  mobileOpen: boolean
  onMobileClose: () => void
}

// ── Component ─────────────────────────────────────────────────────────────

export function Sidebar({ collapsed, onToggle, mobileOpen, onMobileClose }: SidebarProps) {
  const { isAdmin } = useAuth()
  const location = useLocation()

  const isActive = (to: string) =>
    location.pathname === to || location.pathname.startsWith(to + '/')

  return (
    <>
      {/* ── Mobile Backdrop ─────────────────────────────────────────── */}
      {mobileOpen && (
        <div
          className="fixed inset-0 z-40 lg:hidden"
          style={{ background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(4px)' }}
          onClick={onMobileClose}
          aria-hidden
        />
      )}

      {/* ── Sidebar Panel ───────────────────────────────────────────── */}
      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-50 flex flex-col',
          'transition-all duration-300 ease-in-out',
          // Desktop
          'lg:relative lg:z-auto lg:translate-x-0',
          collapsed ? 'lg:w-16' : 'lg:w-60',
          // Mobile
          mobileOpen ? 'translate-x-0 w-64' : '-translate-x-full lg:translate-x-0'
        )}
        style={{
          background: 'var(--bg-surface)',
          borderRight: '1px solid var(--border-default)',
        }}
        aria-label="Main navigation"
      >
        {/* ── Logo ─────────────────────────────────────────────────── */}
        <div
          className={cn(
            'flex h-16 shrink-0 items-center px-4',
            'border-b',
          )}
          style={{ borderColor: 'var(--border-default)' }}
        >
          <div
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg"
            style={{
              background: 'linear-gradient(135deg, var(--accent-600) 0%, var(--accent-400) 100%)',
              boxShadow: '0 4px 12px rgba(99,102,241,0.35)',
            }}
          >
            <BarChart3 className="h-4 w-4 text-white" />
          </div>

          {!collapsed && (
            <span
              className="ml-3 truncate text-body-sm font-semibold tracking-tight"
              style={{ color: 'var(--text-primary)' }}
            >
              Commerce Insight AI
            </span>
          )}

          {/* Mobile close button */}
          <button
            onClick={onMobileClose}
            className="ml-auto lg:hidden rounded-md p-1 transition-colors hover:opacity-70"
            style={{ color: 'var(--text-muted)' }}
            aria-label="Close sidebar"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* ── Nav Items ────────────────────────────────────────────── */}
        <nav className="flex-1 overflow-y-auto py-4 px-2" aria-label="Sidebar navigation">
          {NAV_GROUPS.map((group) => {
            const visibleItems = group.items.filter(
              (item) => !item.adminOnly || isAdmin
            )
            if (visibleItems.length === 0) return null

            return (
              <div key={group.section} className="mb-4">
                {/* Section label — hidden when collapsed */}
                {!collapsed && (
                  <p
                    className="mb-1 px-2 text-caption font-semibold tracking-widest"
                    style={{ color: 'var(--text-muted)' }}
                  >
                    {group.section}
                  </p>
                )}

                {/* Separator line when collapsed */}
                {collapsed && (
                  <div
                    className="mx-2 mb-2 h-px"
                    style={{ background: 'var(--border-subtle)' }}
                  />
                )}

                {visibleItems.map((item) => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    onClick={onMobileClose}
                    title={collapsed ? item.label : undefined}
                    className={cn(
                      'group flex items-center gap-3 rounded-lg px-2 py-2 mb-0.5',
                      'text-body-sm font-medium transition-all duration-150',
                      collapsed && 'justify-center'
                    )}
                    style={
                      isActive(item.to)
                        ? {
                            background: 'rgba(99,102,241,0.12)',
                            color: 'var(--accent-400)',
                          }
                        : {
                            color: 'var(--text-secondary)',
                          }
                    }
                    aria-current={isActive(item.to) ? 'page' : undefined}
                  >
                    {({ isActive: navActive }) => (
                      <>
                        <item.icon
                          className={cn(
                            'h-4 w-4 shrink-0 transition-colors duration-150',
                            navActive
                              ? 'text-[var(--accent-400)]'
                              : 'text-[var(--text-muted)] group-hover:text-[var(--text-secondary)]'
                          )}
                        />
                        {!collapsed && (
                          <span className="truncate">{item.label}</span>
                        )}

                        {/* Active indicator bar */}
                        {navActive && !collapsed && (
                          <span
                            className="ml-auto h-1.5 w-1.5 rounded-full shrink-0"
                            style={{ background: 'var(--accent-400)' }}
                          />
                        )}
                      </>
                    )}
                  </NavLink>
                ))}
              </div>
            )
          })}
        </nav>

        {/* ── Collapse Toggle (Desktop only) ───────────────────────── */}
        <div
          className="hidden lg:flex shrink-0 items-center border-t px-2 py-3"
          style={{ borderColor: 'var(--border-default)' }}
        >
          <button
            onClick={onToggle}
            className={cn(
              'flex items-center gap-2 rounded-lg px-2 py-2 w-full',
              'text-caption transition-all duration-150 hover:opacity-80',
              collapsed && 'justify-center'
            )}
            style={{
              color: 'var(--text-muted)',
              background: 'var(--bg-elevated)',
            }}
            aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            {collapsed
              ? <ChevronRight className="h-4 w-4" />
              : (
                <>
                  <ChevronLeft className="h-4 w-4" />
                  <span>Collapse</span>
                </>
              )
            }
          </button>
        </div>
      </aside>
    </>
  )
}
