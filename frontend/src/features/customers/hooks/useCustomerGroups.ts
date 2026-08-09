/**
 * features/customers/hooks/useCustomerGroups.ts
 * TanStack Query hooks for the customer group domain.
 */
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { customerGroupService } from '../services/customerGroupService'
import type {
  CustomerGroupFilterParams,
  CreateCustomerGroupRequest,
  UpdateCustomerGroupRequest,
} from '@/types/customer.types'

// ── Query keys ────────────────────────────────────────────────────────────

export const CUSTOMER_GROUP_KEYS = {
  all:    ['customer-groups'] as const,
  lists:  () => [...CUSTOMER_GROUP_KEYS.all, 'list'] as const,
  list:   (params: CustomerGroupFilterParams) => [...CUSTOMER_GROUP_KEYS.lists(), params] as const,
  detail: (id: string) => [...CUSTOMER_GROUP_KEYS.all, 'detail', id] as const,
}

// ── Query hooks ───────────────────────────────────────────────────────────

export function useCustomerGroups(params: CustomerGroupFilterParams = {}) {
  return useQuery({
    queryKey: CUSTOMER_GROUP_KEYS.list(params),
    queryFn:  () => customerGroupService.list(params),
    select:   (data) => data.data,
    staleTime: 60_000,
  })
}

export function useCustomerGroup(id: string | null) {
  return useQuery({
    queryKey: CUSTOMER_GROUP_KEYS.detail(id ?? ''),
    queryFn:  () => customerGroupService.getById(id!),
    select:   (data) => data.data,
    enabled:  !!id,
    staleTime: 60_000,
  })
}

/** Lightweight list of all active groups — used for selects/dropdowns. */
export function useCustomerGroupOptions() {
  return useQuery({
    queryKey: [...CUSTOMER_GROUP_KEYS.all, 'options'],
    queryFn:  () => customerGroupService.list({ size: 200, sortBy: 'name', sortDir: 'asc' }),
    select:   (data) =>
      (data.data?.content ?? []).map((g) => ({ value: g.id, label: g.name, code: g.code })),
    staleTime: 120_000,
  })
}

// ── Mutation hooks ────────────────────────────────────────────────────────

export function useCreateCustomerGroup() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateCustomerGroupRequest) => customerGroupService.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: CUSTOMER_GROUP_KEYS.lists() })
      toast.success('Customer group created successfully')
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.error?.message ?? 'Failed to create customer group'
      toast.error(msg)
    },
  })
}

export function useUpdateCustomerGroup() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateCustomerGroupRequest }) =>
      customerGroupService.update(id, data),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: CUSTOMER_GROUP_KEYS.lists() })
      qc.invalidateQueries({ queryKey: CUSTOMER_GROUP_KEYS.detail(id) })
      toast.success('Customer group updated successfully')
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.error?.message ?? 'Failed to update customer group'
      toast.error(msg)
    },
  })
}

export function useDeleteCustomerGroup() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => customerGroupService.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: CUSTOMER_GROUP_KEYS.lists() })
      qc.invalidateQueries({ queryKey: [...CUSTOMER_GROUP_KEYS.all, 'options'] })
      toast.success('Customer group deleted successfully')
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.error?.message ?? 'Failed to delete customer group'
      toast.error(msg)
    },
  })
}
