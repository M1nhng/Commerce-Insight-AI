/**
 * features/inventory/pages/InventoryPage.tsx
 *
 * Main Inventory Management page with:
 *   - KPI stat cards
 *   - Inventory table with filters, search, pagination
 *   - Pending Adjustments tab (ADMIN/MANAGER)
 */
import { useState } from 'react'
import { Package, ArrowLeftRight, RefreshCw, CheckCircle2, XCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { InventoryStats } from '../components/InventoryStats'
import { InventoryTable } from '../components/InventoryTable'
import { TransferDialog } from '../components/TransferDialog'
import { AdjustmentDialog } from '../components/AdjustmentDialog'
import { AdjustmentStatusBadge } from '../components/StockBadge'
import { useStockAdjustments, useApproveAdjustment, useRejectAdjustment } from '../hooks/useStockAdjustments'
import { useAuth } from '@/hooks/useAuth'

// ── Pending Adjustments panel ─────────────────────────────────────────────

function PendingAdjustmentsPanel() {
  const { data, isLoading } = useStockAdjustments({ status: 'PENDING', size: 50, sortBy: 'createdAt', sortDir: 'desc' })
  const approve = useApproveAdjustment()
  const reject  = useRejectAdjustment()

  const rows = data?.content ?? []

  if (isLoading) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8, padding: 4 }}>
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} style={{ height: 64, background: 'var(--bg-elevated)', borderRadius: 10, animation: 'pulse 1.5s ease-in-out infinite' }} />
        ))}
      </div>
    )
  }

  if (rows.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: '48px 24px' }}>
        <p style={{ fontSize: '2rem', marginBottom: 8 }}>✅</p>
        <p style={{ fontSize: '0.9375rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: 4 }}>All clear!</p>
        <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>No pending stock adjustments.</p>
      </div>
    )
  }

  return (
    <div
      style={{
        background: 'var(--bg-surface)',
        border: '1px solid var(--border-default)',
        borderRadius: 12,
        overflow: 'hidden',
      }}
    >
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ background: 'var(--bg-elevated)', borderBottom: '1px solid var(--border-default)' }}>
            {['Product', 'Warehouse', 'Delta', 'Reason', 'Requested By', 'Status', 'Actions'].map((h) => (
              <th
                key={h}
                style={{
                  padding: '10px 16px', textAlign: 'left',
                  fontSize: '0.75rem', fontWeight: 600,
                  letterSpacing: '0.05em', textTransform: 'uppercase',
                  color: 'var(--text-secondary)',
                }}
              >
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, idx) => (
            <tr
              key={row.id}
              style={{
                borderBottom: '1px solid var(--border-subtle)',
                background: idx % 2 === 0 ? 'transparent' : 'var(--bg-surface)',
              }}
            >
              <td style={{ padding: '13px 16px', fontSize: '0.875rem', color: 'var(--text-primary)', fontWeight: 500 }}>
                {row.productName}
              </td>
              <td style={{ padding: '13px 16px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                {row.warehouseName}
              </td>
              <td style={{ padding: '13px 16px' }}>
                <span
                  style={{
                    fontFamily: 'JetBrains Mono, monospace',
                    fontWeight: 700,
                    fontSize: '0.9375rem',
                    color: row.quantityDelta > 0 ? 'var(--success)' : 'var(--error)',
                  }}
                >
                  {row.quantityDelta > 0 ? '+' : ''}{row.quantityDelta}
                </span>
              </td>
              <td style={{ padding: '13px 16px', fontSize: '0.8125rem', color: 'var(--text-secondary)', maxWidth: 200 }}>
                <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'block' }}>
                  {row.reason}
                </span>
              </td>
              <td style={{ padding: '13px 16px', fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                {row.requestedBy}
              </td>
              <td style={{ padding: '13px 16px' }}>
                <AdjustmentStatusBadge status={row.status} />
              </td>
              <td style={{ padding: '13px 16px' }}>
                <div style={{ display: 'flex', gap: 6 }}>
                  <Button
                    size="sm"
                    onClick={() => approve.mutate({ id: row.id })}
                    disabled={approve.isPending}
                    style={{
                      background: 'var(--success-bg)', color: 'var(--success)',
                      border: '1px solid var(--success)', borderRadius: 6,
                      height: 28, fontSize: '0.75rem', gap: 4,
                    }}
                  >
                    <CheckCircle2 size={12} /> Approve
                  </Button>
                  <Button
                    size="sm"
                    onClick={() => reject.mutate({ id: row.id })}
                    disabled={reject.isPending}
                    style={{
                      background: 'var(--error-bg)', color: 'var(--error)',
                      border: '1px solid var(--error)', borderRadius: 6,
                      height: 28, fontSize: '0.75rem', gap: 4,
                    }}
                  >
                    <XCircle size={12} /> Reject
                  </Button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

// ── Page ─────────────────────────────────────────────────────────────────────

type Tab = 'stock' | 'adjustments'

export function InventoryPage() {
  const { user } = useAuth()
  const isManager = user?.role === 'ADMIN' || user?.role === 'MANAGER'

  const [activeTab, setActiveTab] = useState<Tab>('stock')
  const [transferOpen, setTransferOpen] = useState(false)
  const [adjustOpen,   setAdjustOpen]   = useState(false)

  return (
    <main style={{ padding: '32px', maxWidth: 1440, margin: '0 auto', minHeight: '100%' }}>
      {/* ── Page header ─────────────────────────────────────────────────── */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 24, flexWrap: 'wrap', gap: 12 }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4 }}>
            <div style={{ width: 36, height: 36, borderRadius: 8, background: 'var(--accent-500)22', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--accent-400)' }}>
              <Package size={18} />
            </div>
            <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
              Inventory
            </h1>
          </div>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', margin: 0 }}>
            Manage stock levels across all warehouses
          </p>
        </div>

        {isManager && (
          <div style={{ display: 'flex', gap: 10 }}>
            <Button
              onClick={() => setTransferOpen(true)}
              variant="outline"
              style={{
                borderColor: 'var(--border-default)',
                color: 'var(--text-primary)',
                background: 'var(--bg-elevated)',
                borderRadius: 8,
                gap: 6,
                height: 36,
                fontSize: '0.875rem',
              }}
            >
              <ArrowLeftRight size={14} /> Transfer Stock
            </Button>
            <Button
              onClick={() => setAdjustOpen(true)}
              style={{
                background: 'var(--accent-500)',
                color: '#fff',
                borderRadius: 8,
                gap: 6,
                height: 36,
                fontSize: '0.875rem',
              }}
            >
              <RefreshCw size={14} /> Adjust Stock
            </Button>
          </div>
        )}
      </div>

      {/* ── KPI Stats ───────────────────────────────────────────────────── */}
      <InventoryStats />

      {/* ── Tabs ────────────────────────────────────────────────────────── */}
      {isManager && (
        <div style={{ display: 'flex', gap: 4, marginBottom: 20, borderBottom: '1px solid var(--border-subtle)', paddingBottom: 0 }}>
          {([
            { id: 'stock',       label: '📦 Stock Levels' },
            { id: 'adjustments', label: '⏳ Pending Approvals' },
          ] as { id: Tab; label: string }[]).map(({ id, label }) => (
            <button
              key={id}
              onClick={() => setActiveTab(id)}
              style={{
                padding: '8px 18px',
                fontSize: '0.875rem',
                fontWeight: 500,
                color: activeTab === id ? 'var(--accent-400)' : 'var(--text-secondary)',
                background: 'none',
                border: 'none',
                borderBottom: `2px solid ${activeTab === id ? 'var(--accent-500)' : 'transparent'}`,
                cursor: 'pointer',
                transition: 'all 0.15s',
                marginBottom: -1,
              }}
            >
              {label}
            </button>
          ))}
        </div>
      )}

      {/* ── Content ─────────────────────────────────────────────────────── */}
      {(activeTab === 'stock' || !isManager) && <InventoryTable />}
      {activeTab === 'adjustments' && isManager && <PendingAdjustmentsPanel />}

      {/* ── Global dialogs (no pre-selected inventory row) ───────────────── */}
      <TransferDialog
        inventory={null}
        open={transferOpen}
        onClose={() => setTransferOpen(false)}
      />
      <AdjustmentDialog
        inventory={null}
        mode="direct"
        open={adjustOpen}
        onClose={() => setAdjustOpen(false)}
      />
    </main>
  )
}
