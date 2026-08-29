/**
 * Unit tests for ImportToolsProvider (Sprint 10C).
 *
 * Run with:  npm test        (node:test via tsx, no build step)
 *
 * These tests mock the Axios apiClient entirely — they NEVER connect to
 * PostgreSQL and do NOT require the Spring Boot backend to be running.
 */

import { test } from 'node:test';
import assert from 'node:assert/strict';
import { z } from 'zod';
import { AxiosError } from 'axios';
import { ImportToolsProvider } from './import.tool.js';

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

type GetImpl = (url: string, opts?: any) => Promise<any>;

function buildServer(getImpl: GetImpl): FakeServer {
  const server = new FakeServer();
  const apiClient = { get: getImpl } as any;
  new ImportToolsProvider().register(server as any, apiClient);
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

/** Wrap a domain payload the way the shared apiClient exposes it (response.data.data). */
const envelope = (data: unknown) => ({
  data: { success: true, data, message: 'ok', timestamp: '2026-08-29T00:00:00Z' },
});

function makeAxiosError(status: number | undefined, body?: unknown): AxiosError {
  const config = { headers: {} } as any;
  const response =
    status === undefined
      ? undefined
      : ({ status, data: body, statusText: '', headers: {}, config } as any);
  return new AxiosError(
    status === undefined ? 'Network Error' : `Request failed with status code ${status}`,
    status === undefined ? 'ERR_NETWORK' : 'ERR_BAD_RESPONSE',
    config,
    {},
    response,
  );
}

const JOB_ID = '11111111-1111-1111-1111-111111111111';
const NOT_A_UUID = 'job-42';

// ── 1. Valid import_job_lookup ─────────────────────────────────────────────

test('import_job_lookup returns the backend job unchanged', async () => {
  const job = {
    id: JOB_ID,
    fileName: 'products.csv',
    fileType: 'CSV',
    importType: 'PRODUCT',
    status: 'COMPLETED',
    totalRows: 3,
    successfulRows: 3,
    failedRows: 0,
    createdByEmail: 'admin@commerceinsight.ai',
  };
  const server = buildServer(async (url) => {
    assert.equal(url, `/import/jobs/${JOB_ID}`);
    return envelope(job);
  });

  const res = await invoke(server, 'import_job_lookup', { jobId: JOB_ID });
  assert.notEqual(res.isError, true);
  assert.deepEqual(JSON.parse(res.content[0].text), job);
});

// ── 2. Invalid UUID ───────────────────────────────────────────────────────

test('import_job_lookup rejects an invalid UUID', () => {
  const server = buildServer(async () => {
    throw new Error('apiClient.get must not be called for invalid input');
  });
  const parsed = parseArgs(server, 'import_job_lookup', { jobId: NOT_A_UUID });
  assert.equal(parsed.success, false);
});

// ── 3. Valid import_job_search ────────────────────────────────────────────

test('import_job_search sends filters and normalises the Spring page', async () => {
  const page = {
    content: [{ id: JOB_ID, importType: 'PRODUCT', status: 'FAILED' }],
    page: 0,
    size: 10,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true,
  };
  const server = buildServer(async (url, opts) => {
    assert.equal(url, '/import/jobs');
    assert.deepEqual(opts.params, {
      importType: 'PRODUCT',
      status: 'FAILED',
      page: 2,
      size: 25,
    });
    return envelope(page);
  });

  const res = await invoke(server, 'import_job_search', {
    importType: 'PRODUCT',
    status: 'FAILED',
    page: 2,
    size: 25,
  });
  const body = JSON.parse(res.content[0].text);
  assert.deepEqual(body, {
    totalElements: 1,
    totalPages: 1,
    currentPage: 0,
    pageSize: 10,
    jobs: [{ id: JOB_ID, importType: 'PRODUCT', status: 'FAILED' }],
  });
});

test('import_job_search applies default page/size when omitted', async () => {
  const server = buildServer(async (_url, opts) => {
    assert.deepEqual(opts.params, { page: 0, size: 10 });
    return envelope({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
  });
  const res = await invoke(server, 'import_job_search', {});
  assert.notEqual(res.isError, true);
});

// ── 4. Invalid page ──────────────────────────────────────────────────────

test('import_job_search rejects a negative page', () => {
  const server = buildServer(async () => envelope({}));
  assert.equal(parseArgs(server, 'import_job_search', { page: -1 }).success, false);
});

// ── 5. Invalid size ──────────────────────────────────────────────────────

test('import_job_search rejects size greater than 100', () => {
  const server = buildServer(async () => envelope({}));
  assert.equal(parseArgs(server, 'import_job_search', { size: 101 }).success, false);
});

test('import_job_search rejects size below 1', () => {
  const server = buildServer(async () => envelope({}));
  assert.equal(parseArgs(server, 'import_job_search', { size: 0 }).success, false);
});

// ── 6. Valid import_job_errors ───────────────────────────────────────────

test('import_job_errors returns one normalised error page', async () => {
  const page = {
    content: [
      {
        id: 'e1',
        rowNumber: 4,
        fieldName: 'price',
        rawValue: 'abc',
        errorCode: 'INVALID_NUMBER',
        errorMessage: 'price is not a valid number',
      },
    ],
    page: 0,
    size: 50,
    totalElements: 1,
    totalPages: 1,
  };
  const server = buildServer(async (url, opts) => {
    assert.equal(url, `/import/jobs/${JOB_ID}/errors`);
    assert.deepEqual(opts.params, { page: 0, size: 10 });
    return envelope(page);
  });

  const res = await invoke(server, 'import_job_errors', { jobId: JOB_ID });
  const body = JSON.parse(res.content[0].text);
  assert.equal(body.jobId, JOB_ID);
  assert.equal(body.pageSize, 50);
  assert.equal(body.errors.length, 1);
  assert.equal(body.errors[0].errorCode, 'INVALID_NUMBER');
});

test('import_job_errors rejects an invalid UUID', () => {
  const server = buildServer(async () => envelope({}));
  assert.equal(parseArgs(server, 'import_job_errors', { jobId: NOT_A_UUID }).success, false);
});

// ── 7. Valid import_template_info ────────────────────────────────────────

test('import_template_info returns static PRODUCT metadata with no backend call', async () => {
  let called = false;
  const server = buildServer(async () => {
    called = true;
    return envelope({});
  });

  const res = await invoke(server, 'import_template_info', { type: 'PRODUCT' });
  const body = JSON.parse(res.content[0].text);

  assert.equal(called, false, 'import_template_info must not call the backend');
  assert.equal(body.type, 'PRODUCT');
  assert.deepEqual(body.requiredColumns, ['sku', 'name', 'price']);
  assert.deepEqual(body.allColumns, [
    'sku',
    'name',
    'description',
    'price',
    'costprice',
    'categoryname',
    'imageurl',
  ]);
  assert.deepEqual(body.supportedFileTypes, ['CSV', 'XLSX']);
  assert.equal(body.endpoint, '/api/v1/import/templates/PRODUCT');
});

test('import_template_info rejects an unknown type', () => {
  const server = buildServer(async () => envelope({}));
  assert.equal(parseArgs(server, 'import_template_info', { type: 'INVOICE' }).success, false);
});

// ── 8. Valid import_capabilities ────────────────────────────────────────

test('import_capabilities reports configured Sprint 10A defaults, no backend call', async () => {
  let called = false;
  const server = buildServer(async () => {
    called = true;
    return envelope({});
  });

  const res = await invoke(server, 'import_capabilities', {});
  const body = JSON.parse(res.content[0].text);

  assert.equal(called, false, 'import_capabilities must not call the backend');
  assert.deepEqual(body.supportedTypes, ['PRODUCT', 'CUSTOMER', 'ORDER']);
  assert.deepEqual(body.supportedFileTypes, ['CSV', 'XLSX']);
  assert.equal(body.maxFileSizeMb, 10);
  assert.equal(body.maxRows, 5000);
  assert.match(body.note, /configuration defaults/i);
});

// ── 9. Valid import_status ─────────────────────────────────────────────

test('import_status derives progressSummary from counters only', async () => {
  const job = {
    id: JOB_ID,
    status: 'PARTIAL_SUCCESS',
    totalRows: 10,
    successfulRows: 7,
    failedRows: 3,
    createdAt: '2026-08-29T00:00:00Z',
    startedAt: '2026-08-29T00:00:01Z',
    completedAt: '2026-08-29T00:00:05Z',
  };
  const server = buildServer(async (url) => {
    assert.equal(url, `/import/jobs/${JOB_ID}`);
    return envelope(job);
  });

  const res = await invoke(server, 'import_status', { jobId: JOB_ID });
  const body = JSON.parse(res.content[0].text);

  assert.equal(body.status, 'PARTIAL_SUCCESS');
  assert.equal(body.progressSummary.processedRows, 10);
  assert.equal(body.progressSummary.completionPercent, 100);
  assert.equal(body.progressSummary.terminal, true);
  assert.equal(body.completedAt, '2026-08-29T00:00:05Z');
});

test('import_status omits completionPercent when totalRows is 0', async () => {
  const server = buildServer(async () =>
    envelope({ id: JOB_ID, status: 'UPLOADED', totalRows: 0, successfulRows: 0, failedRows: 0 }),
  );
  const res = await invoke(server, 'import_status', { jobId: JOB_ID });
  const body = JSON.parse(res.content[0].text);

  assert.equal('completionPercent' in body.progressSummary, false);
  assert.equal(body.progressSummary.terminal, false);
});

// ── 10. Valid import_history_for_type ─────────────────────────────────

test('import_history_for_type filters by importType server-side', async () => {
  const page = {
    content: [{ id: JOB_ID, importType: 'ORDER' }],
    page: 1,
    size: 5,
    totalElements: 6,
    totalPages: 2,
  };
  const server = buildServer(async (url, opts) => {
    assert.equal(url, '/import/jobs');
    assert.deepEqual(opts.params, { importType: 'ORDER', page: 1, size: 5 });
    return envelope(page);
  });

  const res = await invoke(server, 'import_history_for_type', { type: 'ORDER', page: 1, size: 5 });
  const body = JSON.parse(res.content[0].text);

  assert.equal(body.type, 'ORDER');
  assert.equal(body.currentPage, 1);
  assert.equal(body.totalElements, 6);
  assert.equal(body.jobs.length, 1);
});

test('import_history_for_type requires a type', () => {
  const server = buildServer(async () => envelope({}));
  assert.equal(parseArgs(server, 'import_history_for_type', {}).success, false);
});

// ── 11–15. Backend error mapping (safe, secret-free messages) ─────────

const ERROR_CASES: Array<{ label: string; error: AxiosError; needle: string }> = [
  { label: '401', error: makeAxiosError(401, { message: 'bad key' }), needle: 'access denied' },
  { label: '403', error: makeAxiosError(403, { message: 'forbidden' }), needle: 'access denied' },
  { label: '404', error: makeAxiosError(404, { message: 'missing' }), needle: 'was not found' },
  { label: '429', error: makeAxiosError(429), needle: 'rate limit exceeded' },
  { label: '500', error: makeAxiosError(500), needle: 'internal error' },
  { label: 'network-unreachable', error: makeAxiosError(undefined), needle: 'unable to reach the commerce insight backend' },
];

for (const { label, error, needle } of ERROR_CASES) {
  test(`import_job_lookup maps backend ${label} to a safe message`, async () => {
    const server = buildServer(async () => {
      throw error;
    });
    const res = await invoke(server, 'import_job_lookup', { jobId: JOB_ID });
    const text: string = res.content[0].text;

    assert.equal(res.isError, true);
    assert.ok(
      text.toLowerCase().includes(needle),
      `expected "${needle}" in error text but got: ${text}`,
    );
    // No secret / internal leakage.
    assert.doesNotMatch(text, /stack|x-mcp-api-key|authorization|bearer|jwt|password|select \s|postgres|jdbc/i);
  });
}

test('import_job_search maps a backend 500 to a safe message', async () => {
  const server = buildServer(async () => {
    throw makeAxiosError(500);
  });
  const res = await invoke(server, 'import_job_search', {});
  assert.equal(res.isError, true);
  assert.match(res.content[0].text.toLowerCase(), /internal error/);
});
