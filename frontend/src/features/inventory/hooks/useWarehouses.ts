/**
 * features/inventory/hooks/useWarehouses.ts
 * TanStack Query hooks for warehouse management.
 */
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { getErrorMessage } from '@/lib/apiError'
import { warehouseService } from '../services/warehouseService'
import type {
  WarehouseFilterParams,
  CreateWarehouseRequest,
  UpdateWarehouseRequest,
} from '@/types/inventory.types'

// ── Query keys ────────────────────────────────────────────────────────────

export const WAREHOUSE_KEYS = {
  all:    ['warehouses'] as const,
  lists:  () => [...WAREHOUSE_KEYS.all, 'list'] as const,
  list:   (params: WarehouseFilterParams) => [...WAREHOUSE_KEYS.lists(), params] as const,
  detail: (id: string) => [...WAREHOUSE_KEYS.all, 'detail', id] as const,
}

// ── Query hooks ───────────────────────────────────────────────────────────

export function useWarehouses(params: WarehouseFilterParams = {}) {
  return useQuery({
    queryKey: WAREHOUSE_KEYS.list(params),
    queryFn:  () => warehouseService.list(params),
    select:   (data) => data.data,
    staleTime: 60_000,
  })
}

export function useWarehouse(id: string | null) {
  return useQuery({
    queryKey: WAREHOUSE_KEYS.detail(id ?? ''),
    queryFn:  () => warehouseService.getById(id!),
    select:   (data) => data.data,
    enabled:  !!id,
    staleTime: 60_000,
  })
}

/**
 * Simplified list suitable for <Select> options — fetches all warehouses (large page).
 */
export function useWarehouseOptions() {
  return useQuery({
    queryKey: [...WAREHOUSE_KEYS.lists(), 'options'],
    queryFn:  () => warehouseService.list({ size: 100, sortBy: 'name', sortDir: 'asc' }),
    select:   (data) => data.data?.content ?? [],
    staleTime: 120_000,
  })
}

// ── Mutation hooks ────────────────────────────────────────────────────────

export function useCreateWarehouse() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateWarehouseRequest) => warehouseService.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: WAREHOUSE_KEYS.lists() })
      toast.success('Warehouse created successfully')
    },
    onError: (err) => {
      toast.error(getErrorMessage(err))
    },
  })
}

export function useUpdateWarehouse() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateWarehouseRequest }) =>
      warehouseService.update(id, data),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: WAREHOUSE_KEYS.lists() })
      qc.invalidateQueries({ queryKey: WAREHOUSE_KEYS.detail(id) })
      toast.success('Warehouse updated successfully')
    },
    onError: (err) => {
      toast.error(getErrorMessage(err))
    },
  })
}

export function useDeleteWarehouse() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => warehouseService.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: WAREHOUSE_KEYS.lists() })
      toast.success('Warehouse deleted successfully')
    },
    onError: (err) => {
      toast.error(getErrorMessage(err))
    },
  })
}
