/**
 * types/order.types.ts — Order domain type definitions.
 * Mirrors the Spring Boot Order domain DTOs exactly.
 */

// ── Enums ─────────────────────────────────────────────────────────────────

export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PROCESSING'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'REFUNDED'

export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED'

export type PaymentMethod = 'CASH' | 'BANK_TRANSFER' | 'CARD' | 'OTHER'

export type OrderAddressType = 'SHIPPING' | 'BILLING'

// ── Nested Response Types ─────────────────────────────────────────────────

export interface OrderItemResponse {
  id: string
  productId: string | null
  skuSnapshot: string
  productNameSnapshot: string
  unitPrice: number
  quantity: number
  discountAmount: number
  subtotal: number
}

export interface OrderAddressResponse {
  id: string
  type: OrderAddressType
  recipientName: string
  phone: string | null
  addressLine: string
  ward: string | null
  district: string | null
  province: string | null
  country: string
  createdAt: string
}

export interface PaymentResponse {
  id: string
  method: PaymentMethod
  status: PaymentStatus
  amount: number
  currency: string
  reference: string | null
  paidAt: string | null
  notes: string | null
  createdAt: string
}

export interface OrderStatusHistoryEntry {
  id: string
  fromStatus: OrderStatus | null
  toStatus: OrderStatus
  changedById: string | null
  changedByName: string | null
  reason: string | null
  createdAt: string
}

// ── Main Response Types ───────────────────────────────────────────────────

/** Lightweight — used in list views */
export interface OrderSummaryResponse {
  id: string
  orderNumber: string
  customerId: string | null
  customerName: string
  status: OrderStatus
  paymentStatus: PaymentStatus
  total: number
  currency: string
  itemCount: number
  createdAt: string
  updatedAt: string
}

/** Full detail — used in OrderDetailPage */
export interface OrderResponse {
  id: string
  orderNumber: string
  customerId: string | null
  customerName: string
  customerCode: string | null
  status: OrderStatus
  paymentStatus: PaymentStatus
  subtotal: number
  discount: number
  shippingFee: number
  tax: number
  total: number
  currency: string
  notes: string | null
  createdAt: string
  updatedAt: string
  shippedAt: string | null
  deliveredAt: string | null
  cancelledAt: string | null
  completedAt: string | null
  items: OrderItemResponse[]
  shippingAddress: OrderAddressResponse | null
  billingAddress: OrderAddressResponse | null
  payment: PaymentResponse | null
  statusHistory: OrderStatusHistoryEntry[]
}

// ── Request Types ─────────────────────────────────────────────────────────

export interface CreateOrderAddressRequest {
  type: OrderAddressType
  recipientName: string
  phone?: string | null
  addressLine: string
  ward?: string | null
  district?: string | null
  province?: string | null
  country?: string | null
}

export interface CreateOrderItemRequest {
  productId: string
  quantity: number
  discountAmount?: number | null
}

export interface CreateOrderRequest {
  customerId: string
  items: CreateOrderItemRequest[]
  shippingAddress?: CreateOrderAddressRequest | null
  billingAddress?: CreateOrderAddressRequest | null
  paymentMethod: PaymentMethod
  shippingFee?: number | null
  discount?: number | null
  tax?: number | null
  currency?: string | null
  notes?: string | null
}

export interface UpdateOrderStatusRequest {
  status: OrderStatus
  reason?: string | null
}

export interface CancelOrderRequest {
  reason?: string | null
}

// ── Filter Params ─────────────────────────────────────────────────────────

export interface OrderFilterParams {
  keyword?: string
  customerId?: string
  status?: OrderStatus
  paymentStatus?: PaymentStatus
  dateFrom?: string
  dateTo?: string
  page?: number
  size?: number
  sort?: string
}

// ── UI helpers ────────────────────────────────────────────────────────────

/** Next actions allowed from a given OrderStatus (mirrors backend state machine) */
export const ORDER_NEXT_ACTIONS: Record<OrderStatus, OrderStatus[]> = {
  PENDING:    ['CONFIRMED', 'CANCELLED'],
  CONFIRMED:  ['PROCESSING', 'CANCELLED'],
  PROCESSING: ['SHIPPED'],
  SHIPPED:    ['DELIVERED'],
  DELIVERED:  ['COMPLETED'],
  COMPLETED:  [],
  CANCELLED:  [],
  REFUNDED:   [],
}

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING:    'Pending',
  CONFIRMED:  'Confirmed',
  PROCESSING: 'Processing',
  SHIPPED:    'Shipped',
  DELIVERED:  'Delivered',
  COMPLETED:  'Completed',
  CANCELLED:  'Cancelled',
  REFUNDED:   'Refunded',
}

export const ORDER_ACTION_LABELS: Partial<Record<OrderStatus, string>> = {
  CONFIRMED:  'Confirm Order',
  PROCESSING: 'Start Processing',
  SHIPPED:    'Mark as Shipped',
  DELIVERED:  'Mark as Delivered',
  COMPLETED:  'Mark as Completed',
  CANCELLED:  'Cancel Order',
}

export const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  PENDING:  'Pending',
  PAID:     'Paid',
  FAILED:   'Failed',
  REFUNDED: 'Refunded',
}

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH:          'Cash',
  BANK_TRANSFER: 'Bank Transfer',
  CARD:          'Card',
  OTHER:         'Other',
}
