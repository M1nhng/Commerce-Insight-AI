/**
 * features/customers/components/CustomerStatusBadge.tsx
 * Status badge extended for customer-specific statuses.
 */
import { cn } from '@/lib/utils'
import type { CustomerStatus, GroupStatus } from '@/types/customer.types'

type BadgeStatus = CustomerStatus | GroupStatus

const STYLES: Record<BadgeStatus, { label: string; style: React.CSSProperties }> = {
  ACTIVE:   { label: 'Active',   style: { background: 'var(--success-bg)', color: 'var(--success)' } },
  INACTIVE: { label: 'Inactive', style: { background: 'var(--bg-overlay)', color: 'var(--text-muted)' } },
  BLOCKED:  { label: 'Blocked',  style: { background: 'var(--error-bg)',   color: 'var(--error)' } },
}

interface CustomerStatusBadgeProps {
  status: BadgeStatus
  className?: string
}

export function CustomerStatusBadge({ status, className }: CustomerStatusBadgeProps) {
  const config = STYLES[status] ?? STYLES.INACTIVE
  return (
    <span
      className={cn('font-medium text-xs uppercase tracking-wide px-2.5 py-1 rounded-md', className)}
      style={config.style}
    >
      {config.label}
    </span>
  )
}
