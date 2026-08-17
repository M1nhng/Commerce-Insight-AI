/**
 * features/orders/components/PaymentStatusBadge.tsx
 */
import type { PaymentStatus } from '@/types/order.types'
import { PAYMENT_STATUS_LABELS } from '@/types/order.types'

const BADGE_STYLES: Record<PaymentStatus, { background: string; color: string }> = {
  PENDING:  { background: 'var(--warning-bg)', color: 'var(--warning)' },
  PAID:     { background: 'var(--success-bg)', color: 'var(--success)' },
  FAILED:   { background: 'var(--error-bg)',   color: 'var(--error)' },
  REFUNDED: { background: 'var(--error-bg)',   color: 'var(--warning)' },
}

export function PaymentStatusBadge({ status }: { status: PaymentStatus }) {
  const s = BADGE_STYLES[status] ?? { background: 'var(--bg-overlay)', color: 'var(--text-muted)' }
  return (
    <span
      className="inline-flex items-center px-2 py-0.5 rounded text-caption font-medium"
      style={s}
    >
      {PAYMENT_STATUS_LABELS[status] ?? status}
    </span>
  )
}
