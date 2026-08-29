/**
 * MCP Tool: export
 *
 * Export domain tools for the Commerce Insight AI MCP Server (Sprint 11C).
 *
 * Registered tools:
 *   1. export_capabilities     → static metadata (ZERO backend calls)
 *   2. export_report_info      → static metadata (ZERO backend calls)
 *   3. export_request_preview  → validation + normalisation only (ZERO backend calls)
 *
 * ────────────────────────────────────────────────────────────────────────────
 * ARCHITECTURE DECISION — why there is NO `export_generate` tool
 * ────────────────────────────────────────────────────────────────────────────
 * The Sprint 11A endpoints (GET /api/v1/export/**) return raw XLSX / PDF bytes
 * with a `Content-Disposition: attachment` header — they do NOT use the JSON
 * ApiResponse envelope.
 *
 * The installed MCP SDK (@modelcontextprotocol/sdk 1.29.0) can only carry a
 * binary file to the client as either:
 *   - a base64 `EmbeddedResource` / large text blob  → forbidden by the sprint
 *     brief (huge responses, transport limits, memory) and not a real file
 *     abstraction; or
 *   - a `resource_link` with a resolvable URI  → would require either exposing
 *     an internal backend URL (forbidden) or standing up a new file-hosting /
 *     resource-serving mechanism on the MCP server (forbidden — no new infra).
 *
 * In addition, the shared `apiClient` response interceptor
 * (src/client/api.client.ts) returns `response.data` and discards the response
 * headers, so `Content-Disposition` is not even reachable without modifying
 * shared code used by every other provider.
 *
 * This is exactly the situation Sprint 10C already resolved for the binary
 * import-template endpoint: MCP exposes METADATA, not the binary. Sprint 11C
 * follows that precedent. Binary export execution is intentionally NOT exposed
 * through MCP. This is an accepted, documented limitation.
 *
 * ────────────────────────────────────────────────────────────────────────────
 * ARCHITECTURE RULES (enforced at every level)
 * ────────────────────────────────────────────────────────────────────────────
 *   - Communicates ONLY via the injected apiClient → Spring Boot REST API.
 *     (These three tools make ZERO backend requests — no export capabilities
 *      endpoint exists to query, and preview is pure input validation.)
 *   - NO database access (no SQL, Prisma, TypeORM, Drizzle, pg, mysql, sqlite).
 *   - NO LLM calls (no OpenAI, Anthropic, Gemini, Ollama, LangChain, RAG).
 *   - NO report generation — no ExcelJS / POI / PDFKit / jsPDF / Puppeteer,
 *     no local aggregation, no row counting, no file-size estimation.
 *   - NO loops, NO polling, NO recursion, NO pagination walking.
 *   - NO fake download URLs, NO base64 file dumps, NO internal URL exposure.
 *   - Outputs never expose stack traces, SQL, JWTs, API keys, DB credentials,
 *     or backend configuration internals.
 *   - All three tools are READ-ONLY — no state changes.
 *
 * Output contract:  { content: [{ type: 'text', text: JSON.stringify(data, null, 2) }] }
 * Error contract:    { content: [{ type: 'text', text: '<safe message>' }], isError: true }
 */

import { z } from 'zod';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { AxiosInstance } from 'axios';
import { McpProvider } from '../providers/provider.interface.js';

// ── Enums — mirror the Sprint 11A backend contract exactly ──────────────────

const EXPORT_FORMATS = ['XLSX', 'PDF'] as const;

const EXPORT_REPORT_TYPES = [
  'PRODUCTS',
  'CUSTOMERS',
  'ORDERS',
  'REVENUE',
  'ORDER_ANALYTICS',
  'TOP_PRODUCTS',
  'CUSTOMER_ANALYTICS',
  'PAYMENT_ANALYTICS',
] as const;

// CustomerStatus / OrderStatus / PaymentStatus — from the backend domain enums
// (backend/.../customer/domain/CustomerStatus.java, order/domain/*.java).
const CUSTOMER_STATUSES = ['ACTIVE', 'INACTIVE', 'BLOCKED'] as const;
const ORDER_STATUSES = [
  'PENDING',
  'CONFIRMED',
  'PROCESSING',
  'SHIPPED',
  'DELIVERED',
  'COMPLETED',
  'CANCELLED',
  'REFUNDED',
] as const;
const PAYMENT_STATUSES = ['PENDING', 'PAID', 'FAILED', 'REFUNDED'] as const;
const REVENUE_GROUP_BY = ['DAY', 'WEEK', 'MONTH'] as const;

type ExportReportType = (typeof EXPORT_REPORT_TYPES)[number];

