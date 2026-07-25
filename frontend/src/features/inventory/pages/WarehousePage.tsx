/**
 * features/inventory/pages/WarehousePage.tsx
 * Warehouse management page.
 */
import { Warehouse } from 'lucide-react'
import { WarehouseTable } from '../components/WarehouseTable'

export function WarehousePage() {
  return (
    <main style={{ padding: '32px', maxWidth: 1440, margin: '0 auto', minHeight: '100%' }}>
      {/* Page header */}
      <div style={{ marginBottom: 28 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4 }}>
          <div
            style={{
              width: 36, height: 36, borderRadius: 8,
              background: 'var(--chart-2)22',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: 'var(--chart-2)',
            }}
          >
            <Warehouse size={18} />
          </div>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
            Warehouses
          </h1>
        </div>
        <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', margin: 0 }}>
          Manage your physical warehouse locations
        </p>
      </div>

      <WarehouseTable />
    </main>
  )
}
