/**
 * features/customers/services/customerGroupService.ts
 * API calls for the customer group domain.
 */
import { apiClient } from '@/services/axios'
import type { ApiResponse, PageResponse } from '@/types/api.types'
import type {
  CustomerGroupResponse,
  CreateCustomerGroupRequest,
  UpdateCustomerGroupRequest,
  CustomerGroupFilterParams,
} from '@/types/customer.types'

const BASE = '/customer-groups'

export const customerGroupService = {
  /** GET /api/v1/customer-groups */
  list(params: CustomerGroupFilterParams = {}) {
    return apiClient
      .get<ApiResponse<PageResponse<CustomerGroupResponse>>>(BASE, { params })
      .then((r) => r.data)
  },

  /** GET /api/v1/customer-groups/:id */
  getById(id: string) {
    return apiClient
      .get<ApiResponse<CustomerGroupResponse>>(`${BASE}/${id}`)
      .then((r) => r.data)
  },

  /** POST /api/v1/customer-groups */
  create(data: CreateCustomerGroupRequest) {
    return apiClient
      .post<ApiResponse<CustomerGroupResponse>>(BASE, data)
      .then((r) => r.data)
  },

  /** PUT /api/v1/customer-groups/:id */
  update(id: string, data: UpdateCustomerGroupRequest) {
    return apiClient
      .put<ApiResponse<CustomerGroupResponse>>(`${BASE}/${id}`, data)
      .then((r) => r.data)
  },

  /** DELETE /api/v1/customer-groups/:id */
  delete(id: string) {
    return apiClient.delete(`${BASE}/${id}`)
  },
}
