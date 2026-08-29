/**
 * MCP Tool: import
 *
 * Data Import Management tools for the Commerce Insight AI MCP Server (Sprint 10C).
 *
 * Registered tools:
 *   1. import_job_lookup       → GET  /api/v1/import/jobs/{jobId}
 *   2. import_job_search       → GET  /api/v1/import/jobs
 *   3. import_job_errors       → GET  /api/v1/import/jobs/{jobId}/errors
 *   4. import_template_info    → static metadata (no backend call — endpoint returns a binary file)
 *   5. import_capabilities     → static metadata (configured Sprint 10A defaults)
 *   6. import_status           → GET  /api/v1/import/jobs/{jobId}
 *   7. import_history_for_type → GET  /api/v1/import/jobs?importType={type}
 *
 * ARCHITECTURE RULES (enforced at every level):
 *   - Communicates ONLY via the injected apiClient → Spring Boot REST API.
 *   - NO database access (no SQL, Prisma, TypeORM, Drizzle, pg, mysql, sqlite).
 *   - NO CSV/XLSX parsing, NO file upload, NO import business logic.
 *   - NO ImportOrchestrator / domain-service duplication.
 *   - NO LLM calls (no OpenAI, Anthropic, Gemini, Ollama, LangChain, RAG).
 *   - NO polling loops, NO multi-page fetching — exactly ONE backend request per tool call
 *     (tools 4 and 5 make ZERO backend requests — they return static/configured metadata).
 *   - Outputs never expose stack traces, SQL, JWTs, API keys, DB credentials, or backend
 *     configuration internals.
 *   - All tools are READ-ONLY — no state changes.
 *
 * Backend response contract: the shared apiClient returns the ApiResponse envelope;
 * the domain payload is read as `response.data?.data`, matching every sibling provider
 * (ProductToolsProvider, CustomerToolsProvider, OrderToolsProvider, AnalyticsToolsProvider).
 *
 * Output contract: { content: [{ type: 'text', text: JSON.stringify(data, null, 2) }] }
 * Error contract:  toMcpToolError(toolName, error) → { content: [...], isError: true }
 */

import { z } from 'zod';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { AxiosInstance } from 'axios';
import { McpProvider } from '../providers/provider.interface.js';
import { toMcpToolError } from '../utils/error.handler.js';

// ── Shared Zod schemas — mirror the Spring Boot dataimport enums ─────────────

const IMPORT_TYPES = ['PRODUCT', 'CUSTOMER', 'ORDER'] as const;
const IMPORT_FILE_TYPES = ['CSV', 'XLSX'] as const;
const IMPORT_JOB_STATUSES = [
  'UPLOADED',
  'VALIDATING',
  'IMPORTING',
  'COMPLETED',
  'PARTIAL_SUCCESS',
  'FAILED',
] as const;

/** ImportType enum — PRODUCT | CUSTOMER | ORDER. */
const importTypeSchema = z
  .enum(IMPORT_TYPES)
  .describe('Import domain: PRODUCT, CUSTOMER, or ORDER.');

/** ImportJobStatus enum — job lifecycle state. */
const importJobStatusSchema = z
  .enum(IMPORT_JOB_STATUSES)
  .describe(
    'Import job lifecycle status: UPLOADED, VALIDATING, IMPORTING, COMPLETED, PARTIAL_SUCCESS, or FAILED.',
  );

/** Import job UUID. */
const jobIdSchema = z
  .string()
  .uuid('jobId must be a valid UUID')
  .describe('The UUID of the import job.');

/** Zero-based page index — never unbounded. */
const pageSchema = z
  .number()
  .int('page must be a whole number')
  .min(0, 'page must be 0 or greater')
  .optional()
  .default(0)
  .describe('Zero-based page index. Minimum 0. Default: 0.');

/** Page size — hard-capped so responses stay bounded. */
const sizeSchema = z
  .number()
  .int('size must be a whole number')
  .min(1, 'size must be at least 1')
  .max(100, 'size must not exceed 100')
  .optional()
  .default(10)
  .describe('Number of rows per page. Between 1 and 100. Default: 10.');

// ── Static template metadata — derived strictly from Sprint 10A definitions ──
//
// Source of truth (Spring Boot backend, Sprint 10A):
//   dataimport/service/ProductImportService.java  → REQUIRED_HEADERS / ALL_HEADERS
//   dataimport/service/CustomerImportService.java → REQUIRED_HEADERS / ALL_HEADERS
//   dataimport/service/OrderImportService.java    → REQUIRED_HEADERS / ALL_HEADERS
//
// The backend GET /import/templates/{type} endpoint returns a downloadable CSV
// file. MCP intentionally does NOT fetch or relay that binary content — it only
// tells the model which templates exist and where the application serves them.

type TemplateType = (typeof IMPORT_TYPES)[number];

