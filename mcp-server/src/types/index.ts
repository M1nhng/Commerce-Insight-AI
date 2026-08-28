/**
 * Shared TypeScript types for the MCP Server.
 *
 * These interfaces mirror the backend DTO contracts.
 * Update in sync with any backend DTO changes.
 */

// ── Standard API envelope ─────────────────────────────────────────────────────

export interface ApiResponse<T> {
  success:   boolean;
  data:      T;
  message:   string;
  timestamp: string;
}

export interface PageResponse<T> {
  content:          T[];
  totalElements:    number;
  totalPages:       number;
  number:           number;   // current page (0-indexed)
  size:             number;
  first:            boolean;
  last:             boolean;
  numberOfElements: number;
}

// ── Customer domain types ─────────────────────────────────────────────────────

export type CustomerStatus = 'ACTIVE' | 'INACTIVE' | 'BLOCKED';
export type CustomerGender = 'MALE' | 'FEMALE' | 'OTHER' | 'PREFER_NOT_TO_SAY';
export type AddressType    = 'SHIPPING' | 'BILLING';
export type GroupStatus    = 'ACTIVE' | 'INACTIVE';

export interface CustomerSummary {
  id:           string;
  customerCode: string;
  fullName:     string;
  firstName:    string;
  lastName:     string;
  email:        string | null;
  phone:        string | null;
  status:       CustomerStatus;
  groupId:      string | null;
  groupName:    string | null;
  createdAt:    string;
}

export interface CustomerAddress {
  id:            string;
  type:          AddressType;
  recipientName: string;
  phone:         string | null;
  addressLine:   string;
  ward:          string | null;
  district:      string | null;
  province:      string | null;
  country:       string;
  isDefault:     boolean;
}

export interface CustomerProfile extends CustomerSummary {
  dateOfBirth: string | null;
  gender:      CustomerGender | null;
  addresses:   CustomerAddress[];
  updatedAt:   string;
}

export interface CustomerGroup {
  id:          string;
  code:        string;
  name:        string;
  description: string | null;
  status:      GroupStatus;
  createdAt:   string;
  updatedAt:   string;
}

// ── Order domain types ────────────────────────────────────────────────────────

export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PROCESSING'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'REFUNDED';

export type PaymentStatus  = 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED';
export type PaymentMethod  = 'CASH' | 'BANK_TRANSFER' | 'CARD' | 'OTHER';
export type OrderAddressType = 'SHIPPING' | 'BILLING';

/** Lightweight list-view row — mirrors OrderSummaryResponse */
export interface OrderSummary {
  id:            string;
  orderNumber:   string;
  customerId:    string | null;
  customerName:  string;
  status:        OrderStatus;
  paymentStatus: PaymentStatus;
  total:         number;
  currency:      string;
  itemCount:     number;
  createdAt:     string;
  updatedAt:     string;
}

/** Line item — mirrors OrderItemResponse */
export interface OrderItem {
  id:                  string;
  productId:           string | null;
  skuSnapshot:         string;
  productNameSnapshot: string;
  unitPrice:           number;
  quantity:            number;
  discountAmount:      number;
  subtotal:            number;
}

/** Address snapshot — mirrors OrderAddressResponse */
export interface OrderAddress {
  id:            string;
  type:          OrderAddressType;
  recipientName: string;
  phone:         string | null;
  addressLine:   string;
  ward:          string | null;
  district:      string | null;
  province:      string | null;
  country:       string;
  createdAt:     string;
}

/** Payment record — mirrors PaymentResponse */
export interface Payment {
  id:        string;
  method:    PaymentMethod;
  status:    PaymentStatus;
  amount:    number;
  currency:  string;
  reference: string | null;
  paidAt:    string | null;
  notes:     string | null;
}

/** One entry in the status audit trail — mirrors OrderStatusHistoryResponse */
export interface OrderStatusHistory {
  id:            string;
  fromStatus:    OrderStatus | null;
  toStatus:      OrderStatus;
  changedById:   string | null;
  changedByName: string | null;
  reason:        string | null;
  createdAt:     string;
}

/** Full order detail — mirrors OrderResponse */
export interface OrderDetail extends OrderSummary {
  customerCode:    string | null;
  subtotal:        number;
  discount:        number;
  shippingFee:     number;
  tax:             number;
  notes:           string | null;
  shippedAt:       string | null;
  deliveredAt:     string | null;
  cancelledAt:     string | null;
  completedAt:     string | null;
  items:           OrderItem[];
  shippingAddress: OrderAddress | null;
  billingAddress:  OrderAddress | null;
  payment:         Payment | null;
  statusHistory:   OrderStatusHistory[];
}

