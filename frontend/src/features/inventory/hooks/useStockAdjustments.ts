/**
 * features/inventory/hooks/useStockAdjustments.ts
 * TanStack Query hooks for the stock adjustment approval workflow.
 */
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { stockAdjustmentService } from '../services/stockAdjustmentService'
import type {
  StockAdjustmentFilterParams,
  RequestStockAdjustmentRequest,
  ReviewStockAdjustmentRequest,
} from '@/types/inventory.types'
import { INVENTORY_KEYS } from './useInventory'

// ── Query keys ────────────────────────────────────────────────────────────

export const ADJUSTMENT_KEYS = {
  all:   ['stock-adjustments'] as const,
  lists: () => [...ADJUSTMENT_KEYS.all, 'list'] as const,
  list:  (params: StockAdjustmentFilterParams) => [...ADJUSTMENT_KEYS.lists(), params] as const,
}

// ── Query hooks ───────────────────────────────────────────────────────────

export function useStockAdjustments(params: StockAdjustmentFilterParams = {}) {
  return useQuery({
    queryKey: ADJUSTMENT_KEYS.list(params),
    queryFn:  () => stockAdjustmentService.list(params),
    select:   (data) => data.data,
    staleTime: 30_000,
  })
}

// ── Mutation hooks ────────────────────────────────────────────────────────

export function useRequestAdjustment() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: RequestStockAdjustmentRequest) =>
      stockAdjustmentService.request(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ADJUSTMENT_KEYS.lists() })
      toast.success('Adjustment request submitted — awaiting approval')
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.error?.message ?? 'Failed to submit adjustment request'
      toast.error(msg)
    },
  })
}

export function useApproveAdjustment() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data?: ReviewStockAdjustmentRequest }) =>
      stockAdjustmentService.approve(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ADJUSTMENT_KEYS.lists() })
      qc.invalidateQueries({ queryKey: INVENTORY_KEYS.lists() })
      qc.invalidateQueries({ queryKey: INVENTORY_KEYS.lowStock() })
      toast.success('Adjustment approved — stock updated')
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.error?.message ?? 'Failed to approve adjustment'
      toast.error(msg)
    },
  })
}

export function useRejectAdjustment() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data?: ReviewStockAdjustmentRequest }) =>
      stockAdjustmentService.reject(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ADJUSTMENT_KEYS.lists() })
      toast.success('Adjustment rejected')
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.error?.message ?? 'Failed to reject adjustment'
      toast.error(msg)
    },
  })
}
