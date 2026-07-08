/**
 * features/auth/pages/LoginPage.tsx
 *
 * Design spec (09_UI_UX.md §12.1):
 * - Full viewport, centered card (max-width: 400px)
 * - Dark background with subtle radial gradient
 * - Logo at top, product name
 * - Email + Password inputs with validation
 * - "Forgot password?" link (placeholder)
 * - Submit button full width with loading state
 * - Redirects authenticated users to dashboard
 */
import { useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/store/auth.store'
import { LoginForm } from '@/features/auth/components/LoginForm'
import { BarChart3 } from 'lucide-react'

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const isInitializing = useAuthStore((s) => s.isInitializing)

  // Redirect already-authenticated users
  useEffect(() => {
    if (!isInitializing && isAuthenticated) {
      const from = (location.state as { from?: Location })?.from?.pathname ?? '/dashboard'
      navigate(from, { replace: true })
    }
  }, [isAuthenticated, isInitializing, navigate, location])

  return (
    <div
      className="bg-auth flex min-h-screen items-center justify-center p-4"
      aria-label="Commerce Insight AI Login"
    >
      <div className="w-full max-w-[400px] animate-fade-in">

        {/* ── Logo + Branding ──────────────────────────────────────────── */}
        <div className="mb-8 text-center">
          <div className="mb-4 inline-flex h-14 w-14 items-center justify-center rounded-2xl"
               style={{
                 background: 'linear-gradient(135deg, var(--accent-600) 0%, var(--accent-400) 100%)',
                 boxShadow: '0 8px 32px rgba(99, 102, 241, 0.35)',
               }}>
            <BarChart3 className="h-7 w-7 text-white" />
          </div>

          <h1 className="text-heading-2 mb-1" style={{ color: 'var(--text-primary)' }}>
            Commerce Insight AI
          </h1>
          <p className="text-body-sm" style={{ color: 'var(--text-secondary)' }}>
            Sign in to your workspace
          </p>
        </div>

        {/* ── Login Card ────────────────────────────────────────────────── */}
        <div
          className="rounded-2xl p-8"
          style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--border-default)',
            boxShadow: '0 24px 64px rgba(0, 0, 0, 0.4)',
          }}
        >
          <LoginForm />
        </div>

        {/* ── Footer ───────────────────────────────────────────────────── */}
        <p className="mt-6 text-center text-caption" style={{ color: 'var(--text-muted)' }}>
          Commerce Insight AI &copy; {new Date().getFullYear()}
        </p>
      </div>
    </div>
  )
}