interface TemplateMetadata {
  type: TemplateType;
  supportedFileTypes: string[];
  requiredColumns: string[];
  allColumns: string[];
  description: string;
  endpoint: string;
}

const TEMPLATE_METADATA: Record<TemplateType, TemplateMetadata> = {
  PRODUCT: {
    type: 'PRODUCT',
    supportedFileTypes: [...IMPORT_FILE_TYPES],
    requiredColumns: ['sku', 'name', 'price'],
    allColumns: ['sku', 'name', 'description', 'price', 'costprice', 'categoryname', 'imageurl'],
    description:
      'Product catalog import. One product per row, keyed by "sku". "price" and "costprice" ' +
      'are decimals; "categoryname" is resolved to an existing category; "imageurl" is optional.',
    endpoint: '/api/v1/import/templates/PRODUCT',
  },
  CUSTOMER: {
    type: 'CUSTOMER',
    supportedFileTypes: [...IMPORT_FILE_TYPES],
    requiredColumns: ['firstname', 'lastname'],
    allColumns: ['firstname', 'lastname', 'email', 'phone', 'dateofbirth', 'gender', 'groupname'],
    description:
      'Customer import. One customer per row. "dateofbirth" is an ISO date (YYYY-MM-DD); ' +
      '"gender" is MALE / FEMALE / OTHER / PREFER_NOT_TO_SAY; "groupname" is resolved to an ' +
      'existing customer group.',
    endpoint: '/api/v1/import/templates/CUSTOMER',
  },
  ORDER: {
    type: 'ORDER',
    supportedFileTypes: [...IMPORT_FILE_TYPES],
    requiredColumns: ['ordernumber', 'customeremail', 'productsku', 'quantity', 'paymentmethod'],
    allColumns: [
      'ordernumber',
      'customeremail',
      'productsku',
      'quantity',
      'itemdiscount',
      'paymentmethod',
      'shippingfee',
      'orderdiscount',
      'tax',
      'currency',
      'notes',
    ],
    description:
      'Order import. Rows that share an "ordernumber" form one multi-line order. ' +
      '"customeremail" and "productsku" must match existing records; ' +
      '"paymentmethod" is CASH / BANK_TRANSFER / CARD / OTHER.',
    endpoint: '/api/v1/import/templates/ORDER',
  },
};

// ── Configured Sprint 10A import defaults ────────────────────────────────────
//
// These mirror the backend `app.import.*` properties (ImportProperties.java).
// They are CONFIGURED DEFAULTS, not live runtime values — the backend currently
// exposes no import-configuration endpoint to query.
const IMPORT_DEFAULTS = {
  maxFileSizeMb: 10,
  maxRows: 5000,
} as const;

// ── Small local helpers ─────────────────────────────────────────────────────

/** Drops undefined entries so Axios never serialises `?x=undefined`. */
function cleanParams(input: Record<string, unknown>): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(input)) {
    if (value !== undefined) out[key] = value;
  }
  return out;
}

/**
 * Normalises a Spring Boot PageResponse
 *   { content, page, size, totalElements, totalPages, first, last }
 * into the flat pagination view these tools expose. Defensive against missing
 * fields; never fetches more pages.
 */
function pageMeta(page: unknown): {
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
} {
  const p = (page ?? {}) as Record<string, unknown>;
  const num = (v: unknown): number => (typeof v === 'number' && Number.isFinite(v) ? v : 0);
  return {
    totalElements: num(p['totalElements']),
    totalPages: num(p['totalPages']),
    currentPage: num(p['page']),
    pageSize: num(p['size']),
  };
}

/** Extracts the `content` array from a PageResponse, defaulting to []. */
function pageContent(page: unknown): unknown[] {
  const c = (page as Record<string, unknown> | null | undefined)?.['content'];
  return Array.isArray(c) ? c : [];
}

/** Standard success envelope for an MCP tool. */
function ok(data: unknown): { content: { type: 'text'; text: string }[] } {
  return { content: [{ type: 'text', text: JSON.stringify(data, null, 2) }] };
}

/** Standard non-throwing error envelope for an MCP tool. */
function fail(text: string): { content: { type: 'text'; text: string }[]; isError: true } {
  return { content: [{ type: 'text', text }], isError: true };
}

// ── ImportToolsProvider ─────────────────────────────────────────────────────

