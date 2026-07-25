/**
 * features/products/hooks/useCategories.ts
 * TanStack Query hooks for the category domain.
 */
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { categoryService } from '../services/categoryService'
import type { CreateCategoryRequest, UpdateCategoryRequest } from '@/types/product.types'

// ── Query keys ────────────────────────────────────────────────────────────

export const CATEGORY_KEYS = {
  all:    ['categories'] as const,
  lists:  () => [...CATEGORY_KEYS.all, 'list'] as const,
  list:   (params: object) => [...CATEGORY_KEYS.lists(), params] as const,
  tree:   () => [...CATEGORY_KEYS.all, 'tree'] as const,
  detail: (id: string) => [...CATEGORY_KEYS.all, 'detail', id] as const,
}

// ── Query hooks ───────────────────────────────────────────────────────────

export function useCategoryList(params: { search?: string; page?: number; size?: number } = {}) {
  return useQuery({
    queryKey: CATEGORY_KEYS.list(params),
    queryFn: () => categoryService.list(params),
    select: (data) => data.data,
    staleTime: 60_000,
  })
}

export function useCategoryTree() {
  return useQuery({
    queryKey: CATEGORY_KEYS.tree(),
    queryFn: () => categoryService.tree(),
    select: (data) => data.data ?? [],
    staleTime: 60_000,
  })
}

// ── For select dropdowns — flat list of all categories ───────────────────
export function useCategoryOptions() {
  return useQuery({
    queryKey: CATEGORY_KEYS.list({ size: 200 }),
    queryFn: () => categoryService.list({ size: 200 }),
    select: (data) =>
      (data.data?.content ?? []).map((c) => ({ value: c.id, label: c.name })),
    staleTime: 120_000,
  })
}

// ── Mutation hooks ────────────────────────────────────────────────────────

export function useCreateCategory() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateCategoryRequest) => categoryService.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: CATEGORY_KEYS.all })
      toast.success('Category created successfully')
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.error?.message ?? 'Failed to create category'
      toast.error(msg)
    },
  })
}

export function useUpdateCategory() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateCategoryRequest }) =>
      categoryService.update(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: CATEGORY_KEYS.all })
      toast.success('Category updated successfully')
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.error?.message ?? 'Failed to update category'
      toast.error(msg)
    },
  })
}

export function useDeleteCategory() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => categoryService.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: CATEGORY_KEYS.all })
      toast.success('Category deleted successfully')
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.error?.message ?? 'Failed to delete category'
      toast.error(msg)
    },
  })
}