// ── Report metadata — derived strictly from ExportController.java ───────────
//
// `endpoint` is the application API path (documentation only — the same style
// import_template_info uses). `supportedFilters` are the exact @RequestParam
// names accepted by the Sprint 11A controller for that report.

interface ReportMeta {
  endpoint: string;
  description: string;
  supportedFilters: string[];
}

const REPORT_METADATA: Record<ExportReportType, ReportMeta> = {
  PRODUCTS: {
    endpoint: '/api/v1/export/products',
    description:
      'Product catalogue export — one row per product with pricing and status. ' +
      'Backend builds the file from the ProductService summary read model.',
    supportedFilters: ['search', 'categoryId', 'active', 'priceMin', 'priceMax'],
  },
  CUSTOMERS: {
    endpoint: '/api/v1/export/customers',
    description:
      'Customer export — one row per customer with contact and group details. ' +
      'startDate / endDate filter on customer creation time.',
    supportedFilters: ['keyword', 'status', 'groupId', 'startDate', 'endDate'],
  },
  ORDERS: {
    endpoint: '/api/v1/export/orders',
    description:
      'Order export — one row per order with totals, order status and payment status. ' +
      'dateFrom / dateTo filter on order creation time.',
    supportedFilters: [
      'keyword',
      'customerId',
      'status',
      'paymentStatus',
      'dateFrom',
      'dateTo',
    ],
  },
  REVENUE: {
    endpoint: '/api/v1/export/analytics/revenue',
    description:
      'Revenue time series export, grouped by DAY, WEEK or MONTH (backend default DAY).',
    supportedFilters: ['dateFrom', 'dateTo', 'groupBy'],
  },
  ORDER_ANALYTICS: {
    endpoint: '/api/v1/export/analytics/orders',
    description:
      'Order analytics export — order counts by status with completion and cancellation rates.',
    supportedFilters: ['dateFrom', 'dateTo'],
  },
  TOP_PRODUCTS: {
    endpoint: '/api/v1/export/analytics/products',
    description:
      'Top-products-by-revenue leaderboard export for a period. ' +
      'limit is an integer 1..100 (backend default 10).',
    supportedFilters: ['dateFrom', 'dateTo', 'limit'],
  },
  CUSTOMER_ANALYTICS: {
    endpoint: '/api/v1/export/analytics/customers',
    description:
      'Customer engagement analytics export — new, repeat and unique buyers for a period.',
    supportedFilters: ['dateFrom', 'dateTo'],
  },
  PAYMENT_ANALYTICS: {
    endpoint: '/api/v1/export/analytics/payments',
    description:
      'Payment-method analytics export — order value and count broken down by payment method.',
    supportedFilters: ['dateFrom', 'dateTo'],
  },
};

// ── Zod sub-schemas ────────────────────────────────────────────────────────

const reportTypeSchema = z
  .enum(EXPORT_REPORT_TYPES)
  .describe(
    'Export report: PRODUCTS, CUSTOMERS, ORDERS, REVENUE, ORDER_ANALYTICS, ' +
      'TOP_PRODUCTS, CUSTOMER_ANALYTICS, or PAYMENT_ANALYTICS.',
  );

const formatSchema = z
  .enum(EXPORT_FORMATS)
  .describe('Output file format: XLSX or PDF. Sent to the backend lower-cased.');

/**
 * ISO 8601 datetime string with timezone offset — matches the backend
 * @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant parameters.
 * Example: "2026-08-01T00:00:00Z".
 */
const isoDateTime = z
  .string()
  .datetime({ offset: true })
  .describe('ISO 8601 datetime with timezone, e.g. "2026-08-01T00:00:00Z".');

// Per-report STRICT filter schemas — unknown keys are rejected, and only the
// fields the Sprint 11A controller actually accepts are allowed.

const dateRangeFilters = z
  .object({
    dateFrom: isoDateTime.optional(),
    dateTo: isoDateTime.optional(),
  })
  .strict();

