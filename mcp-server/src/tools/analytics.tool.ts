/**
 * MCP Tool: analytics
 *
 * Analytics domain tools for the Commerce Insight AI MCP Server.
 *
 * Registered tools (Sprint 9C):
 *   1. get_dashboard_overview  → GET /api/v1/analytics/overview
 *   2. get_revenue_analytics   → GET /api/v1/analytics/revenue
 *   3. get_order_analytics     → GET /api/v1/analytics/orders
 *   4. get_top_products        → GET /api/v1/analytics/products/top
 *   5. get_customer_analytics  → GET /api/v1/analytics/customers
 *   6. get_payment_analytics   → GET /api/v1/analytics/payments
 *
 * ARCHITECTURE RULES (enforced at every level):
 *   - Communicates ONLY via apiClient → Spring Boot REST API
 *   - No direct database access (no SQL, Prisma, TypeORM, Drizzle, pg)
 *   - No LLM calls (no OpenAI, Claude, Gemini, Ollama, LangChain, RAG)
 *   - No analytics aggregation inside MCP — backend owns all computation
 *   - No unbounded data fetching or pagination loops
 *   - Outputs never expose stack traces, SQL, passwords, JWTs, or secrets
 *   - All six tools are READ-ONLY — no state changes
 *
 * Backend endpoint base: /api/v1/analytics
 *
 * Date handling:
 *   Backend accepts ISO 8601 datetime strings (e.g. "2026-08-01T00:00:00Z").
 *   Both dateFrom and dateTo are optional; omitting both returns all-time data.
 *   When both are provided, dateFrom must not be after dateTo.
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

/**
 * Optional ISO 8601 datetime string (with optional timezone offset).
 * Matches the Spring Boot @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME).
 * Examples: "2026-08-01T00:00:00Z", "2026-08-31T23:59:59+07:00"
 */
const optionalDateTime = z
  .string()
  .datetime({ offset: true })
  .optional()
  .describe(
    'ISO 8601 datetime string with timezone (e.g. "2026-08-01T00:00:00Z"). ' +
    'Omit to include all records without a lower/upper bound.'
  );

/**
 * Revenue grouping granularity — mirrors the Spring Boot RevenueGroupBy enum.
 * Default: DAY (backend default behaviour).
 */
const revenueGroupBy = z
  .enum(['DAY', 'WEEK', 'MONTH'])
  .optional()
  .default('DAY')
  .describe(
    'Time granularity for grouping revenue data. ' +
    'DAY returns one data point per calendar day. ' +
    'WEEK returns one data point per ISO week (format: "YYYY-Www"). ' +
    'MONTH returns one data point per calendar month (format: "YYYY-MM"). ' +
    'Default: DAY.'
  );

// ── Helper: build analytics query params, omitting undefined values ───────────

/**
 * Constructs a params object for analytics API calls.
 * Undefined fields are excluded so Axios never sends them as the literal
 * string "undefined" in the query string.
 *
 * @param dateFrom - Optional ISO 8601 datetime (lower bound, inclusive)
 * @param dateTo   - Optional ISO 8601 datetime (upper bound, inclusive)
 * @param extra    - Additional optional key-value pairs (e.g. groupBy, limit)
 */
function buildAnalyticsParams(
  dateFrom: string | undefined,
  dateTo:   string | undefined,
  extra?:   Record<string, unknown>
): Record<string, unknown> {
  const params: Record<string, unknown> = {};

  if (dateFrom !== undefined) params['dateFrom'] = dateFrom;
  if (dateTo   !== undefined) params['dateTo']   = dateTo;

  if (extra) {
    for (const [key, value] of Object.entries(extra)) {
      if (value !== undefined) params[key] = value;
    }
  }

  return params;
}

/**
 * Validates that dateFrom is not after dateTo.
 * Returns an MCP tool error response if the range is invalid; null otherwise.
 */
function validateDateRange(
  toolName: string,
  dateFrom: string | undefined,
  dateTo:   string | undefined
): { content: { type: 'text'; text: string }[]; isError: true } | null {
  if (dateFrom && dateTo && new Date(dateFrom) > new Date(dateTo)) {
    return {
      content: [{
        type: 'text',
        text: `Error [${toolName}]: dateFrom must not be after dateTo. ` +
              `Received dateFrom="${dateFrom}", dateTo="${dateTo}".`,
      }],
      isError: true,
    };
  }
  return null;
}

