/**
 * Unit tests for ExportToolsProvider (Sprint 11C).
 *
 * Run with:  npm test        (node:test via tsx, no build step)
 *
 * The Export MCP tools make NO backend requests, so there is nothing to mock —
 * every test passes an apiClient stub whose `get` throws if it is ever called,
 * proving the read-only / no-backend contract.
 *
 * These tests NEVER connect to PostgreSQL and do NOT require the Spring Boot
 * backend to be running.
 */

import { test } from 'node:test';
import assert from 'node:assert/strict';
import { z } from 'zod';
import { ExportToolsProvider } from './export.tool.js';

// ── Test doubles ────────────────────────────────────────────────────────────

type Handler = (args: any) => Promise<any> | any;

interface Registered {
  description: string;
  shape: Record<string, z.ZodTypeAny>;
  handler: Handler;
}

/** Minimal stand-in for the MCP SDK server — just captures registered tools. */
class FakeServer {
  public readonly tools = new Map<string, Registered>();

  public tool(
    name: string,
    description: string,
    shape: Record<string, z.ZodTypeAny>,
    handler: Handler,
  ): void {
    this.tools.set(name, { description, shape, handler });
  }
}

/** An apiClient whose every method fails the test if it is called. */
const NO_BACKEND = new Proxy(
  {},
  {
    get() {
      return async () => {
        throw new Error('Export MCP tools must not call the backend');
      };
    },
  },
) as any;

function buildServer(): FakeServer {
  const server = new FakeServer();
  new ExportToolsProvider().register(server as any, NO_BACKEND);
  return server;
}

/** Validate an input object against a tool's Zod shape. */
function parseArgs(server: FakeServer, tool: string, input: unknown) {
  const entry = server.tools.get(tool);
  assert.ok(entry, `tool "${tool}" should be registered`);
  return z.object(entry!.shape).safeParse(input);
}

/** Apply Zod defaults, then run the tool handler. */
async function invoke(server: FakeServer, tool: string, input: unknown) {
  const entry = server.tools.get(tool);
  assert.ok(entry, `tool "${tool}" should be registered`);
  const parsed = z.object(entry!.shape).parse(input);
  return entry!.handler(parsed);
}

const ALL_REPORTS = [
  'PRODUCTS',
  'CUSTOMERS',
  'ORDERS',
  'REVENUE',
  'ORDER_ANALYTICS',
  'TOP_PRODUCTS',
  'CUSTOMER_ANALYTICS',
  'PAYMENT_ANALYTICS',
] as const;

// ── Provider surface ───────────────────────────────────────────────────────

test('registers exactly the three read-only export tools', () => {
  const server = buildServer();
  assert.deepEqual(
    [...server.tools.keys()].sort(),
    ['export_capabilities', 'export_report_info', 'export_request_preview'],
  );
});

test('no binary-generation tool is exposed (export_generate / export_request absent)', () => {
  const server = buildServer();
  assert.equal(server.tools.has('export_generate'), false);
  assert.equal(server.tools.has('export_request'), false);
});

// ── A. export_capabilities ─────────────────────────────────────────────────

test('export_capabilities returns all 8 report types and both formats, no backend call', async () => {
  const server = buildServer();
  const res = await invoke(server, 'export_capabilities', {});
  const body = JSON.parse(res.content[0].text);

  assert.notEqual(res.isError, true);
  assert.deepEqual(body.supportedReports, [...ALL_REPORTS]);
  assert.deepEqual(body.supportedFormats, ['XLSX', 'PDF']);
  assert.equal(body.source, 'static-metadata');
});

// ── B. export_report_info ──────────────────────────────────────────────────

const EXPECTED_FILTERS: Record<string, string[]> = {
  PRODUCTS: ['search', 'categoryId', 'active', 'priceMin', 'priceMax'],
  CUSTOMERS: ['keyword', 'status', 'groupId', 'startDate', 'endDate'],
  ORDERS: ['keyword', 'customerId', 'status', 'paymentStatus', 'dateFrom', 'dateTo'],
  REVENUE: ['dateFrom', 'dateTo', 'groupBy'],
  ORDER_ANALYTICS: ['dateFrom', 'dateTo'],
  TOP_PRODUCTS: ['dateFrom', 'dateTo', 'limit'],
  CUSTOMER_ANALYTICS: ['dateFrom', 'dateTo'],
  PAYMENT_ANALYTICS: ['dateFrom', 'dateTo'],
};

