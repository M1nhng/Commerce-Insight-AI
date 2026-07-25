/**
 * features/inventory/hooks/useInventory.ts
 * TanStack Query hooks for the inventory domain.
 */
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { inventoryService } from '../services/inventoryService'
import type {
  InventoryFilterParams,
  AdjustStockRequest,
  TransferStockRequest,
} from '@/types/inventory.types'
import { WAREHOUSE_KEYS } from './useWarehouses'

// ── Query keys ────────────────────────────────────────────────────────────

export const INVENTORY_KEYS = {
  all:          ['inventory'] as const,
  lists:        () => [...INVENTORY_KEYS.all, 'list'] as const,
  list:         (params: InventoryFilterParams) => [...INVENTORY_KEYS.lists(), params] as const,
  detail:       (id: string) => [...INVENTORY_KEYS.all, 'detail', id] as const,
  byProduct:    (productId: string) => [...INVENTORY_KEYS.all, 'product', productId] as const,
  lowStock:     () => [...INVENTORY_KEYS.all, 'low-stock'] as const,
  transactions: (id: string) => [...INVENTORY_KEYS.all, 'transactions', id] as const,
}

// ── Query hooks ───────────────────────────────────────────────────────────

export function useInventoryList(params: InventoryFilterParams = {}) {
  return useQuery({
    queryKey: INVENTORY_KEYS.list(params),
    queryFn:  () => inventoryService.list(params),
    select:   (data) => data.data,
    staleTime: 30_000,
  })
}

export function useInventoryById(id: string | null) {
  return useQuery({
    queryKey: INVENTORY_KEYS.detail(id ?? ''),
    queryFn:  () => inventoryService.getById(id!),
    select:   (data) => data.data,
    enabled:  !!id,
    staleTime: 30_000,
  })
}

export function useInventoryByProduct(productId: string | null) {
  return useQuery({
    queryKey: INVENTORY_KEYS.byProduct(productId ?? ''),
    queryFn:  () => inventoryService.getByProduct(productId!),
    select:   (data) => data.data ?? [],
    enabled:  !!productId,
    staleTime: 30_000,
  })
}

export function useLowStock() {
  return useQuery({
    queryKey: INVENTORY_KEYS.lowStock(),
    queryFn:  () => inventoryService.getLowStock(),
    select:   (data) => data.data ?? [],
    staleTime: 60_000,
  })
}

export function useInventoryTransactions(inventoryId: string | null, page = 0) {
  return useQuery({
    queryKey: [...INVENTORY_KEYS.transactions(inventoryId ?? ''), page],
    queryFn:  () => inventoryService.getTransactions(inventoryId!, page),
    select:   (data) => data.data,
    enabled:  !!inventoryId,
    staleTime: 30_000,
  })
}

// ── Mutation hooks ────────────────────────────────────────────────────────

export function useAdjustStock() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: AdjustStockRequest }) =>
      inventoryService.adjust(id, data),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: INVENTORY_KEYS.lists() })
      qc.invalidateQueries({ queryKey: INVENTORY_KEYS.detail(id) })
      qc.invalidateQueries({ queryKey: INVENTORY_KEYS.lowStock() })
      toast.success('Stock adjusted successfully')
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.error?.message ?? 'Failed to adjust stock'
      toast.error(msg)
    },
  })
}

export function useTransferStock() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: TransferStockRequest) => inventoryService.transfer(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: INVENTORY_KEYS.lists() })
      qc.invalidateQueries({ queryKey: INVENTORY_KEYS.lowStock() })
      // Warehouse data may change if new inventory rows were created
      qc.invalidateQueries({ queryKey: WAREHOUSE_KEYS.all })
      toast.success('Stock transferred successfully')
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.error?.message ?? 'Failed to transfer stock'
      toast.error(msg)
    },
  })
}
