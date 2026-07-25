/**
 * features/inventory/index.ts — Public API for the inventory feature module.
 */

// Pages
export { InventoryPage }  from './pages/InventoryPage'
export { WarehousePage }  from './pages/WarehousePage'

// Components (exported for potential reuse in other features)
export { StockBadge, QuantityCell, TransactionTypeBadge, AdjustmentStatusBadge } from './components/StockBadge'
export { InventoryStats }    from './components/InventoryStats'
export { TransactionHistory } from './components/TransactionHistory'

// Hooks (re-exported for use in other features, e.g. Dashboard)
export * from './hooks/useInventory'
export * from './hooks/useWarehouses'
export * from './hooks/useStockAdjustments'
