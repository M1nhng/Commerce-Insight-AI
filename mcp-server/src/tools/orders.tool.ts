/**
 * MCP Tool: orders
 *
 * Order domain tools for the Commerce Insight AI MCP Server.
 *
 * Registered tools (Sprint 8C final):
 *   1. order_lookup          → GET  /api/v1/orders/{id}           fast ID-based summary
 *   2. order_search          → GET  /api/v1/orders                filtered + paginated list
 *   3. customer_orders       → GET  /api/v1/orders?customerId=… paginated per-customer history
 *   4. order_summary         → GET  /api/v1/orders/{id}           full detail (items/addresses/payment/history)
 *   5. order_status_history  → GET  /api/v1/orders/{id}           extracts statusHistory only
 *   6. order_create          → POST /api/v1/orders                create a new order (state-changing)
 *
 * ARCHITECTURE RULES (enforced at every level):
 *   - Communicates ONLY via apiClient → Spring Boot REST API
 *   - No direct database access (no SQL, Prisma, TypeORM, Drizzle, pg)
 *   - No LLM calls (no OpenAI, Claude, Gemini, Ollama, LangChain, RAG)
 *   - No analytics aggregation or unbounded data-fetching loops
 *   - Outputs never expose stack traces, SQL, passwords, JWTs, or internal secrets
 *
 * NOTE on order_summary vs analytics:
 *   The backend provides GET /api/v1/orders/{id} which returns a full OrderResponse
 *   (items, addresses, payment, statusHistory). There is no separate aggregation/analytics
 *   summary endpoint. order_summary correctly uses this endpoint to return the full detail.
 *   Aggregate analytics (total orders, revenue) are the responsibility of the analytics
 *   domain (see analytics.tool.ts), not the order tools.
 *
 * Output contract:  { content: [{ type: 'text', text: JSON.stringify(data, null, 2) }] }
 * Error contract:   { content: [{ type: 'text', text: '<human-readable message>' }], isError: true }
 */

import { z } from 'zod';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { AxiosInstance } from 'axios';
import { McpProvider } from '../providers/provider.interface.js';
import { toMcpToolError } from '../utils/error.handler.js';

// ── Shared Zod sub-schemas ────────────────────────────────────────────────────

/** ISO 8601 date string (YYYY-MM-DD). */
const isoDate = z
  .string()
  .regex(/^\d{4}-\d{2}-\d{2}$/, 'Date must be in YYYY-MM-DD format')
  .describe('ISO 8601 date string in YYYY-MM-DD format (e.g. 2026-01-31)');

/** OrderStatus enum — mirrors the Spring Boot OrderStatus enum exactly. */
const orderStatus = z.enum([
  'PENDING',
  'CONFIRMED',
  'PROCESSING',
  'SHIPPED',
  'DELIVERED',
  'COMPLETED',
  'CANCELLED',
  'REFUNDED',
]).describe(
  'Order lifecycle status. Valid values: PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, COMPLETED, CANCELLED, REFUNDED'
);

/** PaymentStatus enum — mirrors the Spring Boot PaymentStatus enum exactly. */
const paymentStatus = z.enum(['PENDING', 'PAID', 'FAILED', 'REFUNDED']).describe(
  'Payment lifecycle status. Valid values: PENDING, PAID, FAILED, REFUNDED'
);

/** PaymentMethod enum — mirrors the Spring Boot PaymentMethod enum exactly. */
const paymentMethod = z.enum(['CASH', 'BANK_TRANSFER', 'CARD', 'OTHER']).describe(
  'Payment method. Valid values: CASH, BANK_TRANSFER, CARD, OTHER'
);

/** AddressType enum. */
const addressType = z.enum(['SHIPPING', 'BILLING']).describe(
  'Address type: SHIPPING or BILLING'
);

// ── Response projector helpers ────────────────────────────────────────────────

/**
 * Projects OrderSummaryResponse fields.
 * Mirrors OrderSummaryResponse: id, orderNumber, customerId, customerName,
 * status, paymentStatus, total, currency, itemCount, createdAt, updatedAt.
 */
function projectSummary(o: any) {
  return {
    id:            o.id,
    orderNumber:   o.orderNumber,
    customerId:    o.customerId    ?? null,
    customerName:  o.customerName,
    status:        o.status,
    paymentStatus: o.paymentStatus,
    total:         o.total,
    currency:      o.currency,
    itemCount:     o.itemCount,
    createdAt:     o.createdAt,
    updatedAt:     o.updatedAt,
  };
}

