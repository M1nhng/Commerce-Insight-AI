/**
 * features/orders/components/OrderStatusTimeline.tsx
 * Vertical timeline of order status history entries.
 * Most recent entry is at the top and highlighted.
 */
import { CheckCircle2, Circle } from 'lucide-react'
import { OrderStatusBadge } from './OrderStatusBadge'
import type { OrderStatusHistoryEntry } from '@/types/order.types'

interface OrderStatusTimelineProps {
  history: OrderStatusHistoryEntry[]
}

export function OrderStatusTimeline({ history }: OrderStatusTimelineProps) {
  if (!history || history.length === 0) {
    return (
      <p className="text-body-sm py-4 text-center" style={{ color: 'var(--text-muted)' }}>
        No status history available.
      </p>
    )
  }

  // Sort descending (most recent first)
  const sorted = [...history].sort(
    (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  )

  return (
    <ol className="relative space-y-0">
      {sorted.map((entry, idx) => {
        const isLatest = idx === 0
        return (
          <li key={entry.id} className="flex gap-4 pb-6 last:pb-0">
            {/* Timeline connector */}
            <div className="flex flex-col items-center">
              <div
                className="flex items-center justify-center w-8 h-8 rounded-full shrink-0 z-10"
                style={{
                  background: isLatest ? 'var(--accent-500)' : 'var(--bg-overlay)',
                  border: `2px solid ${isLatest ? 'var(--accent-500)' : 'var(--border-default)'}`,
                }}
              >
                {isLatest ? (
                  <CheckCircle2 className="h-4 w-4 text-white" />
                ) : (
                  <Circle className="h-3 w-3" style={{ color: 'var(--text-muted)' }} />
                )}
              </div>
              {/* Vertical line — hide for last item */}
              {idx < sorted.length - 1 && (
                <div
                  className="w-px flex-1 mt-1"
                  style={{ background: 'var(--border-subtle)', minHeight: '20px' }}
                />
              )}
            </div>

            {/* Content */}
            <div className="flex-1 min-w-0 pb-1">
              <div className="flex items-center gap-2 flex-wrap">
                <OrderStatusBadge status={entry.toStatus} size="sm" />
                {entry.fromStatus && (
                  <span className="text-caption" style={{ color: 'var(--text-muted)' }}>
                    from {entry.fromStatus}
                  </span>
                )}
              </div>

              <p
                className="text-caption mt-1"
                style={{ color: 'var(--text-muted)' }}
              >
                {new Date(entry.createdAt).toLocaleString('en-GB', {
                  day: '2-digit',
                  month: 'short',
                  year: 'numeric',
                  hour: '2-digit',
                  minute: '2-digit',
                })}
                {entry.changedByName && (
                  <span> · by <strong style={{ color: 'var(--text-secondary)' }}>{entry.changedByName}</strong></span>
                )}
              </p>

              {entry.reason && (
                <p
                  className="text-caption mt-1 italic"
                  style={{ color: 'var(--text-secondary)' }}
                >
                  "{entry.reason}"
                </p>
              )}
            </div>
          </li>
        )
      })}
    </ol>
  )
}
