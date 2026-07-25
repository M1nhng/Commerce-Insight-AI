/**
 * features/inventory/services/warehouseService.ts
 * Axios API calls for warehouse management.
 */
import { apiClient } from '@/services/axios'
import type { ApiResponse, PageResponse } from '@/types/api.types'
import type {
  WarehouseResponse,
  WarehouseFilterParams,
  CreateWarehouseRequest,
  UpdateWarehouseRequest,
} from '@/types/inventory.types'

const BASE = '/warehouses'

export const warehouseService = {
  /** GET /api/v1/warehouses */
  list(params: WarehouseFilterParams = {}) {
    return apiClient
      .get<ApiResponse<PageResponse<WarehouseResponse>>>(BASE, { params })
      .then((r) => r.data)
  },

  /** GET /api/v1/warehouses/:id */
  getById(id: string) {
    return apiClient
      .get<ApiResponse<WarehouseResponse>>(`${BASE}/${id}`)
      .then((r) => r.data)
  },

  /** POST /api/v1/warehouses */
  create(data: CreateWarehouseRequest) {
    return apiClient
      .post<ApiResponse<WarehouseResponse>>(BASE, data)
      .then((r) => r.data)
  },

  /** PUT /api/v1/warehouses/:id */
  update(id: string, data: UpdateWarehouseRequest) {
    return apiClient
      .put<ApiResponse<WarehouseResponse>>(`${BASE}/${id}`, data)
      .then((r) => r.data)
  },

  /** DELETE /api/v1/warehouses/:id */
  delete(id: string) {
    return apiClient.delete(`${BASE}/${id}`)
  },
}
