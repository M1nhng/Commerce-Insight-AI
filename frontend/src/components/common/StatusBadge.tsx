/**
 * components/common/StatusBadge.tsx
 * Reusable status badge following design spec §10.2
 */
import { cn } from '@/lib/utils'

type Status = 'active' | 'inactive' | 'pending' | 'confirmed' | 'processing' | 'shipped' | 'delivered' | 'cancelled' | 'refunded'

const STATUS_STYLES: Record<Status, { label: string; className: string; style: React.CSSProperties }> = {
  active:     { label: 'Active',     className: 'font-medium text-caption uppercase tracking-wide px-2.5 py-1 rounded-md', style: { background: 'var(--success-bg)', color: 'var(--success)' } },
  inactive:   { label: 'Inactive',   className: 'font-medium text-caption uppercase tracking-wide px-2.5 py-1 rounded-md', style: { background: 'var(--bg-overlay)', color: 'var(--text-muted)' } },
  pending:    { label: 'Pending',    className: 'font-medium text-caption uppercase tracking-wide px-2.5 py-1 rounded-md', style: { background: 'var(--warning-bg)', color: 'var(--warning)' } },
  confirmed:  { label: 'Confirmed',  className: 'font-medium text-caption uppercase tracking-wide px-2.5 py-1 rounded-md', style: { background: 'var(--info-bg)',    color: 'var(--info)' } },
  processing: { label: 'Processing', className: 'font-medium text-caption uppercase tracking-wide px-2.5 py-1 rounded-md', style: { background: 'var(--info-bg)',    color: 'var(--info)' } },
  shipped:    { label: 'Shipped',    className: 'font-medium text-caption uppercase tracking-wide px-2.5 py-1 rounded-md', style: { background: 'var(--info-bg)',    color: 'var(--accent-400)' } },
  delivered:  { label: 'Delivered',  className: 'font-medium text-caption uppercase tracking-wide px-2.5 py-1 rounded-md', style: { background: 'var(--success-bg)', color: 'var(--success)' } },
  cancelled:  { label: 'Cancelled',  className: 'font-medium text-caption uppercase tracking-wide px-2.5 py-1 rounded-md', style: { background: 'var(--error-bg)',   color: 'var(--error)' } },
  refunded:   { label: 'Refunded',   className: 'font-medium text-caption uppercase tracking-wide px-2.5 py-1 rounded-md', style: { background: 'var(--error-bg)',   color: 'var(--warning)' } },
}

interface StatusBadgeProps {
  status: Status | boolean
  className?: string
}

export function StatusBadge({ status, className }: StatusBadgeProps) {
  const key: Status = typeof status === 'boolean' ? (status ? 'active' : 'inactive') : status
  const config = STATUS_STYLES[key] ?? STATUS_STYLES.inactive

  return (
    <span className={cn(config.className, className)} style={config.style}>
      {config.label}
    </span>
  )
}