const FILTER_SCHEMAS: Record<ExportReportType, z.ZodTypeAny> = {
  PRODUCTS: z
    .object({
      search: z.string().trim().min(1).optional(),
      categoryId: z.string().uuid('categoryId must be a valid UUID').optional(),
      active: z.boolean().optional(),
      priceMin: z.number().nonnegative('priceMin must be 0 or greater').optional(),
      priceMax: z.number().nonnegative('priceMax must be 0 or greater').optional(),
    })
    .strict(),
  CUSTOMERS: z
    .object({
      keyword: z.string().trim().min(1).optional(),
      status: z.enum(CUSTOMER_STATUSES).optional(),
      groupId: z.string().uuid('groupId must be a valid UUID').optional(),
      startDate: isoDateTime.optional(),
      endDate: isoDateTime.optional(),
    })
    .strict(),
  ORDERS: z
    .object({
      keyword: z.string().trim().min(1).optional(),
      customerId: z.string().uuid('customerId must be a valid UUID').optional(),
      status: z.enum(ORDER_STATUSES).optional(),
      paymentStatus: z.enum(PAYMENT_STATUSES).optional(),
      dateFrom: isoDateTime.optional(),
      dateTo: isoDateTime.optional(),
    })
    .strict(),
  REVENUE: z
    .object({
      dateFrom: isoDateTime.optional(),
      dateTo: isoDateTime.optional(),
      groupBy: z.enum(REVENUE_GROUP_BY).optional(),
    })
    .strict(),
  ORDER_ANALYTICS: dateRangeFilters,
  TOP_PRODUCTS: z
    .object({
      dateFrom: isoDateTime.optional(),
      dateTo: isoDateTime.optional(),
      limit: z
        .number()
        .int('limit must be a whole number')
        .min(1, 'limit must be at least 1')
        .max(100, 'limit must not exceed 100')
        .optional(),
    })
    .strict(),
  CUSTOMER_ANALYTICS: dateRangeFilters,
  PAYMENT_ANALYTICS: dateRangeFilters,
};

/** Which filter keys hold the lower/upper date bound, per report. */
const DATE_BOUND_KEYS: Record<ExportReportType, [string, string]> = {
  PRODUCTS: ['', ''],
  CUSTOMERS: ['startDate', 'endDate'],
  ORDERS: ['dateFrom', 'dateTo'],
  REVENUE: ['dateFrom', 'dateTo'],
  ORDER_ANALYTICS: ['dateFrom', 'dateTo'],
  TOP_PRODUCTS: ['dateFrom', 'dateTo'],
  CUSTOMER_ANALYTICS: ['dateFrom', 'dateTo'],
  PAYMENT_ANALYTICS: ['dateFrom', 'dateTo'],
};

// ── Small local helpers ────────────────────────────────────────────────────

/** Standard success envelope for an MCP tool. */
function ok(data: unknown): { content: { type: 'text'; text: string }[] } {
  return { content: [{ type: 'text', text: JSON.stringify(data, null, 2) }] };
}

/** Standard non-throwing error envelope for an MCP tool. */
function fail(text: string): {
  content: { type: 'text'; text: string }[];
  isError: true;
} {
  return { content: [{ type: 'text', text }], isError: true };
}

/**
 * Builds the exact query-param object that WOULD be sent to the backend export
 * endpoint. `format` is lower-cased to match the controller convention
 * (`@RequestParam(defaultValue = "xlsx") String format`). undefined / null /
 * empty-string values are dropped so nothing is ever serialised as "undefined".
 * This is preview only — it is never actually sent.
 */
function buildExportParams(
  format: (typeof EXPORT_FORMATS)[number],
  filters: Record<string, unknown>,
): Record<string, unknown> {
  const out: Record<string, unknown> = { format: format.toLowerCase() };
  for (const [key, value] of Object.entries(filters)) {
    if (value === undefined || value === null || value === '') continue;
    out[key] = value;
  }
  return out;
}

/** Turns Zod issues into one safe, internal-free message string. */
function formatIssues(error: z.ZodError): string {
  return error.issues
    .map((i) => {
      const path = i.path.join('.') || '(filters)';
      return `${path}: ${i.message}`;
    })
    .join('; ');
}

// ── ExportToolsProvider ────────────────────────────────────────────────────