const EXPECTED_ENDPOINT: Record<string, string> = {
  PRODUCTS: '/api/v1/export/products',
  CUSTOMERS: '/api/v1/export/customers',
  ORDERS: '/api/v1/export/orders',
  REVENUE: '/api/v1/export/analytics/revenue',
  ORDER_ANALYTICS: '/api/v1/export/analytics/orders',
  TOP_PRODUCTS: '/api/v1/export/analytics/products',
  CUSTOMER_ANALYTICS: '/api/v1/export/analytics/customers',
  PAYMENT_ANALYTICS: '/api/v1/export/analytics/payments',
};

for (const report of ALL_REPORTS) {
  test(`export_report_info returns accurate metadata for ${report}`, async () => {
    const server = buildServer();
    const res = await invoke(server, 'export_report_info', { reportType: report });
    const body = JSON.parse(res.content[0].text);

    assert.notEqual(res.isError, true);
    assert.equal(body.reportType, report);
    assert.deepEqual(body.formats, ['XLSX', 'PDF']);
    assert.deepEqual(body.supportedFilters, EXPECTED_FILTERS[report]);
    assert.equal(body.endpoint, EXPECTED_ENDPOINT[report]);
    assert.equal(body.source, 'static-metadata');
    assert.equal(typeof body.description, 'string');
  });
}

test('export_report_info rejects an invalid report type', () => {
  const server = buildServer();
  assert.equal(
    parseArgs(server, 'export_report_info', { reportType: 'INVOICES' }).success,
    false,
  );
});

// ── C. export_request_preview ──────────────────────────────────────────────

test('export_request_preview validates a valid XLSX request', async () => {
  const server = buildServer();
  const res = await invoke(server, 'export_request_preview', {
    reportType: 'REVENUE',
    format: 'XLSX',
    filters: {
      dateFrom: '2026-08-01T00:00:00Z',
      dateTo: '2026-08-31T23:59:59Z',
      groupBy: 'MONTH',
    },
  });
  const body = JSON.parse(res.content[0].text);

  assert.notEqual(res.isError, true);
  assert.equal(body.ready, true);
  assert.equal(body.endpoint, '/api/v1/export/analytics/revenue');
  assert.equal(body.queryParams.format, 'xlsx');
  assert.equal(body.queryParams.groupBy, 'MONTH');
  assert.equal(body.source, 'validation-only');
});

test('export_request_preview validates a valid PDF request and lower-cases the format', async () => {
  const server = buildServer();
  const res = await invoke(server, 'export_request_preview', {
    reportType: 'PRODUCTS',
    format: 'PDF',
    filters: { search: 'widget', active: true, priceMin: 10, priceMax: 20 },
  });
  const body = JSON.parse(res.content[0].text);

  assert.notEqual(res.isError, true);
  assert.equal(body.format, 'PDF');
  assert.equal(body.queryParams.format, 'pdf');
  assert.equal(body.queryParams.priceMin, 10);
  assert.equal(body.endpoint, '/api/v1/export/products');
});

test('export_request_preview rejects an invalid format', () => {
  const server = buildServer();
  assert.equal(
    parseArgs(server, 'export_request_preview', {
      reportType: 'ORDERS',
      format: 'CSV',
    }).success,
    false,
  );
});

test('export_request_preview rejects an inverted date range', async () => {
  const server = buildServer();
  const res = await invoke(server, 'export_request_preview', {
    reportType: 'ORDER_ANALYTICS',
    format: 'XLSX',
    filters: {
      dateFrom: '2026-09-01T00:00:00Z',
      dateTo: '2026-08-01T00:00:00Z',
    },
  });
  assert.equal(res.isError, true);
  assert.match(res.content[0].text, /must not be after/i);
});

