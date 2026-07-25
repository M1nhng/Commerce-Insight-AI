/**
 * features/inventory/components/StockBadge.tsx
 * Visual badge showing stock level status.
 */
import type { InventoryResponse } from '@/types/inventory.types'

interface StockBadgeProps {
  inventory: Pick<InventoryResponse, 'quantity' | 'isLowStock'>
  size?: 'sm' | 'md'
}

export function StockBadge({ inventory, size = 'md' }: StockBadgeProps) {
  const { quantity, isLowStock } = inventory

  const isOutOfStock = quantity === 0

  const config = isOutOfStock
    ? { label: 'Out of Stock', bg: 'var(--error-bg)',   color: 'var(--error)',   dot: 'var(--error)' }
    : isLowStock
    ? { label: 'Low Stock',    bg: 'var(--warning-bg)', color: 'var(--warning)', dot: 'var(--warning)' }
    : { label: 'In Stock',     bg: 'var(--success-bg)', color: 'var(--success)', dot: 'var(--success)' }

  const fontSize = size === 'sm' ? '0.6875rem' : '0.75rem'
  const padding  = size === 'sm' ? '1px 7px'   : '2px 10px'

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 5,
        background: config.bg,
        color: config.color,
        borderRadius: 4,
        fontSize,
        fontWeight: 500,
        padding,
        letterSpacing: '0.02em',
        whiteSpace: 'nowrap',
      }}
    >
      <span
        style={{
          width: 6,
          height: 6,
          borderRadius: '50%',
          background: config.dot,
          flexShrink: 0,
        }}
      />
      {config.label}
    </span>
  )
}

/** Compact quantity display cell used in table rows */
export function QuantityCell({ value, highlight }: { value: number; highlight?: boolean }) {
  return (
    <span
      style={{
        fontFamily: 'JetBrains Mono, monospace',
        fontSize: '0.875rem',
        fontWeight: 600,
        color: highlight
          ? value === 0
            ? 'var(--error)'
            : 'var(--warning)'
          : 'var(--text-primary)',
      }}
    >
      {value.toLocaleString()}
    </span>
  )
}

/** Transaction type badge */
type TxType = 'PURCHASE' | 'SALE' | 'RETURN' | 'ADJUSTMENT' | 'TRANSFER_IN' | 'TRANSFER_OUT' | 'DAMAGE' | 'EXPIRED' | 'WRITE_OFF'

const TX_CONFIG: Record<TxType, { label: string; bg: string; color: string }> = {
  PURCHASE:     { label: 'Purchase',     bg: 'var(--success-bg)', color: 'var(--success)' },
  SALE:         { label: 'Sale',         bg: 'var(--info-bg)',    color: 'var(--info)' },
  RETURN:       { label: 'Return',       bg: 'var(--warning-bg)', color: 'var(--warning)' },
  ADJUSTMENT:   { label: 'Adjustment',   bg: 'var(--info-bg)',    color: 'var(--accent-400)' },
  TRANSFER_IN:  { label: 'Transfer In',  bg: 'var(--success-bg)', color: 'var(--success)' },
  TRANSFER_OUT: { label: 'Transfer Out', bg: 'var(--error-bg)',   color: 'var(--error)' },
  DAMAGE:       { label: 'Damage',       bg: 'var(--error-bg)',   color: 'var(--error)' },
  EXPIRED:      { label: 'Expired',      bg: 'var(--error-bg)',   color: 'var(--warning)' },
  WRITE_OFF:    { label: 'Write Off',    bg: 'var(--error-bg)',   color: 'var(--error)' },
}

export function TransactionTypeBadge({ type }: { type: TxType }) {
  const cfg = TX_CONFIG[type] ?? { label: type, bg: 'var(--bg-overlay)', color: 'var(--text-secondary)' }
  return (
    <span
      style={{
        display: 'inline-block',
        background: cfg.bg,
        color: cfg.color,
        borderRadius: 4,
        fontSize: '0.6875rem',
        fontWeight: 600,
        padding: '2px 8px',
        letterSpacing: '0.03em',
        textTransform: 'uppercase',
      }}
    >
      {cfg.label}
    </span>
  )
}

/** Adjustment status badge */
type AdjStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

const ADJ_CONFIG: Record<AdjStatus, { label: string; bg: string; color: string }> = {
  PENDING:  { label: 'Pending',  bg: 'var(--warning-bg)', color: 'var(--warning)' },
  APPROVED: { label: 'Approved', bg: 'var(--success-bg)', color: 'var(--success)' },
  REJECTED: { label: 'Rejected', bg: 'var(--error-bg)',   color: 'var(--error)' },
}

export function AdjustmentStatusBadge({ status }: { status: AdjStatus }) {
  const cfg = ADJ_CONFIG[status]
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 5,
        background: cfg.bg,
        color: cfg.color,
        borderRadius: 4,
        fontSize: '0.75rem',
        fontWeight: 500,
        padding: '2px 10px',
      }}
    >
      <span style={{ width: 6, height: 6, borderRadius: '50%', background: cfg.color }} />
      {cfg.label}
    </span>
  )
}
