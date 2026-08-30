/**
 * features/orders/hooks/useOrders.ts
 * TanStack Query hooks for the order domain.
 */
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { getErrorMessage } from '@/lib/apiError'
import { orderService } from '../services/orderService'
import type {
  OrderFilterParams,
  CreateOrderRequest,
  UpdateOrderStatusRequest,
  CancelOrderRequest,
} from '@/types/order.types'

// ── Query keys ────────────────────────────────────────────────────────────

export const ORDER_KEYS = {
  all:    ['orders'] as const,
  lists:  () => [...ORDER_KEYS.all, 'list'] as const,
  list:   (params: OrderFilterParams) => [...ORDER_KEYS.lists(), params] as const,
  detail: (id: string) => [...ORDER_KEYS.all, 'detail', id] as const,
}

// ── Query hooks ───────────────────────────────────────────────────────────

export function useOrders(params: OrderFilterParams = {}) {
  return useQuery({
    queryKey: ORDER_KEYS.list(params),
    queryFn:  () => orderService.list(params),
    select:   (data) => data.data,
    staleTime: 30_000,
  })
}

export function useOrder(id: string | null) {
  return useQuery({
    queryKey: ORDER_KEYS.detail(id ?? ''),
    queryFn:  () => orderService.getById(id!),
    select:   (data) => data.data,
    enabled:  !!id,
    staleTime: 60_000,
  })
}

// ── Mutation hooks ────────────────────────────────────────────────────────

export function useCreateOrder() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateOrderRequest) => orderService.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ORDER_KEYS.lists() })
      toast.success('Order created successfully')
    },
    onError: (err) => {
      toast.error(getErrorMessage(err))
    },
  })
}

export function useUpdateOrderStatus() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateOrderStatusRequest }) =>
      orderService.updateStatus(id, data),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: ORDER_KEYS.lists() })
      qc.invalidateQueries({ queryKey: ORDER_KEYS.detail(id) })
      toast.success('Order status updated')
    },
    onError: (err) => {
      toast.error(getErrorMessage(err))
    },
  })
}

export function useCancelOrder() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data?: CancelOrderRequest }) =>
      orderService.cancel(id, data),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: ORDER_KEYS.lists() })
      qc.invalidateQueries({ queryKey: ORDER_KEYS.detail(id) })
      toast.success('Order cancelled')
    },
    onError: (err) => {
      toast.error(getErrorMessage(err))
    },
  })
}