/**
 * Projects OrderItemResponse fields.
 * Mirrors OrderItemResponse: id, productId, skuSnapshot, productNameSnapshot,
 * unitPrice, quantity, discountAmount, subtotal.
 */
function projectItem(item: any) {
  return {
    id:                  item.id,
    productId:           item.productId           ?? null,
    skuSnapshot:         item.skuSnapshot,
    productNameSnapshot: item.productNameSnapshot,
    unitPrice:           item.unitPrice,
    quantity:            item.quantity,
    discountAmount:      item.discountAmount,
    subtotal:            item.subtotal,
  };
}

/**
 * Projects OrderAddressResponse fields.
 * Mirrors OrderAddressResponse: id, type, recipientName, phone,
 * addressLine, ward, district, province, country, createdAt.
 */
function projectAddress(addr: any) {
  return {
    id:            addr.id,
    type:          addr.type,
    recipientName: addr.recipientName,
    phone:         addr.phone     ?? null,
    addressLine:   addr.addressLine,
    ward:          addr.ward      ?? null,
    district:      addr.district  ?? null,
    province:      addr.province  ?? null,
    country:       addr.country,
    createdAt:     addr.createdAt,
  };
}

/**
 * Projects OrderStatusHistoryResponse fields.
 * Mirrors: id, fromStatus, toStatus, changedById, changedByName, reason, createdAt.
 */
function projectHistoryEntry(entry: any) {
  return {
    fromStatus:    entry.fromStatus    ?? null,
    toStatus:      entry.toStatus,
    changedByName: entry.changedByName ?? null,
    reason:        entry.reason        ?? null,
    createdAt:     entry.createdAt,
  };
}

/**
 * Projects PaymentResponse fields.
 */
function projectPayment(payment: any) {
  return {
    id:        payment.id,
    method:    payment.method,
    status:    payment.status,
    amount:    payment.amount,
    currency:  payment.currency,
    reference: payment.reference ?? null,
    paidAt:    payment.paidAt    ?? null,
    notes:     payment.notes     ?? null,
  };
}

// ── Address sub-schema (reused in order_create) ───────────────────────────────

const addressSchema = z.object({
  type:          addressType,
  recipientName: z.string().min(1).max(255).describe('Name of the recipient at this address'),
  phone:         z.string().max(50).optional().describe('Contact phone for delivery'),
  addressLine:   z.string().min(1).max(500).describe('Full street address line'),
  ward:          z.string().max(100).optional().describe('Phường/Xã'),
  district:      z.string().max(100).optional().describe('Quận/Huyện'),
  province:      z.string().max(100).optional().describe('Tỉnh/Thành phố'),
  country:       z.string().max(100).optional().describe('Country (default: Vietnam)'),
});

// ── OrderToolsProvider ────────────────────────────────────────────────────────

