/**
 * components/ui/AppLoader.tsx — Full-screen initialization loader.
 *
 * Shown while the app is checking stored auth tokens on startup.
 * Uses a subtle pulsing logo — NOT a spinner.
 */
import { BarChart3 } from 'lucide-react'

export function AppLoader() {
  return (
    <div
      className="flex min-h-screen items-center justify-center"
      style={{ background: 'var(--bg-base)' }}
      role="status"
      aria-label="Loading Commerce Insight AI"
    >
      <div className="flex flex-col items-center gap-4">
        {/* Pulsing logo mark */}
        <div
          className="flex h-16 w-16 items-center justify-center rounded-2xl animate-pulse"
          style={{
            background: 'linear-gradient(135deg, var(--accent-600) 0%, var(--accent-400) 100%)',
            boxShadow: '0 0 40px rgba(99, 102, 241, 0.3)',
          }}
        >
          <BarChart3 className="h-8 w-8 text-white" />
        </div>

        {/* Loading dots */}
        <div className="flex gap-1.5">
          {[0, 1, 2].map((i) => (
            <div
              key={i}
              className="h-1.5 w-1.5 rounded-full"
              style={{
                background: 'var(--accent-500)',
                animation: `pulse 1.2s ease-in-out ${i * 0.2}s infinite`,
              }}
            />
          ))}
        </div>
      </div>
    </div>
  )
}
