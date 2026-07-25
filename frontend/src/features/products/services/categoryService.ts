/**
 * features/products/services/categoryService.ts
 * API calls for the category domain.
 */
import { apiClient } from '@/services/axios'
import type { ApiResponse, PageResponse } from '@/types/api.types'
import type {
  CategoryResponse,
  CategoryTreeResponse,
  CreateCategoryRequest,
  UpdateCategoryRequest,
} from '@/types/product.types'

const BASE = '/categories'

export const categoryService = {
  /** GET /api/v1/categories — flat paginated list */
  list(params: { search?: string; page?: number; size?: number } = {}) {
    return apiClient
      .get<ApiResponse<PageResponse<CategoryResponse>>>(BASE, { params })
      .then((r) => r.data)
  },

  /** GET /api/v1/categories/tree — full hierarchical tree */
  tree() {
    return apiClient
      .get<ApiResponse<CategoryTreeResponse[]>>(`${BASE}/tree`)
      .then((r) => r.data)
  },

  /** GET /api/v1/categories/:id */
  getById(id: string) {
    return apiClient
      .get<ApiResponse<CategoryResponse>>(`${BASE}/${id}`)
      .then((r) => r.data)
  },

  /** POST /api/v1/categories */
  create(data: CreateCategoryRequest) {
    return apiClient
      .post<ApiResponse<CategoryResponse>>(BASE, data)
      .then((r) => r.data)
  },

  /** PUT /api/v1/categories/:id */
  update(id: string, data: UpdateCategoryRequest) {
    return apiClient
      .put<ApiResponse<CategoryResponse>>(`${BASE}/${id}`, data)
      .then((r) => r.data)
  },

  /** DELETE /api/v1/categories/:id */
  delete(id: string) {
    return apiClient.delete(`${BASE}/${id}`)
  },
}