/** Single line item in a CreateOrderRequest */
export interface CreateOrderItemRequest {
  productId:      string;   // UUID
  quantity:       number;   // 1..10,000
  discountAmount: number | null;  // optional, defaults to 0 server-side
}

/** Address snapshot in a CreateOrderRequest */
export interface CreateOrderAddressRequest {
  type:          OrderAddressType;
  recipientName: string;
  phone:         string | null;
  addressLine:   string;
  ward:          string | null;
  district:      string | null;
  province:      string | null;
  country:       string | null;
}

/** Full create order payload — mirrors CreateOrderRequest */
export interface CreateOrderRequest {
  customerId:      string;        // UUID — must be ACTIVE
  items:           CreateOrderItemRequest[];
  shippingAddress: CreateOrderAddressRequest | null;
  billingAddress:  CreateOrderAddressRequest | null;
  paymentMethod:   PaymentMethod;
  shippingFee:     number | null; // optional, defaults to 0
  discount:        number | null; // order-level discount, defaults to 0
  tax:             number | null; // tax amount, defaults to 0
  currency:        string | null; // ISO 4217, defaults to VND
  notes:           string | null;
}

/** Filter params for GET /orders */
export interface OrderSearchParams {
  keyword?:       string;
  customerId?:    string;
  status?:        OrderStatus;
  paymentStatus?: PaymentStatus;
  dateFrom?:      string; // YYYY-MM-DD
  dateTo?:        string; // YYYY-MM-DD
  page?:          number;
  size?:          number;
  sort?:          string;
}

/** Paginated response wrapping OrderSummary */
export interface OrderSearchResponse {
  totalElements: number;
  totalPages:    number;
  currentPage:   number;
  pageSize:      number;
  orders:        OrderSummary[];
}

// ── Analytics domain types ────────────────────────────────────────────────────
// These types mirror the Spring Boot analytics DTO contracts exactly.
// Source: src/main/java/com/commerceinsight/analytics/dto/

export type RevenueGroupBy = 'DAY' | 'WEEK' | 'MONTH';

/** Mirrors OverviewResponse — high-level KPI snapshot */
export interface AnalyticsOverview {
  totalRevenue:       number;
  totalOrders:        number;
  totalCustomers:     number;
  totalProductsSold:  number;
  averageOrderValue:  number;
  cancelledOrders:    number;
  cancellationRate:   number;
  currency:           string;
  dateFrom:           string | null;
  dateTo:             string | null;
}

/** Mirrors RevenuePeriodResponse — one time-series data point */
export interface RevenuePeriod {
  period:  string;   // "2026-08-01" | "2026-W32" | "2026-08"
  revenue: number;
  orders:  number;
}

/** Mirrors RevenueResponse — complete revenue time series */
export interface RevenueAnalytics {
  groupBy:  string;
  currency: string;
  dateFrom: string | null;
  dateTo:   string | null;
  data:     RevenuePeriod[];
}

/** Mirrors OrderAnalyticsResponse — per-status breakdown + rates */
export interface OrderAnalytics {
  totalOrders:       number;
  pendingOrders:     number;
  confirmedOrders:   number;
  processingOrders:  number;
  shippedOrders:     number;
  deliveredOrders:   number;
  completedOrders:   number;
  cancelledOrders:   number;
  completionRate:    number;
  cancellationRate:  number;
  dateFrom:          string | null;
  dateTo:            string | null;
}

/** Mirrors TopProductEntry — one ranked product */
export interface TopProductAnalytics {
  productId:    string | null;  // null when product has been deleted
  sku:          string;
  productName:  string;
  quantitySold: number;
  revenue:      number;
}

/** Mirrors CustomerAnalyticsResponse — engagement metrics */
export interface CustomerAnalytics {
  uniqueCustomers:          number;
  newCustomers:             number;
  repeatCustomers:          number;
  averageOrdersPerCustomer: number;
  dateFrom:                 string | null;
  dateTo:                   string | null;
}

/** Mirrors PaymentMethodStats — aggregated stats for one payment method */
export interface PaymentMethodAnalytics {
  orders: number;
  amount: number;
}

/** Mirrors PaymentAnalyticsResponse — breakdown keyed by payment method */
export interface PaymentAnalytics {
  currency:  string;
  breakdown: Record<string, PaymentMethodAnalytics>;  // keys: CASH | BANK_TRANSFER | CARD | OTHER
  dateFrom:  string | null;
  dateTo:    string | null;
}

