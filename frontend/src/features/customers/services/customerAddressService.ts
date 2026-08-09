/**
 * features/customers/services/customerAddressService.ts
 * API calls for the customer address sub-resource.
 */
import { apiClient } from '@/services/axios'
import type { ApiResponse } from '@/types/api.types'
import type {
  CustomerAddressResponse,
  CreateAddressRequest,
  UpdateAddressRequest,
} from '@/types/customer.types'

const base = (customerId: string) => `/customers/${customerId}/addresses`

export const customerAddressService = {
  /** GET /api/v1/customers/:id/addresses */
  list(customerId: string) {
    return apiClient
      .get<ApiResponse<CustomerAddressResponse[]>>(base(customerId))
      .then((r) => r.data)
  },

  /** POST /api/v1/customers/:id/addresses */
  add(customerId: string, data: CreateAddressRequest) {
    return apiClient
      .post<ApiResponse<CustomerAddressResponse>>(base(customerId), data)
      .then((r) => r.data)
  },

  /** PUT /api/v1/customers/:id/addresses/:addressId */
  update(customerId: string, addressId: string, data: UpdateAddressRequest) {
    return apiClient
      .put<ApiResponse<CustomerAddressResponse>>(`${base(customerId)}/${addressId}`, data)
      .then((r) => r.data)
  },

  /** DELETE /api/v1/customers/:id/addresses/:addressId */
  delete(customerId: string, addressId: string) {
    return apiClient.delete(`${base(customerId)}/${addressId}`)
  },

  /** PATCH /api/v1/customers/:id/addresses/:addressId/default */
  setDefault(customerId: string, addressId: string) {
    return apiClient
      .patch<ApiResponse<CustomerAddressResponse>>(`${base(customerId)}/${addressId}/default`)
      .then((r) => r.data)
  },
}