// ── AnalyticsToolsProvider ────────────────────────────────────────────────────

export class AnalyticsToolsProvider implements McpProvider {
  public register(server: McpServer, apiClient: AxiosInstance): void {

    // ──────────────────────────────────────────────────────────────────────────
    // 1. get_dashboard_overview
    //    High-level ecommerce KPI snapshot for a time window.
    //    READ-ONLY — does not create, update, or delete any data.
    //    Backend DTO: OverviewResponse
    //      totalRevenue, totalOrders, totalCustomers, totalProductsSold,
    //      averageOrderValue, cancelledOrders, cancellationRate, currency,
    //      dateFrom, dateTo.
    //    Backend: GET /api/v1/analytics/overview
    // ──────────────────────────────────────────────────────────────────────────
    server.tool(
      'get_dashboard_overview',
      'Retrieve high-level ecommerce KPI metrics for a specified date range. ' +
      'Returns total revenue (excluding CANCELLED and REFUNDED orders), total order count, ' +
      'unique customers who placed orders, total units sold, average order value, ' +
      'number of cancelled orders, and the cancellation rate as a percentage. ' +
      'All monetary values are in the system currency (VND by default). ' +
      'Omit dateFrom and dateTo to retrieve all-time metrics. ' +
      'This is a read-only analytics operation — it does not modify any data.',
      {
        dateFrom: optionalDateTime,
        dateTo:   optionalDateTime,
      },
      async ({ dateFrom, dateTo }) => {
        const rangeError = validateDateRange('get_dashboard_overview', dateFrom, dateTo);
        if (rangeError) return rangeError;

        try {
          const params   = buildAnalyticsParams(dateFrom, dateTo);
          const response = await apiClient.get('/analytics/overview', { params });
          const overview = response.data?.data;

          if (!overview) {
            return {
              content: [{ type: 'text', text: 'No overview data returned from the analytics backend.' }],
              isError: true,
            };
          }

          // Return structure mirrors OverviewResponse exactly; period block added for LLM context
          const result = {
            period: {
              dateFrom: overview.dateFrom ?? null,
              dateTo:   overview.dateTo   ?? null,
            },
            currency:          overview.currency,
            totalRevenue:      overview.totalRevenue,
            totalOrders:       overview.totalOrders,
            totalCustomers:    overview.totalCustomers,
            totalProductsSold: overview.totalProductsSold,
            averageOrderValue: overview.averageOrderValue,
            cancelledOrders:   overview.cancelledOrders,
            cancellationRate:  overview.cancellationRate,
          };

          return {
            content: [{ type: 'text', text: JSON.stringify(result, null, 2) }],
          };
        } catch (error) {
          return toMcpToolError('get_dashboard_overview', error);
        }
      }
    );

    // ──────────────────────────────────────────────────────────────────────────
    // 2. get_revenue_analytics
    //    Revenue time series grouped by DAY, WEEK, or MONTH.
    //    READ-ONLY — no aggregation performed by MCP.
    //    Backend DTO: RevenueResponse { groupBy, currency, dateFrom, dateTo, data[] }
    //    Each data point: RevenuePeriodResponse { period, revenue, orders }
    //    Backend: GET /api/v1/analytics/revenue
    // ──────────────────────────────────────────────────────────────────────────
    server.tool(
      'get_revenue_analytics',
      'Retrieve revenue trend analytics for a specified period, grouped by day, week, or month. ' +
      'Returns a time series where each data point contains the period label, total revenue, ' +
      'and total order count for that period. Only revenue-eligible orders ' +
      '(CONFIRMED, PROCESSING, SHIPPED, DELIVERED, COMPLETED) are included; ' +
      'PENDING, CANCELLED, and REFUNDED orders are excluded from revenue figures. ' +
      'Use this when the user asks about revenue trends, sales over time, or period-over-period ' +
      'revenue comparisons. groupBy=DAY gives fine-grained daily data; ' +
      'groupBy=MONTH is better for long date ranges. ' +
      'This is a read-only analytics operation.',
      {
        dateFrom: optionalDateTime,
        dateTo:   optionalDateTime,
        groupBy:  revenueGroupBy,
      },
      async ({ dateFrom, dateTo, groupBy }) => {
        const rangeError = validateDateRange('get_revenue_analytics', dateFrom, dateTo);
        if (rangeError) return rangeError;

        try {
          const params   = buildAnalyticsParams(dateFrom, dateTo, { groupBy });
          const response = await apiClient.get('/analytics/revenue', { params });
          const revenue  = response.data?.data;

          if (!revenue) {
            return {
              content: [{ type: 'text', text: 'No revenue data returned from the analytics backend.' }],
              isError: true,
            };
          }

          // Backend returns RevenueResponse directly — return it as-is
          return {
            content: [{ type: 'text', text: JSON.stringify(revenue, null, 2) }],
          };
        } catch (error) {
          return toMcpToolError('get_revenue_analytics', error);
        }
      }
    );

    // ──────────────────────────────────────────────────────────────────────────
    // 3. get_order_analytics
    //    Order status distribution with completion and cancellation rates.
    //    READ-ONLY — all metrics computed server-side.
    //    Backend DTO: OrderAnalyticsResponse
    //      totalOrders, pendingOrders, confirmedOrders, processingOrders,
    //      shippedOrders, deliveredOrders, completedOrders, cancelledOrders,
    //      completionRate, cancellationRate, dateFrom, dateTo.
    //    Backend: GET /api/v1/analytics/orders
    // ──────────────────────────────────────────────────────────────────────────
    server.tool(
      'get_order_analytics',
      'Retrieve order analytics for a specified period, including a breakdown of order counts ' +
      'by every lifecycle status (PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, COMPLETED, CANCELLED), ' +
      'the completion rate (percentage of COMPLETED orders out of total), ' +
      'and the cancellation rate (percentage of CANCELLED orders out of total). ' +
      'Use this when the user asks about order volumes, order status distribution, ' +
      'fulfilment performance, or cancellation trends. ' +
      'This is a read-only analytics operation — it does not modify any order data.',
      {
        dateFrom: optionalDateTime,
        dateTo:   optionalDateTime,
      },
      async ({ dateFrom, dateTo }) => {
        const rangeError = validateDateRange('get_order_analytics', dateFrom, dateTo);
        if (rangeError) return rangeError;

        try {
          const params   = buildAnalyticsParams(dateFrom, dateTo);
          const response = await apiClient.get('/analytics/orders', { params });
          const orders   = response.data?.data;

          if (!orders) {
            return {
              content: [{ type: 'text', text: 'No order analytics data returned from the backend.' }],
              isError: true,
            };
          }

          return {
            content: [{ type: 'text', text: JSON.stringify(orders, null, 2) }],
          };
        } catch (error) {
          return toMcpToolError('get_order_analytics', error);
        }
      }
    );

    // ──────────────────────────────────────────────────────────────────────────
    // 4. get_top_products
    //    Top N products ranked by revenue within a time window.
    //    READ-ONLY — ranking done by backend, never by MCP.
    //    Backend DTO: TopProductEntry[]
    //      productId (nullable), sku, productName, quantitySold, revenue
    //    limit enforced: min 1, max 100 to prevent unbounded responses.
    //    Backend: GET /api/v1/analytics/products/top
    // ──────────────────────────────────────────────────────────────────────────
    server.tool(
      'get_top_products',
      'Retrieve the top-selling products ranked by total revenue for a specified period. ' +
      'Each entry contains: product ID (may be null if the product was deleted), ' +
      'SKU (snapshot from order time, accurate even for deleted products), ' +
      'product name (snapshot), total quantity sold, and total revenue generated. ' +
      'The backend performs all ranking and aggregation — MCP never sorts locally. ' +
      'Use this when the user asks which products sell most, which generate the highest revenue, ' +
      'or wants a product performance leaderboard. ' +
      'The limit parameter controls how many results to return (1 to 100, default 10). ' +
      'This is a read-only analytics operation.',
      {
        dateFrom: optionalDateTime,
        dateTo:   optionalDateTime,
        limit: z
          .number()
          .int('limit must be a whole number')
          .min(1,   'limit must be at least 1')
          .max(100, 'limit must not exceed 100')
          .optional()
          .default(10)
          .describe('Number of top products to return. Minimum 1, maximum 100. Default: 10.'),
      },
      async ({ dateFrom, dateTo, limit }) => {
        const rangeError = validateDateRange('get_top_products', dateFrom, dateTo);
        if (rangeError) return rangeError;

        try {
          const params   = buildAnalyticsParams(dateFrom, dateTo, { limit });
          const response = await apiClient.get('/analytics/products/top', { params });
          const products = response.data?.data;

          if (!products) {
            return {
              content: [{ type: 'text', text: 'No top-product data returned from the backend.' }],
              isError: true,
            };
          }

          const result = {
            period:   { dateFrom: dateFrom ?? null, dateTo: dateTo ?? null },
            limit,
            count:    Array.isArray(products) ? products.length : 0,
            products: Array.isArray(products) ? products : [],
          };

          return {
            content: [{ type: 'text', text: JSON.stringify(result, null, 2) }],
          };
        } catch (error) {
          return toMcpToolError('get_top_products', error);
        }
      }
    );

    // ──────────────────────────────────────────────────────────────────────────
    // 5. get_customer_analytics
    //    Customer engagement metrics for a time window.
    //    READ-ONLY — no RFM, no AI segmentation, no local computation.
    //    Backend DTO: CustomerAnalyticsResponse
    //      uniqueCustomers, newCustomers, repeatCustomers,
    //      averageOrdersPerCustomer, dateFrom, dateTo.
    //    Backend: GET /api/v1/analytics/customers
    // ──────────────────────────────────────────────────────────────────────────
    server.tool(
      'get_customer_analytics',
      'Retrieve customer engagement analytics for a specified period. ' +
      'Returns: unique customers (those who placed at least one order in the period), ' +
      'new customers (first-ever order falls within the period), ' +
      'repeat customers (placed more than one order in the period), ' +
      'and average orders per unique customer. ' +
      'Use this when the user asks about customer activity, customer acquisition, ' +
      'new vs. returning customer ratios, or customer ordering frequency. ' +
      'This is a read-only analytics operation — no RFM analysis, no AI, no segmentation.',
      {
        dateFrom: optionalDateTime,
        dateTo:   optionalDateTime,
      },
      async ({ dateFrom, dateTo }) => {
        const rangeError = validateDateRange('get_customer_analytics', dateFrom, dateTo);
        if (rangeError) return rangeError;

        try {
          const params    = buildAnalyticsParams(dateFrom, dateTo);
          const response  = await apiClient.get('/analytics/customers', { params });
          const customers = response.data?.data;

          if (!customers) {
            return {
              content: [{ type: 'text', text: 'No customer analytics data returned from the backend.' }],
              isError: true,
            };
          }

          return {
            content: [{ type: 'text', text: JSON.stringify(customers, null, 2) }],
          };
        } catch (error) {
          return toMcpToolError('get_customer_analytics', error);
        }
      }
    );

    // ──────────────────────────────────────────────────────────────────────────
    // 6. get_payment_analytics
    //    Payment method distribution — order count and amount per method.
    //    READ-ONLY — backend groups all data; MCP returns it as-is.
    //    Backend DTO: PaymentAnalyticsResponse
    //      currency, breakdown: Record<string, PaymentMethodStats>,
    //      dateFrom, dateTo.
    //    PaymentMethodStats: { orders: number, amount: number }
    //    Keys: "CASH" | "BANK_TRANSFER" | "CARD" | "OTHER"
    //    Methods with no activity in the period are omitted from breakdown.
    //    Backend: GET /api/v1/analytics/payments
    // ──────────────────────────────────────────────────────────────────────────
    server.tool(
      'get_payment_analytics',
      'Retrieve payment method analytics for a specified period. ' +
      'Returns a breakdown by payment method (CASH, BANK_TRANSFER, CARD, OTHER) ' +
      'showing the number of orders and total amount collected for each method. ' +
      'Payment methods with no activity in the period are not included in the response. ' +
      'Use this when the user asks how customers are paying, which payment method is most popular, ' +
      'or wants a revenue breakdown by payment type. ' +
      'This is a read-only analytics operation — it does not modify any payment data.',
      {
        dateFrom: optionalDateTime,
        dateTo:   optionalDateTime,
      },
      async ({ dateFrom, dateTo }) => {
        const rangeError = validateDateRange('get_payment_analytics', dateFrom, dateTo);
        if (rangeError) return rangeError;

        try {
          const params   = buildAnalyticsParams(dateFrom, dateTo);
          const response = await apiClient.get('/analytics/payments', { params });
          const payments = response.data?.data;

          if (!payments) {
            return {
              content: [{ type: 'text', text: 'No payment analytics data returned from the backend.' }],
              isError: true,
            };
          }

          return {
            content: [{ type: 'text', text: JSON.stringify(payments, null, 2) }],
          };
        } catch (error) {
          return toMcpToolError('get_payment_analytics', error);
        }
      }
    );

  }
}