export class ImportToolsProvider implements McpProvider {
  public register(server: McpServer, apiClient: AxiosInstance): void {
    // ────────────────────────────────────────────────────────────────────────
    // 1. import_job_lookup
    //    Full detail for one import job. Returns the backend ImportJobResponse
    //    with no transformation. READ-ONLY. Exactly one backend request.
    //    Backend: GET /api/v1/import/jobs/{jobId}
    // ────────────────────────────────────────────────────────────────────────
    server.tool(
      'import_job_lookup',
      'Retrieve full details of a single data-import job by its UUID. Returns the backend ' +
        'ImportJobResponse as-is: id, fileName, fileType (CSV/XLSX), importType (PRODUCT/CUSTOMER/ORDER), ' +
        'status, totalRows, successfulRows, failedRows, startedAt, completedAt, createdAt, createdByEmail. ' +
        'Use this when you already know the import job ID and want the complete record. ' +
        'Read-only; makes exactly one backend request.',
      { jobId: jobIdSchema },
      async ({ jobId }) => {
        try {
          const response = await apiClient.get(`/import/jobs/${jobId}`);
          const job = response.data?.data;

          if (!job) {
            return fail(`No import job data was returned for ID: ${jobId}`);
          }

          return ok(job);
        } catch (error) {
          return toMcpToolError('import_job_lookup', error);
        }
      },
    );

    // ────────────────────────────────────────────────────────────────────────
    // 2. import_job_search
    //    Paginated search over import job history with optional filters.
    //    READ-ONLY. Exactly one backend request — no manual pagination.
    //    Backend: GET /api/v1/import/jobs
    // ────────────────────────────────────────────────────────────────────────
    server.tool(
      'import_job_search',
      'Search the data-import job history. Optional filters: importType (PRODUCT/CUSTOMER/ORDER) ' +
        'and status (UPLOADED/VALIDATING/IMPORTING/COMPLETED/PARTIAL_SUCCESS/FAILED). ' +
        'Returns a single page: { totalElements, totalPages, currentPage, pageSize, jobs }. ' +
        'Read-only; makes exactly one backend request and never fetches additional pages.',
      {
        importType: importTypeSchema.optional(),
        status: importJobStatusSchema.optional(),
        page: pageSchema,
        size: sizeSchema,
      },
      async ({ importType, status, page, size }) => {
        try {
          const params = cleanParams({ importType, status, page, size });
          const response = await apiClient.get('/import/jobs', { params });
          const pageData = response.data?.data;

          if (!pageData) {
            return fail('No import job data was returned from the backend.');
          }

          return ok({ ...pageMeta(pageData), jobs: pageContent(pageData) });
        } catch (error) {
          return toMcpToolError('import_job_search', error);
        }
      },
    );

    // ────────────────────────────────────────────────────────────────────────
    // 3. import_job_errors
    //    One page of row-level errors for a job. READ-ONLY.
    //    Exactly one backend request — does NOT walk every error page.
    //    Backend: GET /api/v1/import/jobs/{jobId}/errors
    // ────────────────────────────────────────────────────────────────────────
    server.tool(
      'import_job_errors',
      'Retrieve ONE page of row-level errors for a data-import job. Each error contains ' +
        'rowNumber, fieldName, rawValue, errorCode, and errorMessage. ' +
        'Returns { jobId, totalElements, totalPages, currentPage, pageSize, errors }. ' +
        'Read-only; makes exactly one backend request. To see more errors, call again with the next page — ' +
        'this tool never auto-fetches all pages.',
      { jobId: jobIdSchema, page: pageSchema, size: sizeSchema },
      async ({ jobId, page, size }) => {
        try {
          const params = cleanParams({ page, size });
          const response = await apiClient.get(`/import/jobs/${jobId}/errors`, { params });
          const pageData = response.data?.data;

          if (!pageData) {
            return fail(`No import error data was returned for job: ${jobId}`);
          }

          return ok({ jobId, ...pageMeta(pageData), errors: pageContent(pageData) });
        } catch (error) {
          return toMcpToolError('import_job_errors', error);
        }
      },
    );

    // ────────────────────────────────────────────────────────────────────────
    // 4. import_template_info
    //    Static template metadata for a given type. NO backend request — the
    //    backend template endpoint returns a downloadable file, which MCP must
    //    not relay. Metadata is derived strictly from Sprint 10A definitions.
    // ────────────────────────────────────────────────────────────────────────
    server.tool(
      'import_template_info',
      'Describe the import file template for a given type (PRODUCT, CUSTOMER, or ORDER): ' +
        'supported file types, required columns, all columns, a short description, and the ' +
        'application endpoint that serves the downloadable template file. ' +
        'This is static metadata derived from the Sprint 10A template definitions — MCP does NOT ' +
        'download or return binary file content. Read-only; makes no backend request.',
      { type: importTypeSchema },
      async ({ type }) => {
        const meta = TEMPLATE_METADATA[type];
        return ok({
          ...meta,
          source: 'static-metadata',
          note:
            'This is template metadata, not the template file. The endpoint returns a downloadable ' +
            'CSV file — request it through the Commerce Insight application, not through MCP.',
        });
      },
    );

    // ────────────────────────────────────────────────────────────────────────
    // 5. import_capabilities
    //    Read-only informational tool. Reports supported types / file types and
    //    the CONFIGURED Sprint 10A limits. NO backend request (no config
    //    endpoint exists to query).
    // ────────────────────────────────────────────────────────────────────────
    server.tool(
      'import_capabilities',
      'List the data-import capabilities of the platform: supported import types, supported file ' +
        'types, and the configured maximum file size (MB) and maximum row count. ' +
        'maxFileSizeMb and maxRows are the CONFIGURED Sprint 10A backend defaults (app.import.*), ' +
        'not values fetched from a live runtime endpoint. Read-only; makes no backend request.',
      {},
      async () => {
        return ok({
          supportedTypes: [...IMPORT_TYPES],
          supportedFileTypes: [...IMPORT_FILE_TYPES],
          maxFileSizeMb: IMPORT_DEFAULTS.maxFileSizeMb,
          maxRows: IMPORT_DEFAULTS.maxRows,
          source: 'configured-defaults',
          note:
            'maxFileSizeMb and maxRows reflect the backend app.import.* configuration defaults ' +
            'documented in Sprint 10A. The backend exposes no import-configuration endpoint, so ' +
            'these are configured defaults, not dynamically fetched runtime values.',
        });
      },
    );

    // ────────────────────────────────────────────────────────────────────────
    // 6. import_status
    //    Concise status view for one job. progressSummary is derived minimally
    //    from the backend counters only. NO polling. Exactly one backend request.
    //    Backend: GET /api/v1/import/jobs/{jobId}
    // ────────────────────────────────────────────────────────────────────────
    server.tool(
      'import_status',
      'Return a concise status view for one import job: status, row counters, timestamps, and a ' +
        'minimal progressSummary derived only from the backend counters ' +
        '(processedRows = successfulRows + failedRows; completionPercent is included only when ' +
        'totalRows > 0, computed as processedRows / totalRows). ' +
        'Read-only; makes exactly one backend request. Does NOT poll or wait for completion.',
      { jobId: jobIdSchema },
      async ({ jobId }) => {
        try {
          const response = await apiClient.get(`/import/jobs/${jobId}`);
          const job = response.data?.data;

          if (!job) {
            return fail(`No import job data was returned for ID: ${jobId}`);
          }

          const toNum = (v: unknown): number =>
            typeof v === 'number' && Number.isFinite(v) ? v : 0;

          const totalRows = toNum(job.totalRows);
          const successfulRows = toNum(job.successfulRows);
          const failedRows = toNum(job.failedRows);
          const processedRows = successfulRows + failedRows;

          const TERMINAL_STATUSES = ['COMPLETED', 'PARTIAL_SUCCESS', 'FAILED'];
          const progressSummary: Record<string, unknown> = {
            processedRows,
            totalRows,
            terminal: TERMINAL_STATUSES.includes(job.status),
          };
          if (totalRows > 0) {
            // (successfulRows + failedRows) / totalRows, rounded to 2 decimals.
            progressSummary['completionPercent'] =
              Math.round((processedRows / totalRows) * 10000) / 100;
          }

          return ok({
            jobId: job.id ?? jobId,
            status: job.status,
            totalRows,
            successfulRows,
            failedRows,
            progressSummary,
            createdAt: job.createdAt ?? null,
            startedAt: job.startedAt ?? null,
            completedAt: job.completedAt ?? null,
          });
        } catch (error) {
          return toMcpToolError('import_status', error);
        }
      },
    );

    // ────────────────────────────────────────────────────────────────────────
    // 7. import_history_for_type
    //    Recent jobs for one import type. Filtering is done by the backend via
    //    the importType query param — never client-side. READ-ONLY.
    //    Exactly one backend request; no loops.
    //    Backend: GET /api/v1/import/jobs?importType={type}
    // ────────────────────────────────────────────────────────────────────────
    server.tool(
      'import_history_for_type',
      'Retrieve ONE page of recent import jobs for a specific import type (PRODUCT, CUSTOMER, or ' +
        'ORDER), ordered by the backend (newest first). Filtering is performed by the backend via ' +
        'the importType query parameter — never client-side. ' +
        'Returns { type, totalElements, totalPages, currentPage, pageSize, jobs }. ' +
        'Read-only; makes exactly one backend request and never loops over pages.',
      { type: importTypeSchema, page: pageSchema, size: sizeSchema },
      async ({ type, page, size }) => {
        try {
          const params = cleanParams({ importType: type, page, size });
          const response = await apiClient.get('/import/jobs', { params });
          const pageData = response.data?.data;

          if (!pageData) {
            return fail(`No import job data was returned for type: ${type}`);
          }

          return ok({ type, ...pageMeta(pageData), jobs: pageContent(pageData) });
        } catch (error) {
          return toMcpToolError('import_history_for_type', error);
        }
      },
    );
  }
}