export class ExportToolsProvider implements McpProvider {
  // These tools make no backend requests, so the injected apiClient is unused.
  public register(server: McpServer, _apiClient: AxiosInstance): void {
    // ──────────────────────────────────────────────────────────────────────
    // 1. export_capabilities
    //    Static description of what the Export domain supports. ZERO backend
    //    requests — no export-capabilities endpoint exists to query.
    // ──────────────────────────────────────────────────────────────────────
    server.tool(
      'export_capabilities',
      'List what the Commerce Insight export domain supports: the eight report ' +
        'types and the two output formats (XLSX, PDF). This is static metadata — ' +
        'it makes no backend request and reports no live runtime capability. ' +
        'Read-only. Export files themselves are generated by the Commerce Insight ' +
        'backend and are not delivered through MCP.',
      {},
      async () => {
        return ok({
          supportedReports: [...EXPORT_REPORT_TYPES],
          supportedFormats: [...EXPORT_FORMATS],
          source: 'static-metadata',
          note: 'Export files are generated by the Commerce Insight backend. Binary export execution is intentionally not exposed through MCP.',
        });
      },
    );

    // ──────────────────────────────────────────────────────────────────────
    // 2. export_report_info
    //    Metadata for ONE export report: formats, description, the exact
    //    supported filter names, and the application endpoint. ZERO backend
    //    requests — derived strictly from the Sprint 11A ExportController.
    // ──────────────────────────────────────────────────────────────────────
    server.tool(
      'export_report_info',
      'Describe one export report (PRODUCTS, CUSTOMERS, ORDERS, REVENUE, ' +
        'ORDER_ANALYTICS, TOP_PRODUCTS, CUSTOMER_ANALYTICS, PAYMENT_ANALYTICS): ' +
        'its output formats, a short description, the exact filter parameters it ' +
        'accepts, and the backend API endpoint that generates it. Static metadata ' +
        'derived from the Sprint 11A export contract — makes no backend request. ' +
        'Read-only.',
      { reportType: reportTypeSchema },
      async ({ reportType }) => {
        const meta = REPORT_METADATA[reportType as ExportReportType];
        return ok({
          reportType,
          formats: [...EXPORT_FORMATS],
          description: meta.description,
          supportedFilters: meta.supportedFilters,
          endpoint: meta.endpoint,
          source: 'static-metadata',
        });
      },
    );

    // ──────────────────────────────────────────────────────────────────────
    // 3. export_request_preview
    //    Validate and normalise an export request WITHOUT generating a file.
    //    Strict per-report Zod validation + cross-field checks (date range,
    //    price range). Returns the query params that WOULD be sent. ZERO
    //    backend requests. Does not count rows, aggregate, or estimate size.
    // ──────────────────────────────────────────────────────────────────────
    server.tool(
      'export_request_preview',
      'Validate and normalise an export request WITHOUT generating any file and ' +
        'WITHOUT calling the backend. Checks the format, the report type, and only ' +
        'the filters that report actually supports (strict — unknown fields are ' +
        'rejected), enforces dateFrom <= dateTo, priceMin <= priceMax, and ' +
        'limit 1..100, then returns the normalised backend endpoint and the exact ' +
        'query parameters that a generation request would use. It never counts ' +
        'rows, aggregates data, or estimates file size. Read-only; makes no backend request.',
      {
        reportType: reportTypeSchema,
        format: formatSchema,
        filters: z
          .record(z.unknown())
          .optional()
          .default({})
          .describe(
            'Report-specific filter object. Only keys supported by the chosen ' +
              'report are allowed; any other key is rejected.',
          ),
      },
      async ({ reportType, format, filters }) => {
        const report = reportType as ExportReportType;

        // 0. Drop undefined / null / '' up front so an explicitly-empty optional
        //    filter is treated as omitted (same semantics as the frontend
        //    cleanParams). Genuinely unknown keys still reach .strict() below.
        const rawFilters: Record<string, unknown> = {};
        for (const [key, value] of Object.entries(
          (filters ?? {}) as Record<string, unknown>,
        )) {
          if (value === undefined || value === null || value === '') continue;
          rawFilters[key] = value;
        }

        // 1. Strict per-report filter validation.
        const schema = FILTER_SCHEMAS[report];
        const parsed = schema.safeParse(rawFilters);
        if (!parsed.success) {
          return fail(
            `Error [export_request_preview]: Invalid export request — ${formatIssues(parsed.error)}`,
          );
        }
        const cleanFilters = parsed.data as Record<string, unknown>;

        // 2. Cross-field: date range must not be inverted.
        const [fromKey, toKey] = DATE_BOUND_KEYS[report];
        if (fromKey && toKey) {
          const from = cleanFilters[fromKey] as string | undefined;
          const to = cleanFilters[toKey] as string | undefined;
          if (from && to && new Date(from) > new Date(to)) {
            return fail(
              `Error [export_request_preview]: ${fromKey} must not be after ${toKey}. ` +
                `Received ${fromKey}="${from}", ${toKey}="${to}".`,
            );
          }
        }

        // 3. Cross-field: price range must not be inverted (PRODUCTS only).
        if (report === 'PRODUCTS') {
          const min = cleanFilters['priceMin'] as number | undefined;
          const max = cleanFilters['priceMax'] as number | undefined;
          if (min !== undefined && max !== undefined && min > max) {
            return fail(
              `Error [export_request_preview]: priceMin must be less than or equal to priceMax. ` +
                `Received priceMin=${min}, priceMax=${max}.`,
            );
          }
        }

        // 4. Build the request that WOULD be sent (never actually sent).
        const queryParams = buildExportParams(format, cleanFilters);

        return ok({
          reportType: report,
          format,
          filters: cleanFilters,
          queryParams,
          endpoint: REPORT_METADATA[report].endpoint,
          ready: true,
          source: 'validation-only',
          note: 'No file was generated and no backend request was made. This tool only validates and normalises the request. Generate the report through the Commerce Insight application.',
        });
      },
    );
  }
}
