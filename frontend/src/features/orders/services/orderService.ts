/**
 * features/orders/services/orderService.ts
 * API calls for the order domain — mirrors OrderController endpoints.
 */
import { apiClient } from '@/services/axios'
import type { ApiResponse, PageResponse } from '@/types/api.types'
import type {
  OrderSummaryResponse,
  OrderResponse,
  CreateOrderRequest,
  UpdateOrderStatusRequest,
  CancelOrderRequest,
  OrderFilterParams,
} from '@/types/order.types'

const BASE = '/orders'

export const orderService = {
  /** GET /api/v1/orders — paginated, filterable list */
  list(params: OrderFilterParams = {}) {
    return apiClient
      .get<ApiResponse<PageResponse<OrderSummaryResponse>>>(BASE, { params })
      .then((r) => r.data)
  },

  /** GET /api/v1/orders/:id — full order detail with items, addresses, payment, history */
  getById(id: string) {
    return apiClient
      .get<ApiResponse<OrderResponse>>(`${BASE}/${id}`)
      .then((r) => r.data)
  },

  /** POST /api/v1/orders — create a new order */
  create(data: CreateOrderRequest) {
    return apiClient
      .post<ApiResponse<OrderResponse>>(BASE, data)
      .then((r) => r.data)
  },

  /** PATCH /api/v1/orders/:id/status — status transition (enforces state machine) */
  updateStatus(id: string, data: UpdateOrderStatusRequest) {
    return apiClient
      .patch<ApiResponse<OrderResponse>>(`${BASE}/${id}/status`, data)
      .then((r) => r.data)
  },

  /** POST /api/v1/orders/:id/cancel — explicit cancel shortcut */
  cancel(id: string, data?: CancelOrderRequest) {
    return apiClient
      .post<ApiResponse<OrderResponse>>(`${BASE}/${id}/cancel`, data ?? {})
      .then((r) => r.data)
  },
}
