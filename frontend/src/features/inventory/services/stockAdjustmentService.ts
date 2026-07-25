/**
 * features/inventory/services/stockAdjustmentService.ts
 * Axios API calls for the stock adjustment approval workflow.
 */
import { apiClient } from '@/services/axios'
import type { ApiResponse, PageResponse } from '@/types/api.types'
import type {
  StockAdjustmentResponse,
  StockAdjustmentFilterParams,
  RequestStockAdjustmentRequest,
  ReviewStockAdjustmentRequest,
} from '@/types/inventory.types'

const BASE = '/stock-adjustments'

export const stockAdjustmentService = {
  /** GET /api/v1/stock-adjustments */
  list(params: StockAdjustmentFilterParams = {}) {
    return apiClient
      .get<ApiResponse<PageResponse<StockAdjustmentResponse>>>(BASE, { params })
      .then((r) => r.data)
  },

  /** GET /api/v1/stock-adjustments/:id */
  getById(id: string) {
    return apiClient
      .get<ApiResponse<StockAdjustmentResponse>>(`${BASE}/${id}`)
      .then((r) => r.data)
  },

  /** POST /api/v1/stock-adjustments */
  request(data: RequestStockAdjustmentRequest) {
    return apiClient
      .post<ApiResponse<StockAdjustmentResponse>>(BASE, data)
      .then((r) => r.data)
  },

  /** PATCH /api/v1/stock-adjustments/:id/approve */
  approve(id: string, data?: ReviewStockAdjustmentRequest) {
    return apiClient
      .patch<ApiResponse<StockAdjustmentResponse>>(`${BASE}/${id}/approve`, data ?? {})
      .then((r) => r.data)
  },

  /** PATCH /api/v1/stock-adjustments/:id/reject */
  reject(id: string, data?: ReviewStockAdjustmentRequest) {
    return apiClient
      .patch<ApiResponse<StockAdjustmentResponse>>(`${BASE}/${id}/reject`, data ?? {})
      .then((r) => r.data)
  },
}