export class OrderToolsProvider implements McpProvider {
  public register(server: McpServer, apiClient: AxiosInstance): void {

    // ──────────────────────────────────────────────────────────────────────────
    // 1. order_lookup
    //    Fast single-order summary fetch by ID.
    //    Returns lightweight fields from OrderSummaryResponse.
    //    Backend: GET /api/v1/orders/{orderId}
    // ──────────────────────────────────────────────────────────────────────────
    server.tool(
      'order_lookup',
      'Look up a specific order by its unique ID. Returns the order number, customer name, total amount, currency, item count, order lifecycle status, payment status, and timestamps. Use this when you already know the order ID and need a quick status check without all detail.',
      {
        orderId: z
          .string()
          .uuid('orderId must be a valid UUID')
          .describe('The unique UUID identifier of the order to look up'),
      },
      async ({ orderId }) => {
        try {
          const response = await apiClient.get(`/orders/${orderId}`);
          const order = response.data?.data;

          if (!order) {
            return {
              content: [{ type: 'text', text: `No order data returned for ID: ${orderId}` }],
              isError: true,
            };
          }

          return {
            content: [{ type: 'text', text: JSON.stringify(projectSummary(order), null, 2) }],
          };
        } catch (error) {
          return toMcpToolError('order_lookup', error);
        }
      }
    );

    // ──────────────────────────────────────────────────────────────────────────
    // 2. order_search
    //    Paginated, filtered search across all orders.
    //    Supports: keyword (by order number), customerId, status, paymentStatus,
    //    date range (dateFrom/dateTo in YYYY-MM-DD format), pagination.
    //    Backend: GET /api/v1/orders
    // ──────────────────────────────────────────────────────────────────────────
    server.tool(
      'order_search',
      'Search and filter the complete order list with optional criteria. Supports free-text search by order number (keyword), filtering by customer ID, order status, payment status, and a creation date range in YYYY-MM-DD format. Returns a paginated list of order summaries sorted by creation date descending.',
      {
        keyword: z
          .string()
          .optional()
          .describe('Free-text search matched against order number'),

        customerId: z
          .string()
          .uuid('customerId must be a valid UUID if provided')
          .optional()
          .describe('Filter orders belonging to a specific customer UUID'),

        status: orderStatus
          .optional()
          .describe('Filter by order lifecycle status'),

        paymentStatus: paymentStatus
          .optional()
          .describe('Filter by payment status'),

        dateFrom: isoDate
          .optional()
          .describe('Return orders created on or after this date (YYYY-MM-DD, inclusive)'),

        dateTo: isoDate
          .optional()
          .describe('Return orders created on or before this date (YYYY-MM-DD, inclusive)'),

        page: z
          .number().int().min(0)
          .optional().default(0)
          .describe('Page number, 0-indexed (default: 0)'),

        size: z
          .number().int().min(1).max(100)
          .optional().default(10)
          .describe('Number of orders per page (default: 10, max: 100)'),
      },
      async ({ keyword, customerId, status, paymentStatus: pmtStatus, dateFrom, dateTo, page, size }) => {
        try {
          const params: Record<string, unknown> = { page, size, sort: 'createdAt,desc' };

          if (keyword)    params['keyword']       = keyword;
          if (customerId) params['customerId']     = customerId;
          if (status)     params['status']         = status;
          if (pmtStatus)  params['paymentStatus']  = pmtStatus;
          if (dateFrom)   params['dateFrom']        = dateFrom;
          if (dateTo)     params['dateTo']          = dateTo;

          const response = await apiClient.get('/orders', { params });
          const pageData = response.data?.data;

          if (!pageData) {
            return {
              content: [{ type: 'text', text: 'No order data returned from the backend.' }],
              isError: true,
            };
          }

          const result = {
            totalElements: pageData.totalElements,
            totalPages:    pageData.totalPages,
            currentPage:   pageData.number ?? page,
            pageSize:      pageData.size   ?? size,
            orders:        (pageData.content ?? []).map(projectSummary),
          };

          return {
            content: [{ type: 'text', text: JSON.stringify(result, null, 2) }],
          };
        } catch (error) {
          return toMcpToolError('order_search', error);
        }
      }
    );

    // ──────────────────────────────────────────────────────────────────────────
    // 3. customer_orders
    //    Paginated order history for a specific customer.
    //    Optionally filter by status.
    //    Backend: GET /api/v1/orders?customerId={id}
    // ──────────────────────────────────────────────────────────────────────────
    server.tool(
      'customer_orders',
      'Retrieve the paginated order history for a specific customer by their unique customer ID. Optionally filter by order status. Returns order summaries sorted from most recent to oldest. Use this when you need to see all orders placed by a particular customer.',
      {
        customerId: z
          .string()
          .uuid('customerId must be a valid UUID')
          .describe('The unique UUID identifier of the customer whose orders to retrieve'),

        status: orderStatus
          .optional()
          .describe('Optional filter: return only orders with this status'),

        page: z
          .number().int().min(0)
          .optional().default(0)
          .describe('Page number, 0-indexed (default: 0)'),

        size: z
          .number().int().min(1).max(100)
          .optional().default(10)
          .describe('Number of orders per page (default: 10, max: 100)'),
      },
      async ({ customerId, status, page, size }) => {
        try {
          const params: Record<string, unknown> = {
            customerId,
            page,
            size,
            sort: 'createdAt,desc',
          };

          if (status) params['status'] = status;

          const response = await apiClient.get('/orders', { params });
          const pageData = response.data?.data;

          if (!pageData) {
            return {
              content: [{ type: 'text', text: `No order history returned for customer ID: ${customerId}` }],
              isError: true,
            };
          }

          const result = {
            customerId,
            totalOrders: pageData.totalElements,
            totalPages:  pageData.totalPages,
            currentPage: pageData.number ?? page,
            pageSize:    pageData.size   ?? size,
            orders:      (pageData.content ?? []).map(projectSummary),
          };

          return {
            content: [{ type: 'text', text: JSON.stringify(result, null, 2) }],
          };
        } catch (error) {
          return toMcpToolError('customer_orders', error);
        }
      }
    );

    // ──────────────────────────────────────────────────────────────────────────
    // 4. order_summary
    //    Full order detail using GET /api/v1/orders/{id} (OrderResponse).
    //    Includes: all line items, shipping/billing address snapshots,
    //    payment record, and the complete status history timeline.
    //
    //    NOTE: The backend provides no separate analytics/aggregation endpoint
    //    for orders. This tool correctly uses the full detail endpoint which
    //    returns all domain information the LLM needs to describe an order.
    //    Order-level analytics (total revenue, order count) live in the
    //    analytics domain (get_order_analytics).
    //
    //    Backend: GET /api/v1/orders/{orderId}
    // ──────────────────────────────────────────────────────────────────────────
    server.tool(
      'order_summary',
      'Retrieve the complete detail of a specific order by ID. Returns all fields: every line item (product name/SKU snapshot, unit price, quantity, discount, subtotal), shipping and billing address snapshots, the payment record (method, status, amount, paid-at), and the full status history timeline. Use this when the user needs the complete order record, not just a summary.',
      {
        orderId: z
          .string()
          .uuid('orderId must be a valid UUID')
          .describe('The unique UUID identifier of the order to retrieve in full detail'),
      },
      async ({ orderId }) => {
        try {
          const response = await apiClient.get(`/orders/${orderId}`);
          const order = response.data?.data;

          if (!order) {
            return {
              content: [{ type: 'text', text: `No order found for ID: ${orderId}` }],
              isError: true,
            };
          }

          const result = {
            id:           order.id,
            orderNumber:  order.orderNumber,
            customerId:   order.customerId   ?? null,
            customerName: order.customerName,
            customerCode: order.customerCode ?? null,

            // Status & payment
            status:        order.status,
            paymentStatus: order.paymentStatus,

            // Financials — calculated server-side, read-only
            subtotal:    order.subtotal,
            discount:    order.discount,
            shippingFee: order.shippingFee,
            tax:         order.tax,
            total:       order.total,
            currency:    order.currency,

            // Notes & timestamps
            notes:       order.notes       ?? null,
            createdAt:   order.createdAt,
            updatedAt:   order.updatedAt,
            shippedAt:   order.shippedAt   ?? null,
            deliveredAt: order.deliveredAt ?? null,
            cancelledAt: order.cancelledAt ?? null,
            completedAt: order.completedAt ?? null,

            // Line items
            items: (order.items ?? []).map(projectItem),

            // Address snapshots
            shippingAddress: order.shippingAddress ? projectAddress(order.shippingAddress) : null,
            billingAddress:  order.billingAddress  ? projectAddress(order.billingAddress)  : null,

            // Payment record
            payment: order.payment ? projectPayment(order.payment) : null,

            // Status audit trail (chronological)
            statusHistory: (order.statusHistory ?? []).map(projectHistoryEntry),
          };

          return {
            content: [{ type: 'text', text: JSON.stringify(result, null, 2) }],
          };
        } catch (error) {
          return toMcpToolError('order_summary', error);
        }
      }
    );

    // ──────────────────────────────────────────────────────────────────────────
    // 5. order_status_history
    //    Extracts only the status audit trail from an order.
    //    Returns: orderId, orderNumber, currentStatus, history[].
    //    The MCP server does NOT modify status — read-only.
    //    Backend: GET /api/v1/orders/{orderId}  (extracts statusHistory)
    // ──────────────────────────────────────────────────────────────────────────
    server.tool(
      'order_status_history',
      'Retrieve the complete status transition history for a specific order. Returns the order number, current status, and a chronological list of every status change including: previous status, new status, who made the change, an optional reason, and the timestamp. Use this when the user asks how an order progressed or why it was cancelled.',
      {
        orderId: z
          .string()
          .uuid('orderId must be a valid UUID')
          .describe('The unique UUID identifier of the order whose status history to retrieve'),
      },
      async ({ orderId }) => {
        try {
          const response = await apiClient.get(`/orders/${orderId}`);
          const order = response.data?.data;

          if (!order) {
            return {
              content: [{ type: 'text', text: `No order found for ID: ${orderId}` }],
              isError: true,
            };
          }

          const result = {
            orderId:       order.id,
            orderNumber:   order.orderNumber,
            currentStatus: order.status,
            history:       (order.statusHistory ?? []).map(projectHistoryEntry),
          };

          return {
            content: [{ type: 'text', text: JSON.stringify(result, null, 2) }],
          };
        } catch (error) {
          return toMcpToolError('order_status_history', error);
        }
      }
    );

    // ──────────────────────────────────────────────────────────────────────────
    // 6. order_create
    //    Creates a new order via POST /api/v1/orders.
    //    Input mirrors CreateOrderRequest exactly (field names verified against
    //    the Spring Boot DTO).
    //
    //    The backend is responsible for ALL business logic:
    //      - customer validation (must be ACTIVE)
    //      - product validation (must exist and be active)
    //      - price calculation (unit price from product catalog)
    //      - subtotal / total calculation (never trusted from client)
    //      - inventory reservation
    //      - warehouse selection
    //      - payment record creation
    //      - transaction management
    //
    //    The MCP server does NOT duplicate these rules.
    //    Backend: POST /api/v1/orders
    //
    //    ⚠️  STATE-CHANGING OPERATION — creates a real order.
    //    The LLM MUST ask the user to confirm before executing this tool.
    // ──────────────────────────────────────────────────────────────────────────
    server.tool(
      'order_create',
      '⚠️ STATE-CHANGING OPERATION: This tool creates a real order in the system. The user must explicitly confirm the order details before this tool is executed. Creates a new customer order by sending the order payload to the backend. The backend validates the customer (must be ACTIVE), validates all products (must exist and be active), checks inventory availability, calculates all prices and totals server-side, reserves inventory, and creates the payment record. Returns the created order ID, order number, status, payment status, calculated total, and currency.',
      {
        customerId: z
          .string()
          .uuid('customerId must be a valid UUID')
          .describe('UUID of the customer placing the order — must be ACTIVE'),

        items: z
          .array(
            z.object({
              productId: z
                .string()
                .uuid('productId must be a valid UUID')
                .describe('UUID of the product to order'),
              quantity: z
                .number().int().min(1).max(10000)
                .describe('Number of units to order (1 to 10,000)'),
              discountAmount: z
                .number().min(0)
                .optional()
                .describe('Optional per-item discount amount in the order currency (defaults to 0)'),
            })
          )
          .min(1).max(100)
          .describe('List of line items. At least 1 item required, maximum 100 items.'),

        paymentMethod: paymentMethod
          .describe('Payment method: CASH, BANK_TRANSFER, CARD, or OTHER'),

        shippingAddress: addressSchema
          .extend({ type: z.literal('SHIPPING') })
          .optional()
          .describe('Optional shipping address snapshot captured at order time'),

        billingAddress: addressSchema
          .extend({ type: z.literal('BILLING') })
          .optional()
          .describe('Optional billing address snapshot captured at order time'),

        shippingFee: z
          .number().min(0)
          .optional()
          .describe('Shipping fee in order currency (optional, defaults to 0 if omitted)'),

        discount: z
          .number().min(0)
          .optional()
          .describe('Order-level discount amount in order currency (optional, defaults to 0)'),

        tax: z
          .number().min(0)
          .optional()
          .describe('Tax amount in order currency (optional, defaults to 0)'),

        currency: z
          .string().max(10)
          .optional()
          .describe('ISO 4217 currency code (optional, defaults to VND)'),

        notes: z
          .string().max(2000)
          .optional()
          .describe('Free-text notes for this order (optional, max 2000 characters)'),
      },
      async ({ customerId, items, paymentMethod: pmtMethod, shippingAddress, billingAddress, shippingFee, discount, tax, currency, notes }) => {
        try {
          // Build the request payload, omitting undefined optional fields
          // Field names mirror CreateOrderRequest exactly
          const body: Record<string, unknown> = {
            customerId,
            items: items.map((item) => ({
              productId:      item.productId,
              quantity:       item.quantity,
              discountAmount: item.discountAmount ?? null,
            })),
            paymentMethod: pmtMethod,
          };

          if (shippingAddress !== undefined) body['shippingAddress'] = shippingAddress;
          if (billingAddress  !== undefined) body['billingAddress']  = billingAddress;
          if (shippingFee     !== undefined) body['shippingFee']     = shippingFee;
          if (discount        !== undefined) body['discount']        = discount;
          if (tax             !== undefined) body['tax']             = tax;
          if (currency        !== undefined) body['currency']        = currency;
          if (notes           !== undefined) body['notes']           = notes;

          const response = await apiClient.post('/orders', body);
          const order = response.data?.data;

          if (!order) {
            return {
              content: [{ type: 'text', text: 'Order creation succeeded but no order data was returned.' }],
              isError: true,
            };
          }

          // Return only the fields relevant for confirming the creation
          const result = {
            orderId:       order.id,
            orderNumber:   order.orderNumber,
            status:        order.status,
            paymentStatus: order.paymentStatus,
            totalAmount:   order.total,
            currency:      order.currency,
          };

          return {
            content: [{ type: 'text', text: JSON.stringify(result, null, 2) }],
          };
        } catch (error) {
          return toMcpToolError('order_create', error);
        }
      }
    );
  }
}
