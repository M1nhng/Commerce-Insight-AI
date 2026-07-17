/**
 * components/layout/AppShell.tsx
 *
 * Root authenticated application layout.
 *
 * Structure:
 *   <div flex h-screen>
 *     <Sidebar />          — fixed left panel
 *     <div flex-col flex-1>
 *       <Header />         — 64px top bar
 *       <main scrollable>  — page content
 *         <Outlet />
 *       </main>
 *     </div>
 *   </div>
 *
 * State:
 * - sidebarCollapsed: desktop collapse (persisted to localStorage)
 * - mobileOpen: mobile drawer open state
 */
import { useState, useEffect } from 'react'
import { Outlet } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import { Header } from './Header'

const SIDEBAR_STATE_KEY = 'cia-sidebar-collapsed'

export function AppShell() {
  // ── Sidebar state ─────────────────────────────────────────────────────
  const [collapsed, setCollapsed] = useState<boolean>(() => {
    return localStorage.getItem(SIDEBAR_STATE_KEY) === 'true'
  })
  const [mobileOpen, setMobileOpen] = useState(false)

  // Persist collapse preference
  useEffect(() => {
    localStorage.setItem(SIDEBAR_STATE_KEY, String(collapsed))
  }, [collapsed])

  // Close mobile sidebar on resize to desktop
  useEffect(() => {
    const mq = window.matchMedia('(min-width: 1024px)')
    const handler = (e: MediaQueryListEvent) => {
      if (e.matches) setMobileOpen(false)
    }
    mq.addEventListener('change', handler)
    return () => mq.removeEventListener('change', handler)
  }, [])

  return (
    <div
      className="flex h-screen overflow-hidden"
      style={{ background: 'var(--bg-base)' }}
    >
      {/* ── Sidebar ──────────────────────────────────────────────────── */}
      <Sidebar
        collapsed={collapsed}
        onToggle={() => setCollapsed((c) => !c)}
        mobileOpen={mobileOpen}
        onMobileClose={() => setMobileOpen(false)}
      />

      {/* ── Main Column ──────────────────────────────────────────────── */}
      <div className="flex flex-1 flex-col min-w-0 overflow-hidden">
        {/* ── Header ─────────────────────────────────────────────────── */}
        <Header onMobileMenuOpen={() => setMobileOpen(true)} />

        {/* ── Page Content ───────────────────────────────────────────── */}
        <main
          className="flex-1 overflow-y-auto p-4 lg:p-6"
          id="main-content"
        >
          {/* Max content width per design spec §7.3 */}
          <div className="mx-auto w-full max-w-[1440px]">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
