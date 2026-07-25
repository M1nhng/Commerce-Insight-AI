/**
 * features/products/hooks/useProducts.ts
 * TanStack Query hooks for the product domain.
 */
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { productService } from '../services/productService'
import type { ProductFilterParams, CreateProductRequest, UpdateProductRequest } from '@/types/product.types'

// ── Query keys ────────────────────────────────────────────────────────────

export const PRODUCT_KEYS = {
  all:    ['products'] as const,
  lists:  () => [...PRODUCT_KEYS.all, 'list'] as const,
  list:   (params: ProductFilterParams) => [...PRODUCT_KEYS.lists(), params] as const,
  detail: (id: string) => [...PRODUCT_KEYS.all, 'detail', id] as const,
}

// ── Query hooks ───────────────────────────────────────────────────────────

export function useProducts(params: ProductFilterParams) {
  return useQuery({
    queryKey: PRODUCT_KEYS.list(params),
    queryFn: () => productService.list(params),
    select: (data) => data.data,
    staleTime: 30_000,
  })
}

export function useProduct(id: string | null) {
  return useQuery({
    queryKey: PRODUCT_KEYS.detail(id ?? ''),
    queryFn: () => productService.getById(id!),
    select: (data) => data.data,
    enabled: !!id,
    staleTime: 60_000,
  })
}

// ── Mutation hooks ────────────────────────────────────────────────────────

export function useCreateProduct() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateProductRequest) => productService.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: PRODUCT_KEYS.lists() })
      toast.success('Product created successfully')
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.error?.message ?? 'Failed to create product'
      toast.error(msg)
    },
  })
}

export function useUpdateProduct() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateProductRequest }) =>
      productService.update(id, data),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: PRODUCT_KEYS.lists() })
      qc.invalidateQueries({ queryKey: PRODUCT_KEYS.detail(id) })
      toast.success('Product updated successfully')
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.error?.message ?? 'Failed to update product'
      toast.error(msg)
    },
  })
}

export function useDeleteProduct() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => productService.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: PRODUCT_KEYS.lists() })
      toast.success('Product deleted successfully')
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.error?.message ?? 'Failed to delete product'
      toast.error(msg)
    },
  })
}
