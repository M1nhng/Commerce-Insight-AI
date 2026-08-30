/**
 * features/customers/hooks/useCustomers.ts
 * TanStack Query hooks for the customer domain.
 */
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { getErrorMessage } from '@/lib/apiError'
import { customerService } from '../services/customerService'
import { CUSTOMER_GROUP_KEYS } from './useCustomerGroups'
import type {
  CustomerFilterParams,
  CreateCustomerRequest,
  UpdateCustomerRequest,
  UpdateCustomerStatusRequest,
} from '@/types/customer.types'

// ── Query keys ────────────────────────────────────────────────────────────

export const CUSTOMER_KEYS = {
  all:    ['customers'] as const,
  lists:  () => [...CUSTOMER_KEYS.all, 'list'] as const,
  list:   (params: CustomerFilterParams) => [...CUSTOMER_KEYS.lists(), params] as const,
  detail: (id: string) => [...CUSTOMER_KEYS.all, 'detail', id] as const,
}

// ── Query hooks ───────────────────────────────────────────────────────────

export function useCustomers(params: CustomerFilterParams = {}) {
  return useQuery({
    queryKey: CUSTOMER_KEYS.list(params),
    queryFn:  () => customerService.list(params),
    select:   (data) => data.data,
    staleTime: 30_000,
  })
}

export function useCustomer(id: string | null) {
  return useQuery({
    queryKey: CUSTOMER_KEYS.detail(id ?? ''),
    queryFn:  () => customerService.getById(id!),
    select:   (data) => data.data,
    enabled:  !!id,
    staleTime: 60_000,
  })
}

// ── Mutation hooks ────────────────────────────────────────────────────────

export function useCreateCustomer() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateCustomerRequest) => customerService.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: CUSTOMER_KEYS.lists() })
      toast.success('Customer created successfully')
    },
    onError: (err) => {
      toast.error(getErrorMessage(err))
    },
  })
}

export function useUpdateCustomer() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateCustomerRequest }) =>
      customerService.update(id, data),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: CUSTOMER_KEYS.lists() })
      qc.invalidateQueries({ queryKey: CUSTOMER_KEYS.detail(id) })
      toast.success('Customer updated successfully')
    },
    onError: (err) => {
      toast.error(getErrorMessage(err))
    },
  })
}

export function useDeleteCustomer() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => customerService.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: CUSTOMER_KEYS.lists() })
      toast.success('Customer deleted successfully')
    },
    onError: (err) => {
      toast.error(getErrorMessage(err))
    },
  })
}

export function useUpdateCustomerStatus() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateCustomerStatusRequest }) =>
      customerService.updateStatus(id, data),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: CUSTOMER_KEYS.lists() })
      qc.invalidateQueries({ queryKey: CUSTOMER_KEYS.detail(id) })
      // Group options may embed customer count in future, invalidate to be safe
      qc.invalidateQueries({ queryKey: CUSTOMER_GROUP_KEYS.all })
      toast.success('Customer status updated')
    },
    onError: (err) => {
      toast.error(getErrorMessage(err))
    },
  })
}
