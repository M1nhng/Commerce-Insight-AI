/**
 * features/customers/hooks/useCustomerAddresses.ts
 * TanStack Query hooks for the customer address sub-resource.
 */
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { customerAddressService } from '../services/customerAddressService'
import { CUSTOMER_KEYS } from './useCustomers'
import type { CreateAddressRequest, UpdateAddressRequest } from '@/types/customer.types'

// ── Query keys ────────────────────────────────────────────────────────────

export const CUSTOMER_ADDRESS_KEYS = {
  all:  (customerId: string) => ['customer-addresses', customerId] as const,
  list: (customerId: string) => [...CUSTOMER_ADDRESS_KEYS.all(customerId), 'list'] as const,
}

// ── Query hooks ───────────────────────────────────────────────────────────

export function useCustomerAddresses(customerId: string | null) {
  return useQuery({
    queryKey: CUSTOMER_ADDRESS_KEYS.list(customerId ?? ''),
    queryFn:  () => customerAddressService.list(customerId!),
    select:   (data) => data.data ?? [],
    enabled:  !!customerId,
    staleTime: 30_000,
  })
}

// ── Mutation hooks ────────────────────────────────────────────────────────

export function useAddAddress() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ customerId, data }: { customerId: string; data: CreateAddressRequest }) =>
      customerAddressService.add(customerId, data),
    onSuccess: (_, { customerId }) => {
      qc.invalidateQueries({ queryKey: CUSTOMER_ADDRESS_KEYS.list(customerId) })
      qc.invalidateQueries({ queryKey: CUSTOMER_KEYS.detail(customerId) })
      toast.success('Address added successfully')
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.error?.message ?? 'Failed to add address'
      toast.error(msg)
    },
  })
}

export function useUpdateAddress() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({
      customerId,
      addressId,
      data,
    }: { customerId: string; addressId: string; data: UpdateAddressRequest }) =>
      customerAddressService.update(customerId, addressId, data),
    onSuccess: (_, { customerId }) => {
      qc.invalidateQueries({ queryKey: CUSTOMER_ADDRESS_KEYS.list(customerId) })
      toast.success('Address updated successfully')
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.error?.message ?? 'Failed to update address'
      toast.error(msg)
    },
  })
}

export function useDeleteAddress() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ customerId, addressId }: { customerId: string; addressId: string }) =>
      customerAddressService.delete(customerId, addressId),
    onSuccess: (_, { customerId }) => {
      qc.invalidateQueries({ queryKey: CUSTOMER_ADDRESS_KEYS.list(customerId) })
      toast.success('Address deleted successfully')
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.error?.message ?? 'Failed to delete address'
      toast.error(msg)
    },
  })
}

export function useSetDefaultAddress() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ customerId, addressId }: { customerId: string; addressId: string }) =>
      customerAddressService.setDefault(customerId, addressId),
    onSuccess: (_, { customerId }) => {
      qc.invalidateQueries({ queryKey: CUSTOMER_ADDRESS_KEYS.list(customerId) })
      toast.success('Default address updated')
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.error?.message ?? 'Failed to set default address'
      toast.error(msg)
    },
  })
}
