/**
 * features/inventory/services/inventoryService.ts
 * Axios API calls for the inventory domain.
 */
import { apiClient } from '@/services/axios'
import type { ApiResponse, PageResponse } from '@/types/api.types'
import type {
  InventoryResponse,
  InventoryFilterParams,
  AdjustStockRequest,
  TransferStockRequest,
  InventoryTransactionResponse,
} from '@/types/inventory.types'

const BASE = '/inventory'

export const inventoryService = {
  /** GET /api/v1/inventory */
  list(params: InventoryFilterParams = {}) {
    return apiClient
      .get<ApiResponse<PageResponse<InventoryResponse>>>(BASE, { params })
      .then((r) => r.data)
  },

  /** GET /api/v1/inventory/:id */
  getById(id: string) {
    return apiClient
      .get<ApiResponse<InventoryResponse>>(`${BASE}/${id}`)
      .then((r) => r.data)
  },

  /** GET /api/v1/inventory/product/:productId */
  getByProduct(productId: string) {
    return apiClient
      .get<ApiResponse<InventoryResponse[]>>(`${BASE}/product/${productId}`)
      .then((r) => r.data)
  },

  /** GET /api/v1/inventory/low-stock */
  getLowStock() {
    return apiClient
      .get<ApiResponse<InventoryResponse[]>>(`${BASE}/low-stock`)
      .then((r) => r.data)
  },

  /** PATCH /api/v1/inventory/:id/adjust */
  adjust(id: string, data: AdjustStockRequest) {
    return apiClient
      .patch<ApiResponse<InventoryResponse>>(`${BASE}/${id}/adjust`, data)
      .then((r) => r.data)
  },

  /** POST /api/v1/inventory/transfer */
  transfer(data: TransferStockRequest) {
    return apiClient
      .post<ApiResponse<void>>(`${BASE}/transfer`, data)
      .then((r) => r.data)
  },

  /** GET /api/v1/inventory/:id/transactions */
  getTransactions(id: string, page = 0, size = 20) {
    return apiClient
      .get<ApiResponse<PageResponse<InventoryTransactionResponse>>>(
        `${BASE}/${id}/transactions`,
        { params: { page, size } }
      )
      .then((r) => r.data)
  },
}
