/**
 * features/inventory/components/InventoryTable.tsx
 * Filterable, paginated inventory table with row actions.
 */
import { useState, useCallback } from 'react'
import { Search, X, ArrowUpDown, MoreHorizontal, RefreshCw, History, ArrowLeftRight } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { useInventoryList } from '../hooks/useInventory'
import { useWarehouseOptions } from '../hooks/useWarehouses'
import { StockBadge, QuantityCell } from './StockBadge'
import { AdjustmentDialog } from './AdjustmentDialog'
import { TransferDialog } from './TransferDialog'
import { TransactionHistory } from './TransactionHistory'
import type { InventoryResponse, InventoryFilterParams } from '@/types/inventory.types'
import { useAuth } from '@/hooks/useAuth'

// ── Table skeleton ────────────────────────────────────────────────────────

function TableSkeleton() {
  return (
    <>
      {Array.from({ length: 6 }).map((_, i) => (
        <tr key={i} style={{ borderBottom: '1px solid var(--border-subtle)' }}>
          {[200, 100, 150, 80, 80, 80, 90, 60].map((w, j) => (
            <td key={j} style={{ padding: '14px 16px' }}>
              <div
                style={{
                  height: 14,
                  width: w,
                  maxWidth: '100%',
                  background: 'var(--bg-elevated)',
                  borderRadius: 4,
                  animation: 'pulse 1.5s ease-in-out infinite',
                }}
              />
            </td>
          ))}
        </tr>
      ))}
    </>
  )
}

// ── Pagination component ──────────────────────────────────────────────────

interface PaginationProps {
  page: number
  totalPages: number
  totalElements: number
  size: number
  onPageChange: (p: number) => void
  onSizeChange: (s: number) => void
  label: string
}

function Pagination({ page, totalPages, totalElements, size, onPageChange, onSizeChange, label }: PaginationProps) {
  const start = page * size + 1
  const end = Math.min((page + 1) * size, totalElements)
  const pages = Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
    if (totalPages <= 5) return i
    if (page <= 2) return i
    if (page >= totalPages - 3) return totalPages - 5 + i
    return page - 2 + i
  })

  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 16px', flexWrap: 'wrap', gap: 8 }}>
      <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
        Showing <strong style={{ color: 'var(--text-primary)' }}>{totalElements === 0 ? 0 : start}–{end}</strong>{' '}
        of <strong style={{ color: 'var(--text-primary)' }}>{totalElements}</strong> {label}
      </p>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <Select value={String(size)} onValueChange={(v) => { onSizeChange(Number(v)); onPageChange(0) }}>
          <SelectTrigger className="h-8 w-28 text-xs" style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}>
            <SelectValue />
          </SelectTrigger>
          <SelectContent style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}>
            {[10, 25, 50].map((s) => (
              <SelectItem key={s} value={String(s)} style={{ color: 'var(--text-primary)' }}>{s} / page</SelectItem>
            ))}
          </SelectContent>
        </Select>
        <div style={{ display: 'flex', gap: 4 }}>
          <Button variant="ghost" size="sm" disabled={page === 0} onClick={() => onPageChange(page - 1)}
            style={{ color: 'var(--text-secondary)', padding: '4px 10px', fontSize: '0.8125rem' }}>←</Button>
          {pages.map((p) => (
            <Button key={p} variant={p === page ? 'default' : 'ghost'} size="sm"
              onClick={() => onPageChange(p)}
              style={{
                padding: '4px 10px', fontSize: '0.8125rem', minWidth: 32,
                background: p === page ? 'var(--accent-500)' : 'transparent',
                color: p === page ? '#fff' : 'var(--text-secondary)',
              }}
            >{p + 1}</Button>
          ))}
          <Button variant="ghost" size="sm" disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)}
            style={{ color: 'var(--text-secondary)', padding: '4px 10px', fontSize: '0.8125rem' }}>→</Button>
        </div>
      </div>
    </div>
  )
}

// ── Main table ────────────────────────────────────────────────────────────

