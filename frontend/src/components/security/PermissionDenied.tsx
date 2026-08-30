/**
 * components/security/PermissionDenied.tsx
 *
 * Reusable "you do not have permission" surface. Used by the route-level
 * RoleGuard and available for in-page permission failures (e.g. after a 403
 * from the API). Design matches the original RoleGuard.ForbiddenPage.
 *
 * A 403 is NOT an authentication failure — this component never logs the user
 * out and never triggers a token refresh.
 */
import { ShieldAlert } from 'lucide-react'

interface PermissionDeniedProps {
  /** Overrides the default body copy. */
  message?: string
  /** Where the "back" link points. Defaults to the dashboard. */
  backTo?: string
  backLabel?: string
  /** Optional backend correlation id for support (X-Request-Id). */
  requestId?: string
  /** Render inside a card instead of a full screen. */
  inline?: boolean
}

const DEFAULT_MESSAGE =
  'You do not have permission to perform this action. Contact your administrator to request access.'

export function PermissionDenied({
  message = DEFAULT_MESSAGE,
  backTo = '/dashboard',
  backLabel = 'Back to Dashboard',
  requestId,
  inline = false,
}: PermissionDeniedProps) {
  return (
    <div
      className={
        inline
          ? 'flex w-full items-center justify-center py-16'
          : 'flex min-h-screen items-center justify-center'
      }
      style={inline ? undefined : { background: 'var(--bg-base)' }}
    >
      <div className="animate-fade-in text-center">
        <div className="mb-6 flex justify-center">
          <div
            className="flex h-20 w-20 items-center justify-center rounded-2xl"
            style={{ background: 'var(--error-bg)', border: '1px solid var(--error)' }}
          >
            <ShieldAlert className="h-10 w-10" style={{ color: 'var(--error)' }} />
          </div>
        </div>
        <h1 className="text-heading-1 mb-2" style={{ color: 'var(--text-primary)' }}>
          Access Denied
        </h1>
        <p
          className="text-body-md mb-8 max-w-sm"
          style={{ color: 'var(--text-secondary)' }}
        >
          {message}
        </p>
        {requestId && (
          <p
            className="text-caption mb-6"
            style={{ color: 'var(--text-muted)' }}
          >
            Reference ID: {requestId}
          </p>
        )}
        <a
          href={backTo}
          className="text-body-md inline-flex items-center gap-2 rounded-lg px-6 py-2.5 font-medium transition-all duration-200"
          style={{ background: 'var(--accent-500)', color: '#ffffff' }}
        >
          {backLabel}
        </a>
      </div>
    </div>
  )
}
