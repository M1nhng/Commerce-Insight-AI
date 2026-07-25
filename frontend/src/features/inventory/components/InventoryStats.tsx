/**
 * features/inventory/components/InventoryStats.tsx
 * KPI stat cards for the top of the Inventory page.
 */
import { Package, Warehouse, AlertTriangle, XCircle } from 'lucide-react'
import { useInventoryList, useLowStock } from '../hooks/useInventory'
import { useWarehouses } from '../hooks/useWarehouses'

interface StatCardProps {
  icon: React.ReactNode
  label: string
  value: string | number
  sublabel?: string
  accent?: string
  loading?: boolean
}

function StatCard({ icon, label, value, sublabel, accent, loading }: StatCardProps) {
  return (
    <div
      style={{
        background: 'var(--bg-surface)',
        border: '1px solid var(--border-default)',
        borderRadius: 12,
        padding: '20px 24px',
        display: 'flex',
        alignItems: 'center',
        gap: 16,
        flex: 1,
        minWidth: 0,
        transition: 'border-color 0.2s',
      }}
      className="hover:border-[var(--border-strong)]"
    >
      <div
        style={{
          width: 44,
          height: 44,
          borderRadius: 10,
          background: accent ? `${accent}22` : 'var(--bg-elevated)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexShrink: 0,
          color: accent ?? 'var(--accent-400)',
        }}
      >
        {icon}
      </div>
      <div style={{ minWidth: 0 }}>
        <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontWeight: 500, marginBottom: 2 }}>
          {label}
        </p>
        {loading ? (
          <div style={{ width: 60, height: 24, background: 'var(--bg-elevated)', borderRadius: 4, animation: 'pulse 1.5s ease-in-out infinite' }} />
        ) : (
          <p style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--text-primary)', lineHeight: 1.2 }}>
            {value}
          </p>
        )}
        {sublabel && (
          <p style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', marginTop: 2 }}>{sublabel}</p>
        )}
      </div>
    </div>
  )
}

export function InventoryStats() {
  const { data: inventoryPage, isLoading: loadingInventory } = useInventoryList({ size: 1 })
  const { data: lowStockItems,  isLoading: loadingLow }       = useLowStock()
  const { data: warehousePage,  isLoading: loadingWarehouses } = useWarehouses({ size: 1 })

  const totalSKUs      = inventoryPage?.totalElements ?? 0
  const lowStockCount  = lowStockItems?.length ?? 0
  const outOfStock     = lowStockItems?.filter((i) => i.quantity === 0).length ?? 0
  const warehouseCount = warehousePage?.totalElements ?? 0

  return (
    <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginBottom: 24 }}>
      <StatCard
        icon={<Warehouse size={20} />}
        label="Warehouses"
        value={warehouseCount}
        sublabel="Active locations"
        loading={loadingWarehouses}
        accent="var(--accent-500)"
      />
      <StatCard
        icon={<Package size={20} />}
        label="Total SKUs"
        value={totalSKUs}
        sublabel="Across all warehouses"
        loading={loadingInventory}
        accent="var(--chart-2)"
      />
      <StatCard
        icon={<AlertTriangle size={20} />}
        label="Low Stock"
        value={lowStockCount}
        sublabel="Below threshold"
        loading={loadingLow}
        accent="var(--warning)"
      />
      <StatCard
        icon={<XCircle size={20} />}
        label="Out of Stock"
        value={outOfStock}
        sublabel="Zero quantity"
        loading={loadingLow}
        accent="var(--error)"
      />
    </div>
  )
}