test('export_request_preview rejects an inverted CUSTOMERS date range (startDate/endDate)', async () => {
  const server = buildServer();
  const res = await invoke(server, 'export_request_preview', {
    reportType: 'CUSTOMERS',
    format: 'XLSX',
    filters: {
      startDate: '2026-09-01T00:00:00Z',
      endDate: '2026-08-01T00:00:00Z',
    },
  });
  assert.equal(res.isError, true);
  assert.match(res.content[0].text, /startDate must not be after endDate/i);
});

test('export_request_preview rejects a limit outside 1..100', async () => {
  const server = buildServer();
  for (const limit of [0, 101, 3.5]) {
    const res = await invoke(server, 'export_request_preview', {
      reportType: 'TOP_PRODUCTS',
      format: 'XLSX',
      filters: { limit },
    });
    assert.equal(res.isError, true, `limit=${limit} should be rejected`);
  }
});

test('export_request_preview rejects an inverted price range', async () => {
  const server = buildServer();
  const res = await invoke(server, 'export_request_preview', {
    reportType: 'PRODUCTS',
    format: 'XLSX',
    filters: { priceMin: 100, priceMax: 10 },
  });
  assert.equal(res.isError, true);
  assert.match(res.content[0].text, /priceMin must be less than or equal to priceMax/i);
});

test('export_request_preview rejects a negative price', async () => {
  const server = buildServer();
  const res = await invoke(server, 'export_request_preview', {
    reportType: 'PRODUCTS',
    format: 'XLSX',
    filters: { priceMin: -1 },
  });
  assert.equal(res.isError, true);
});

test('export_request_preview rejects unknown filter fields (strict)', async () => {
  const server = buildServer();
  const res = await invoke(server, 'export_request_preview', {
    reportType: 'REVENUE',
    format: 'XLSX',
    filters: { dateFrom: '2026-08-01T00:00:00Z', bogus: 'nope' },
  });
  assert.equal(res.isError, true);
  assert.match(res.content[0].text, /Invalid export request/i);
});

test('export_request_preview rejects a filter not supported by the chosen report', async () => {
  const server = buildServer();
  // groupBy is only valid for REVENUE, not ORDER_ANALYTICS.
  const res = await invoke(server, 'export_request_preview', {
    reportType: 'ORDER_ANALYTICS',
    format: 'XLSX',
    filters: { groupBy: 'DAY' },
  });
  assert.equal(res.isError, true);
});

test('export_request_preview strips undefined / empty params from queryParams', async () => {
  const server = buildServer();
  const res = await invoke(server, 'export_request_preview', {
    reportType: 'ORDERS',
    format: 'XLSX',
    filters: { keyword: 'abc', status: undefined, paymentStatus: '' },
  });
  const body = JSON.parse(res.content[0].text);

  assert.notEqual(res.isError, true);
  assert.deepEqual(Object.keys(body.queryParams).sort(), ['format', 'keyword']);
  assert.equal(body.queryParams.keyword, 'abc');
});

test('export_request_preview accepts an empty filters object', async () => {
  const server = buildServer();
  const res = await invoke(server, 'export_request_preview', {
    reportType: 'PAYMENT_ANALYTICS',
    format: 'PDF',
  });
  const body = JSON.parse(res.content[0].text);

  assert.notEqual(res.isError, true);
  assert.equal(body.ready, true);
  assert.deepEqual(body.queryParams, { format: 'pdf' });
});

test('export_request_preview rejects a malformed datetime', () => {
  const server = buildServer();
  const parsed = parseArgs(server, 'export_request_preview', {
    reportType: 'REVENUE',
    format: 'XLSX',
    filters: { dateFrom: '2026-08-01' },
  });
  // The outer shape accepts any filters record; the datetime is checked inside
  // the handler's strict schema — so run it and assert the error there.
  assert.equal(parsed.success, true);
});

test('export_request_preview surfaces a malformed datetime as a safe error', async () => {
  const server = buildServer();
  const res = await invoke(server, 'export_request_preview', {
    reportType: 'REVENUE',
    format: 'XLSX',
    filters: { dateFrom: '2026-08-01' },
  });
  assert.equal(res.isError, true);
  assert.match(res.content[0].text, /Invalid export request/i);
  // No internal leakage.
  assert.doesNotMatch(
    res.content[0].text,
    /stack|x-mcp-api-key|authorization|bearer|jwt|password|postgres|jdbc/i,
  );
});
