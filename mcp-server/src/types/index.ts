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

// ── Data Import domain types (Sprint 10C) ────────────────────────────────────
// Mirror the Spring Boot dataimport DTO contracts exactly.
// Source: backend/src/main/java/com/commerceinsight/dataimport/
//   - domain/ImportType.java, domain/ImportFileType.java, domain/ImportJobStatus.java
//   - dto/response/ImportJobResponse.java
//   - dto/response/ImportJobSummaryResponse.java
//   - dto/response/ImportErrorResponse.java
// The MCP server NEVER parses files or runs import logic — these types only
// describe what the backend REST API already returns.

/** Mirrors the ImportType enum — the domain being imported. */
export type ImportType = 'PRODUCT' | 'CUSTOMER' | 'ORDER';

/** Mirrors the ImportFileType enum — supported upload formats. */
export type ImportFileType = 'CSV' | 'XLSX';

/** Mirrors the ImportJobStatus enum — import job lifecycle states. */
export type ImportJobStatus =
  | 'UPLOADED'
  | 'VALIDATING'
  | 'IMPORTING'
  | 'COMPLETED'
  | 'PARTIAL_SUCCESS'
  | 'FAILED';

/** Mirrors ImportJobSummaryResponse — condensed row for GET /import/jobs. */
export interface ImportJobSummary {
  id:             string;
  fileName:       string;
  fileType:       ImportFileType;
  importType:     ImportType;
  status:         ImportJobStatus;
  totalRows:      number;
  successfulRows: number;
  failedRows:     number;
  createdAt:      string;
}

/** Mirrors ImportJobResponse — full detail from GET /import/jobs/{id}. */
export interface ImportJobDetail {
  id:             string;
  fileName:       string;
  fileType:       ImportFileType;
  importType:     ImportType;
  status:         ImportJobStatus;
  totalRows:      number;
  successfulRows: number;
  failedRows:     number;
  startedAt:      string | null;
  completedAt:    string | null;
  createdAt:      string;
  createdByEmail: string | null;
}

/** Mirrors ImportErrorResponse — one row-level import error. */
export interface ImportError {
  id:           string;
  rowNumber:    number;
  fieldName:    string | null;
  rawValue:     string | null;
  errorCode:    string;
  errorMessage: string;
}

/** Query params accepted by GET /import/jobs. */
export interface ImportJobSearchParams {
  importType?: ImportType;
  status?:     ImportJobStatus;
  page?:       number;
  size?:       number;
}

/** Normalised paginated response returned by the import_job_search tool. */
export interface ImportJobSearchResponse {
  totalElements: number;
  totalPages:    number;
  currentPage:   number;
  pageSize:      number;
  jobs:          ImportJobSummary[];
}

/** Query params accepted by GET /import/jobs/{id}/errors. */
export interface ImportErrorSearchParams {
  jobId: string;
  page?: number;
  size?: number;
}

/** Normalised paginated response returned by the import_job_errors tool. */
export interface ImportErrorSearchResponse {
  jobId:         string;
  totalElements: number;
  totalPages:    number;
  currentPage:   number;
  pageSize:      number;
  errors:        ImportError[];
}

// ── Export domain types (Sprint 11C) ────────────────────────────────────────
// Describe the Sprint 11A binary export endpoints (GET /api/v1/export/**).
// These endpoints return XLSX / PDF files — NOT the ApiResponse envelope — so
// these types describe MCP *metadata* and *request-preview* payloads only.
// The MCP server NEVER downloads or relays the generated binary (see the
// export.tool.ts header for the architecture decision).
//
// Source of truth: backend ExportController.java + frontend src/features/export/.

/** Output file format — backend accepts `format=xlsx|pdf` (case-insensitive). */
export type ExportFormat = 'XLSX' | 'PDF';

/** The eight exportable reports. Do NOT confuse with the import ImportType. */
export type ExportReportType =
  | 'PRODUCTS'
  | 'CUSTOMERS'
  | 'ORDERS'
  | 'REVENUE'
  | 'ORDER_ANALYTICS'
  | 'TOP_PRODUCTS'
  | 'CUSTOMER_ANALYTICS'
  | 'PAYMENT_ANALYTICS';

/** Static metadata for one export report (export_report_info output). */
export interface ExportReportInfo {
  reportType:       ExportReportType;
  formats:          ExportFormat[];
  description:      string;
  supportedFilters: string[];
  endpoint:         string;
  source:           'static-metadata';
}

/** GET /export/products query params. */
export interface ProductExportParams {
  search?:     string;
  categoryId?: string;   // UUID
  active?:     boolean;
  priceMin?:   number;   // >= 0
  priceMax?:   number;   // >= 0, and >= priceMin
}

/** GET /export/customers query params. */
export interface CustomerExportParams {
  keyword?:   string;
  status?:    CustomerStatus;
  groupId?:   string;   // UUID
  startDate?: string;   // ISO 8601 datetime
  endDate?:   string;   // ISO 8601 datetime
}

/** GET /export/orders query params. */
export interface OrderExportParams {
  keyword?:       string;
  customerId?:    string;   // UUID
  status?:        OrderStatus;
  paymentStatus?: PaymentStatus;
  dateFrom?:      string;   // ISO 8601 datetime
  dateTo?:        string;   // ISO 8601 datetime
}

/** Shared date-range params for the analytics exports. */
export interface DateRangeExportParams {
  dateFrom?: string;   // ISO 8601 datetime
  dateTo?:   string;   // ISO 8601 datetime
}

/** GET /export/analytics/revenue query params. */
export interface RevenueExportParams extends DateRangeExportParams {
  groupBy?: RevenueGroupBy;   // DAY | WEEK | MONTH, backend default DAY
}

/** GET /export/analytics/products query params. */
export interface TopProductsExportParams extends DateRangeExportParams {
  limit?: number;   // integer 1..100, backend default 10
}

/** order / customer / payment analytics exports take only a date range. */
export type AnalyticsExportParams = DateRangeExportParams;

/** Any one report's filter object. */
export type ExportFilters =
  | ProductExportParams
  | CustomerExportParams
  | OrderExportParams
  | RevenueExportParams
  | TopProductsExportParams
  | AnalyticsExportParams;

/** export_request_preview output — the request that WOULD be sent, no file made. */
export interface ExportRequestPreview {
  reportType:  ExportReportType;
  format:      ExportFormat;
  filters:     Record<string, unknown>;
  queryParams: Record<string, unknown>;
  endpoint:    string;
  ready:       true;
  source:      'validation-only';
  note:        string;
}