export function InventoryTable() {
  const { user } = useAuth()
  const isManager = user?.role === 'ADMIN' || user?.role === 'MANAGER'

  // Filter state
  const [search,       setSearch]       = useState('')
  const [warehouseId,  setWarehouseId]  = useState<string>('')
  const [lowStockOnly, setLowStockOnly] = useState(false)
  const [page,         setPage]         = useState(0)
  const [size,         setSize]         = useState(10)
  const [sortBy,       setSortBy]       = useState('product.name')
  const [sortDir,      setSortDir]      = useState<'asc' | 'desc'>('asc')

  // Dialogs
  const [adjustTarget,  setAdjustTarget]  = useState<InventoryResponse | null>(null)
  const [adjustMode,    setAdjustMode]    = useState<'direct' | 'request'>('direct')
  const [transferTarget, setTransferTarget] = useState<InventoryResponse | null>(null)
  const [historyTarget,  setHistoryTarget]  = useState<InventoryResponse | null>(null)

  const params: InventoryFilterParams = {
    search: search || undefined,
    warehouseId: warehouseId || undefined,
    lowStockOnly: lowStockOnly || undefined,
    page, size, sortBy, sortDir,
  }

  const { data, isLoading } = useInventoryList(params)
  const { data: warehouses = [] } = useWarehouseOptions()

  const rows       = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const total      = data?.totalElements ?? 0

  const handleSort = useCallback((col: string) => {
    if (sortBy === col) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortBy(col)
      setSortDir('asc')
    }
    setPage(0)
  }, [sortBy])

  const clearFilters = () => {
    setSearch('')
    setWarehouseId('')
    setLowStockOnly(false)
    setPage(0)
  }

  const hasFilters = search || warehouseId || lowStockOnly

  const SortButton = ({ col, label }: { col: string; label: string }) => (
    <button
      onClick={() => handleSort(col)}
      style={{
        display: 'flex', alignItems: 'center', gap: 4,
        color: sortBy === col ? 'var(--accent-400)' : 'var(--text-secondary)',
        background: 'none', border: 'none', cursor: 'pointer',
        fontSize: '0.75rem', fontWeight: 600, letterSpacing: '0.05em', textTransform: 'uppercase',
        padding: 0,
      }}
    >
      {label}
      <ArrowUpDown size={11} style={{ opacity: sortBy === col ? 1 : 0.4 }} />
    </button>
  )

  return (
    <div style={{ background: 'var(--bg-surface)', border: '1px solid var(--border-default)', borderRadius: 12, overflow: 'hidden' }}>
      {/* ── Filter bar ─────────────────────────────────────────────────────── */}
      <div
        style={{
          padding: '16px 20px',
          display: 'flex',
          gap: 12,
          alignItems: 'center',
          flexWrap: 'wrap',
          borderBottom: '1px solid var(--border-subtle)',
        }}
      >
        {/* Search */}
        <div style={{ position: 'relative', flex: 1, minWidth: 200 }}>
          <Search size={14} style={{ position: 'absolute', left: 11, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <Input
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0) }}
            placeholder="Search product name or SKU…"
            style={{
              paddingLeft: 32, height: 36, fontSize: '0.875rem',
              background: 'var(--bg-elevated)', border: '1px solid var(--border-default)',
              color: 'var(--text-primary)', borderRadius: 8,
            }}
          />
        </div>

        {/* Warehouse filter */}
        <Select
          value={warehouseId || 'all'}
          onValueChange={(v) => { setWarehouseId(v === 'all' ? '' : v); setPage(0) }}
        >
          <SelectTrigger
            style={{
              width: 180, height: 36, fontSize: '0.875rem',
              background: 'var(--bg-elevated)', border: '1px solid var(--border-default)',
              color: warehouseId ? 'var(--text-primary)' : 'var(--text-muted)', borderRadius: 8,
            }}
          >
            <SelectValue placeholder="All Warehouses" />
          </SelectTrigger>
          <SelectContent style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}>
            <SelectItem value="all" style={{ color: 'var(--text-secondary)' }}>All Warehouses</SelectItem>
            {warehouses.map((w) => (
              <SelectItem key={w.id} value={w.id} style={{ color: 'var(--text-primary)' }}>
                {w.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        {/* Low stock toggle */}
        <button
          onClick={() => { setLowStockOnly((v) => !v); setPage(0) }}
          style={{
            display: 'flex', alignItems: 'center', gap: 6,
            height: 36, padding: '0 14px', borderRadius: 8,
            background: lowStockOnly ? 'var(--warning-bg)' : 'var(--bg-elevated)',
            border: `1px solid ${lowStockOnly ? 'var(--warning)' : 'var(--border-default)'}`,
            color: lowStockOnly ? 'var(--warning)' : 'var(--text-secondary)',
            cursor: 'pointer', fontSize: '0.875rem', fontWeight: 500, transition: 'all 0.2s',
            whiteSpace: 'nowrap',
          }}
        >
          ⚠ Low Stock Only
        </button>

        {/* Clear */}
        {hasFilters && (
          <Button variant="ghost" size="sm" onClick={clearFilters}
            style={{ color: 'var(--text-muted)', height: 36, padding: '0 10px' }}>
            <X size={14} style={{ marginRight: 4 }} /> Clear
          </Button>
        )}
      </div>

      {/* ── Table ──────────────────────────────────────────────────────────── */}
      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ background: 'var(--bg-elevated)', borderBottom: '1px solid var(--border-default)' }}>
              {[
                { col: 'product.name',  label: 'Product' },
                { col: 'product.sku',   label: 'SKU' },
                { col: 'warehouse.name',label: 'Warehouse' },
                { col: 'quantity',      label: 'Stock' },
                { col: 'reservedQuantity', label: 'Reserved' },
                { col: 'availableQuantity', label: 'Available' },
              ].map(({ col, label }) => (
                <th key={col} style={{ padding: '10px 16px', textAlign: 'left' }}>
                  <SortButton col={col} label={label} />
                </th>
              ))}
              <th style={{ padding: '10px 16px', textAlign: 'left', fontSize: '0.75rem', fontWeight: 600, letterSpacing: '0.05em', textTransform: 'uppercase', color: 'var(--text-secondary)' }}>Status</th>
              <th style={{ padding: '10px 16px', textAlign: 'right', width: 60 }} />
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <TableSkeleton />
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={8}>
                  <div style={{ textAlign: 'center', padding: '56px 24px' }}>
                    <p style={{ fontSize: '2.5rem', marginBottom: 12 }}>📦</p>
                    <p style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: 6 }}>
                      No inventory records
                    </p>
                    <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
                      {hasFilters ? 'Try adjusting your filters.' : 'Inventory records appear here once products are added.'}
                    </p>
                  </div>
                </td>
              </tr>
            ) : (
              rows.map((row, idx) => (
                <tr
                  key={row.id}
                  style={{
                    borderBottom: '1px solid var(--border-subtle)',
                    background: idx % 2 === 0 ? 'transparent' : 'var(--bg-surface)',
                    transition: 'background 0.15s',
                  }}
                  className="hover:bg-[var(--bg-elevated)]"
                >
                  <td style={{ padding: '13px 16px' }}>
                    <p style={{ fontSize: '0.875rem', fontWeight: 500, color: 'var(--text-primary)' }}>{row.productName}</p>
                  </td>
                  <td style={{ padding: '13px 16px' }}>
                    <code style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', fontFamily: 'JetBrains Mono, monospace' }}>{row.productSku}</code>
                  </td>
                  <td style={{ padding: '13px 16px' }}>
                    <div>
                      <p style={{ fontSize: '0.875rem', color: 'var(--text-primary)' }}>{row.warehouseName}</p>
                      <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{row.warehouseCode}</p>
                    </div>
                  </td>
                  <td style={{ padding: '13px 16px' }}>
                    <QuantityCell value={row.quantity} highlight={row.quantity === 0} />
                  </td>
                  <td style={{ padding: '13px 16px' }}>
                    <QuantityCell value={row.reservedQuantity} />
                  </td>
                  <td style={{ padding: '13px 16px' }}>
                    <QuantityCell value={row.availableQuantity} highlight={row.isLowStock} />
                  </td>
                  <td style={{ padding: '13px 16px' }}>
                    <StockBadge inventory={row} />
                  </td>
                  <td style={{ padding: '13px 16px', textAlign: 'right' }}>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="sm" style={{ color: 'var(--text-muted)', padding: '4px 8px' }}>
                          <MoreHorizontal size={16} />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent
                        align="end"
                        style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)', borderRadius: 10, minWidth: 180 }}
                      >
                        {isManager && (
                          <DropdownMenuItem
                            onClick={() => { setAdjustTarget(row); setAdjustMode('direct') }}
                            style={{ color: 'var(--text-primary)', fontSize: '0.875rem', cursor: 'pointer' }}
                          >
                            <RefreshCw size={14} style={{ marginRight: 8, color: 'var(--accent-400)' }} />
                            Adjust Stock
                          </DropdownMenuItem>
                        )}
                        <DropdownMenuItem
                          onClick={() => { setAdjustTarget(row); setAdjustMode('request') }}
                          style={{ color: 'var(--text-primary)', fontSize: '0.875rem', cursor: 'pointer' }}
                        >
                          <RefreshCw size={14} style={{ marginRight: 8, color: 'var(--warning)' }} />
                          Request Adjustment
                        </DropdownMenuItem>
                        {isManager && (
                          <DropdownMenuItem
                            onClick={() => setTransferTarget(row)}
                            style={{ color: 'var(--text-primary)', fontSize: '0.875rem', cursor: 'pointer' }}
                          >
                            <ArrowLeftRight size={14} style={{ marginRight: 8, color: 'var(--chart-2)' }} />
                            Transfer Stock
                          </DropdownMenuItem>
                        )}
                        <DropdownMenuItem
                          onClick={() => setHistoryTarget(row)}
                          style={{ color: 'var(--text-primary)', fontSize: '0.875rem', cursor: 'pointer' }}
                        >
                          <History size={14} style={{ marginRight: 8, color: 'var(--text-secondary)' }} />
                          View History
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* ── Pagination ─────────────────────────────────────────────────────── */}
      {total > 0 && (
        <div style={{ borderTop: '1px solid var(--border-subtle)' }}>
          <Pagination
            page={page} totalPages={totalPages} totalElements={total} size={size}
            onPageChange={setPage} onSizeChange={setSize}
            label="inventory records"
          />
        </div>
      )}

      {/* ── Dialogs ────────────────────────────────────────────────────────── */}
      <AdjustmentDialog
        inventory={adjustTarget}
        mode={adjustMode}
        open={!!adjustTarget}
        onClose={() => setAdjustTarget(null)}
      />
      <TransferDialog
        inventory={transferTarget}
        open={!!transferTarget}
        onClose={() => setTransferTarget(null)}
      />
      <TransactionHistory
        inventory={historyTarget}
        open={!!historyTarget}
        onClose={() => setHistoryTarget(null)}
      />
    </div>
  )
}
