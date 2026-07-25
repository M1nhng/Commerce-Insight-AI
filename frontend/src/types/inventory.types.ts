/**
 * types/inventory.types.ts
 * TypeScript interfaces mirroring the backend Inventory domain DTOs.
 */

// ── Warehouse ──────────────────────────────────────────────────────────────

export interface WarehouseResponse {
  id: string
  name: string
  code: string
  address: string | null
  city: string | null
  country: string | null
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateWarehouseRequest {
  name: string
  code: string
  address?: string | null
  city?: string | null
  country?: string | null
}

export interface UpdateWarehouseRequest {
  name: string
  code: string
  address?: string | null
  city?: string | null
  country?: string | null
  active: boolean
}

export interface WarehouseFilterParams {
  page?: number
  size?: number
  sortBy?: string
  sortDir?: 'asc' | 'desc'
}

// ── Inventory ──────────────────────────────────────────────────────────────

export interface InventoryResponse {
  id: string
  productId: string
  productName: string
  productSku: string
  warehouseId: string
  warehouseName: string
  warehouseCode: string
  quantity: number
  reservedQuantity: number
  availableQuantity: number
  lowStockThreshold: number
  isLowStock: boolean
  updatedAt: string
}

export interface AdjustStockRequest {
  quantity: number
  notes?: string | null
  lowStockThreshold?: number | null
}

export interface TransferStockRequest {
  productId: string
  sourceWarehouseId: string
  destinationWarehouseId: string
  quantity: number
  notes?: string | null
}

export interface InventoryFilterParams {
  warehouseId?: string
  productId?: string
  search?: string
  lowStockOnly?: boolean
  page?: number
  size?: number
  sortBy?: string
  sortDir?: 'asc' | 'desc'
}

// ── Inventory Transactions ─────────────────────────────────────────────────

export type TransactionType =
  | 'PURCHASE'
  | 'SALE'
  | 'RETURN'
  | 'ADJUSTMENT'
  | 'TRANSFER_IN'
  | 'TRANSFER_OUT'
  | 'DAMAGE'
  | 'EXPIRED'
  | 'WRITE_OFF'

export interface InventoryTransactionResponse {
  id: string
  inventoryId: string
  productId: string
  productName: string
  warehouseId: string
  warehouseName: string
  type: TransactionType
  quantity: number
  quantityBefore: number
  quantityAfter: number
  referenceId: string | null
  notes: string | null
  performedBy: string | null
  createdAt: string
}

// ── Stock Adjustments ──────────────────────────────────────────────────────

export type AdjustmentStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface StockAdjustmentResponse {
  id: string
  inventoryId: string
  productId: string
  productName: string
  warehouseId: string
  warehouseName: string
  quantityDelta: number
  reason: string
  status: AdjustmentStatus
  requestedBy: string
  requestedAt: string
  reviewedBy: string | null
  reviewedAt: string | null
  reviewNotes: string | null
  transactionId: string | null
}

export interface RequestStockAdjustmentRequest {
  inventoryId: string
  quantityDelta: number
  reason: string
}

export interface ReviewStockAdjustmentRequest {
  reviewNotes?: string | null
}

export interface StockAdjustmentFilterParams {
  status?: AdjustmentStatus
  warehouseId?: string
  productId?: string
  requestedBy?: string
  page?: number
  size?: number
  sortBy?: string
  sortDir?: 'asc' | 'desc'
}
