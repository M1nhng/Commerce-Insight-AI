/**
 * types/customer.types.ts — Customer domain type definitions.
 * Mirrors the Spring Boot Customer domain DTOs.
 */

// ── Enums ─────────────────────────────────────────────────────────────────

export type CustomerStatus = 'ACTIVE' | 'INACTIVE' | 'BLOCKED'

export type CustomerGender = 'MALE' | 'FEMALE' | 'OTHER' | 'PREFER_NOT_TO_SAY'

export type AddressType = 'SHIPPING' | 'BILLING'

export type GroupStatus = 'ACTIVE' | 'INACTIVE'

// ── Customer Group Types ──────────────────────────────────────────────────

export interface CustomerGroupResponse {
  id: string
  code: string
  name: string
  description: string | null
  status: GroupStatus
  createdAt: string
  updatedAt: string
}

export interface CreateCustomerGroupRequest {
  code: string
  name: string
  description?: string | null
  status?: GroupStatus
}

export interface UpdateCustomerGroupRequest {
  name?: string | null
  description?: string | null
  status?: GroupStatus
}

// ── Customer Address Types ────────────────────────────────────────────────

export interface CustomerAddressResponse {
  id: string
  customerId: string
  type: AddressType
  recipientName: string
  phone: string | null
  addressLine: string
  ward: string | null
  district: string | null
  province: string | null
  country: string
  isDefault: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateAddressRequest {
  type: AddressType
  recipientName: string
  phone?: string | null
  addressLine: string
  ward?: string | null
  district?: string | null
  province?: string | null
  country?: string | null
  isDefault?: boolean
}

export interface UpdateAddressRequest {
  recipientName?: string | null
  phone?: string | null
  addressLine?: string | null
  ward?: string | null
  district?: string | null
  province?: string | null
  country?: string | null
}

// ── Customer Types ────────────────────────────────────────────────────────

export interface CustomerSummaryResponse {
  id: string
  customerCode: string
  firstName: string
  lastName: string
  fullName: string
  email: string | null
  phone: string | null
  status: CustomerStatus
  groupId: string | null
  groupName: string | null
  createdAt: string
}

export interface CustomerResponse {
  id: string
  customerCode: string
  firstName: string
  lastName: string
  fullName: string
  email: string | null
  phone: string | null
  dateOfBirth: string | null
  gender: CustomerGender | null
  status: CustomerStatus
  groupId: string | null
  groupName: string | null
  addresses: CustomerAddressResponse[]
  createdAt: string
  updatedAt: string
}

export interface CreateCustomerRequest {
  customerCode?: string | null
  firstName: string
  lastName: string
  email?: string | null
  phone?: string | null
  dateOfBirth?: string | null
  gender?: CustomerGender | null
  groupId?: string | null
}

export interface UpdateCustomerRequest {
  firstName?: string | null
  lastName?: string | null
  email?: string | null
  phone?: string | null
  dateOfBirth?: string | null
  gender?: CustomerGender | null
  groupId?: string | null
}

export interface UpdateCustomerStatusRequest {
  status: CustomerStatus
}

// ── Filter Params ─────────────────────────────────────────────────────────

export interface CustomerFilterParams {
  keyword?: string
  status?: CustomerStatus
  groupId?: string
  startDate?: string
  endDate?: string
  page?: number
  size?: number
  sortBy?: string
  sortDir?: string
}

export interface CustomerGroupFilterParams {
  page?: number
  size?: number
  sortBy?: string
  sortDir?: string
}
