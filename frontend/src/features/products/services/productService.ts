/**
 * features/products/services/productService.ts
 * API calls for the product domain.
 */
import { apiClient } from '@/services/axios'
import type { ApiResponse, PageResponse } from '@/types/api.types'
import type {
  ProductSummaryResponse,
  ProductResponse,
  CreateProductRequest,
  UpdateProductRequest,
  ProductFilterParams,
} from '@/types/product.types'

const BASE = '/products'

export const productService = {
  /** GET /api/v1/products */
  list(params: ProductFilterParams = {}) {
    return apiClient
      .get<ApiResponse<PageResponse<ProductSummaryResponse>>>(BASE, { params })
      .then((r) => r.data)
  },

  /** GET /api/v1/products/:id */
  getById(id: string) {
    return apiClient
      .get<ApiResponse<ProductResponse>>(`${BASE}/${id}`)
      .then((r) => r.data)
  },

  /** POST /api/v1/products */
  create(data: CreateProductRequest) {
    return apiClient
      .post<ApiResponse<ProductResponse>>(BASE, data)
      .then((r) => r.data)
  },

  /** PUT /api/v1/products/:id */
  update(id: string, data: UpdateProductRequest) {
    return apiClient
      .put<ApiResponse<ProductResponse>>(`${BASE}/${id}`, data)
      .then((r) => r.data)
  },

  /** DELETE /api/v1/products/:id */
  delete(id: string) {
    return apiClient.delete(`${BASE}/${id}`)
  },
}
