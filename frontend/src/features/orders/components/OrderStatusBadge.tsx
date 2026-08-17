/**
 * features/orders/components/OrderStatusBadge.tsx
 * Status badge following design spec §10.2 color tokens.
 */
import type { OrderStatus } from '@/types/order.types'
import { ORDER_STATUS_LABELS } from '@/types/order.types'

const BADGE_STYLES: Record<OrderStatus, { background: string; color: string }> = {
  PENDING:    { background: 'var(--warning-bg)',  color: 'var(--warning)' },
  CONFIRMED:  { background: 'var(--info-bg)',     color: 'var(--info)' },
  PROCESSING: { background: 'var(--info-bg)',     color: 'var(--info)' },
  SHIPPED:    { background: 'var(--info-bg)',     color: 'var(--accent-400)' },
  DELIVERED:  { background: 'var(--success-bg)',  color: 'var(--success)' },
  COMPLETED:  { background: 'var(--success-bg)',  color: 'var(--success)' },
  CANCELLED:  { background: 'var(--error-bg)',    color: 'var(--error)' },
  REFUNDED:   { background: 'var(--error-bg)',    color: 'var(--warning)' },
}

interface OrderStatusBadgeProps {
  status: OrderStatus
  size?: 'sm' | 'md'
}

export function OrderStatusBadge({ status, size = 'sm' }: OrderStatusBadgeProps) {
  const s = BADGE_STYLES[status] ?? { background: 'var(--bg-overlay)', color: 'var(--text-muted)' }
  return (
    <span
      className={`inline-flex items-center rounded font-medium ${size === 'md' ? 'px-3 py-1 text-body-sm' : 'px-2 py-0.5 text-caption'}`}
      style={s}
    >
      {ORDER_STATUS_LABELS[status] ?? status}
    </span>
  )
}
