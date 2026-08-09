/**
 * features/customers/services/customerService.ts
 * API calls for the customer domain.
 */
import { apiClient } from '@/services/axios'
import type { ApiResponse, PageResponse } from '@/types/api.types'
import type {
  CustomerSummaryResponse,
  CustomerResponse,
  CreateCustomerRequest,
  UpdateCustomerRequest,
  UpdateCustomerStatusRequest,
  CustomerFilterParams,
} from '@/types/customer.types'

const BASE = '/customers'

export const customerService = {
  /** GET /api/v1/customers */
  list(params: CustomerFilterParams = {}) {
    return apiClient
      .get<ApiResponse<PageResponse<CustomerSummaryResponse>>>(BASE, { params })
      .then((r) => r.data)
  },

  /** GET /api/v1/customers/:id */
  getById(id: string) {
    return apiClient
      .get<ApiResponse<CustomerResponse>>(`${BASE}/${id}`)
      .then((r) => r.data)
  },

  /** POST /api/v1/customers */
  create(data: CreateCustomerRequest) {
    return apiClient
      .post<ApiResponse<CustomerResponse>>(BASE, data)
      .then((r) => r.data)
  },

  /** PUT /api/v1/customers/:id */
  update(id: string, data: UpdateCustomerRequest) {
    return apiClient
      .put<ApiResponse<CustomerResponse>>(`${BASE}/${id}`, data)
      .then((r) => r.data)
  },

  /** DELETE /api/v1/customers/:id */
  delete(id: string) {
    return apiClient.delete(`${BASE}/${id}`)
  },

  /** PATCH /api/v1/customers/:id/status */
  updateStatus(id: string, data: UpdateCustomerStatusRequest) {
    return apiClient
      .patch<ApiResponse<CustomerResponse>>(`${BASE}/${id}/status`, data)
      .then((r) => r.data)
  },
}
