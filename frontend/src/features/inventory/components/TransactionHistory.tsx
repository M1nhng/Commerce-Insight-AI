/**
 * features/inventory/components/TransactionHistory.tsx
 * Slide-in Sheet drawer showing the immutable audit trail for an inventory record.
 */
import { useState } from 'react'
import { X, ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet'
import { useInventoryTransactions } from '../hooks/useInventory'
import { TransactionTypeBadge } from './StockBadge'
import type { InventoryResponse } from '@/types/inventory.types'

interface TransactionHistoryProps {
  inventory: InventoryResponse | null
  open: boolean
  onClose: () => void
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleString('en-US', {
    month: 'short', day: 'numeric', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

export function TransactionHistory({ inventory, open, onClose }: TransactionHistoryProps) {
  const [page, setPage] = useState(0)
  const { data, isLoading } = useInventoryTransactions(
    open ? inventory?.id ?? null : null,
    page,
  )

  const rows       = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const total      = data?.totalElements ?? 0

  return (
    <Sheet open={open} onOpenChange={(v) => !v && onClose()}>
      <SheetContent
        side="right"
        style={{
          width: 560,
          maxWidth: '95vw',
          background: 'var(--bg-surface)',
          borderLeft: '1px solid var(--border-default)',
          display: 'flex',
          flexDirection: 'column',
          padding: 0,
        }}
      >
        {/* Header */}
        <SheetHeader
          style={{
            padding: '24px 24px 16px',
            borderBottom: '1px solid var(--border-subtle)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <SheetTitle style={{ color: 'var(--text-primary)', fontSize: '1rem', fontWeight: 600 }}>
                Transaction History
              </SheetTitle>
              {inventory && (
                <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginTop: 4 }}>
                  {inventory.productName} · {inventory.warehouseName}
                </p>
              )}
            </div>
            <Button variant="ghost" size="sm" onClick={onClose} style={{ color: 'var(--text-muted)' }}>
              <X size={16} />
            </Button>
          </div>
          {inventory && (
            <div style={{ display: 'flex', gap: 24, marginTop: 12 }}>
              {[
                { label: 'Current Stock', value: inventory.quantity },
                { label: 'Reserved',      value: inventory.reservedQuantity },
                { label: 'Available',     value: inventory.availableQuantity },
              ].map(({ label, value }) => (
                <div key={label}>
                  <p style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', marginBottom: 2 }}>{label}</p>
                  <p style={{ fontSize: '1.125rem', fontWeight: 700, color: 'var(--text-primary)', fontFamily: 'JetBrains Mono, monospace' }}>
                    {value.toLocaleString()}
                  </p>
                </div>
              ))}
            </div>
          )}
        </SheetHeader>

        {/* Transaction list */}
        <div style={{ flex: 1, overflowY: 'auto', padding: '8px 0' }}>
          {isLoading ? (
            Array.from({ length: 8 }).map((_, i) => (
              <div
                key={i}
                style={{
                  padding: '12px 24px',
                  display: 'flex',
                  gap: 12,
                  alignItems: 'center',
                  borderBottom: '1px solid var(--border-subtle)',
                }}
              >
                <div style={{ width: 80, height: 20, background: 'var(--bg-elevated)', borderRadius: 4, flexShrink: 0 }} />
                <div style={{ flex: 1 }}>
                  <div style={{ width: '60%', height: 14, background: 'var(--bg-elevated)', borderRadius: 4, marginBottom: 6 }} />
                  <div style={{ width: '40%', height: 12, background: 'var(--bg-elevated)', borderRadius: 4 }} />
                </div>
                <div style={{ width: 48, height: 20, background: 'var(--bg-elevated)', borderRadius: 4 }} />
              </div>
            ))
          ) : rows.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '48px 24px', color: 'var(--text-muted)' }}>
              <p style={{ fontSize: '2rem', marginBottom: 8 }}>📋</p>
              <p style={{ fontSize: '0.875rem' }}>No transactions recorded yet</p>
            </div>
          ) : (
            rows.map((tx) => {
              const isPositive = tx.quantity > 0
              return (
                <div
                  key={tx.id}
                  style={{
                    padding: '14px 24px',
                    borderBottom: '1px solid var(--border-subtle)',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 12,
                    transition: 'background 0.15s',
                  }}
                  className="hover:bg-[var(--bg-elevated)]"
                >
                  {/* Type badge */}
                  <div style={{ flexShrink: 0, minWidth: 100 }}>
                    <TransactionTypeBadge type={tx.type} />
                  </div>

                  {/* Info */}
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <p style={{ fontSize: '0.8125rem', color: 'var(--text-primary)', fontWeight: 500, marginBottom: 2 }}>
                      {tx.notes ?? tx.type}
                    </p>
                    <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      {tx.performedBy ? `By ${tx.performedBy} · ` : ''}{formatDate(tx.createdAt)}
                    </p>
                  </div>

                  {/* Delta + before→after */}
                  <div style={{ textAlign: 'right', flexShrink: 0 }}>
                    <p
                      style={{
                        fontSize: '0.9375rem',
                        fontWeight: 700,
                        fontFamily: 'JetBrains Mono, monospace',
                        color: isPositive ? 'var(--success)' : 'var(--error)',
                        marginBottom: 2,
                      }}
                    >
                      {isPositive ? '+' : ''}{tx.quantity.toLocaleString()}
                    </p>
                    <p style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', fontFamily: 'JetBrains Mono, monospace' }}>
                      {tx.quantityBefore} → {tx.quantityAfter}
                    </p>
                  </div>
                </div>
              )
            })
          )}
        </div>

        {/* Pagination footer */}
        {total > 0 && (
          <div
            style={{
              padding: '12px 24px',
              borderTop: '1px solid var(--border-subtle)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}
          >
            <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              {total} transaction{total !== 1 ? 's' : ''} total
            </p>
            <div style={{ display: 'flex', gap: 4 }}>
              <Button
                variant="ghost" size="sm"
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
                style={{ color: 'var(--text-secondary)', padding: '4px 8px' }}
              >
                <ChevronLeft size={16} />
              </Button>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', padding: '0 8px' }}>
                {page + 1} / {totalPages}
              </span>
              <Button
                variant="ghost" size="sm"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                style={{ color: 'var(--text-secondary)', padding: '4px 8px' }}
              >
                <ChevronRight size={16} />
              </Button>
            </div>
          </div>
        )}
      </SheetContent>
    </Sheet>
  )
}
