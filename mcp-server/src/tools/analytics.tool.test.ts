/**
 * Unit tests for the AI insights MCP tool (analytics_ai_insights) added to
 * AnalyticsToolsProvider.
 *
 * The Axios apiClient is fully stubbed — these tests NEVER reach the backend, a
 * database, or any LLM provider.
 *
 * Run with:  npm test   (node:test via tsx, no build step)
 */

import { test } from 'node:test';
import assert from 'node:assert/strict';
import { z } from 'zod';
import { AxiosError } from 'axios';
import { AnalyticsToolsProvider } from './analytics.tool.js';

// ── Test doubles ────────────────────────────────────────────────────────────

type Handler = (args: any) => Promise<any> | any;

interface Registered {
  description: string;
  shape: Record<string, z.ZodTypeAny>;
  handler: Handler;
}

class FakeServer {
  public readonly tools = new Map<string, Registered>();
  public tool(name: string, description: string, shape: Record<string, z.ZodTypeAny>, handler: Handler): void {
    this.tools.set(name, { description, shape, handler });
  }
}

type PostImpl = (url: string, body?: any, opts?: any) => Promise<any>;

function buildServer(postImpl: PostImpl): FakeServer {
  const server = new FakeServer();
  const apiClient = {
    get: async () => { throw new Error('get() must not be called by analytics_ai_insights'); },
    post: postImpl,
  } as any;
  new AnalyticsToolsProvider().register(server as any, apiClient);
  return server;
}

async function invoke(server: FakeServer, tool: string, input: unknown) {
  const entry = server.tools.get(tool);
  assert.ok(entry, `tool "${tool}" should be registered`);
  const parsed = z.object(entry!.shape).parse(input);
  return entry!.handler(parsed);
}

/** Raw-AxiosResponse-shaped envelope, matching how the tools read response.data.data. */
const envelope = (data: unknown) => ({
  data: { success: true, data, message: 'ok', timestamp: '2026-09-01T00:00:00Z' },
});

function makeAxiosError(status: number | undefined): AxiosError {
  const cfg = { headers: {} } as any;
  const err = new AxiosError('request failed', 'ERR', cfg,
    {}, status === undefined ? undefined : ({ status, data: { error: { message: 'x' } }, headers: {}, config: cfg, statusText: '' } as any));
  return err;
}

const OK_PAYLOAD = {
  available: true,
  summary: 'Revenue grew over the window based on the supplied aggregates.',
  insights: [{ type: 'TREND', title: 'Revenue trend', description: 'Monthly points provided.', metric: '', severity: 'LOW' }],
  recommendations: [{ title: 'Watch stock', description: 'Low-stock items present.', priority: 'MEDIUM' }],
  generatedAt: '2026-09-01T00:00:00Z',
  provider: 'openai',
  model: 'gpt-4o-mini',
};

// ── Tests ──────────────────────────────────────────────────────────────────

test('analytics_ai_insights is registered', () => {
  const server = buildServer(async () => envelope(OK_PAYLOAD));
  assert.ok(server.tools.has('analytics_ai_insights'));
});

test('valid request → safe structured output, POSTs normalized instants', async () => {
  let sentUrl = '';
  let sentBody: any = null;
  const server = buildServer(async (url, body) => { sentUrl = url; sentBody = body; return envelope(OK_PAYLOAD); });

  const res = await invoke(server, 'analytics_ai_insights', { dateFrom: '2026-01-01', dateTo: '2026-06-30' });

  assert.equal(sentUrl, '/analytics/ai-insights');
  assert.equal(sentBody.dateFrom, '2026-01-01T00:00:00Z');
  assert.equal(sentBody.dateTo, '2026-06-30T23:59:59Z');
  assert.equal(res.isError, undefined);
  const out = JSON.parse(res.content[0].text);
  assert.equal(out.available, true);
  assert.equal(out.insights.length, 1);
  assert.equal(out.recommendations.length, 1);
  // Only the safe fields are surfaced.
  assert.deepEqual(Object.keys(out).sort(), ['available', 'generatedAt', 'insights', 'recommendations', 'summary']);
});

test('available:false is passed through unchanged (feature disabled path)', async () => {
  const server = buildServer(async () => envelope({
    available: false, summary: 'AI insights are temporarily unavailable.', insights: [], recommendations: [], generatedAt: '2026-09-01T00:00:00Z',
  }));
  const res = await invoke(server, 'analytics_ai_insights', { dateFrom: '2026-01-01', dateTo: '2026-02-01' });
  assert.equal(res.isError, undefined);
  const out = JSON.parse(res.content[0].text);
  assert.equal(out.available, false);
  assert.deepEqual(out.insights, []);
});

test('malformed date → validation error, no backend call', async () => {
  const server = buildServer(async () => { throw new Error('must not POST'); });
  const res = await invoke(server, 'analytics_ai_insights', { dateFrom: 'yesterday', dateTo: '2026-02-01' });
  assert.equal(res.isError, true);
  assert.match(res.content[0].text, /YYYY-MM-DD|ISO 8601/);
});

test('dateFrom on/after dateTo → error, no backend call', async () => {
  const server = buildServer(async () => { throw new Error('must not POST'); });
  const res = await invoke(server, 'analytics_ai_insights', { dateFrom: '2026-06-01', dateTo: '2026-01-01' });
  assert.equal(res.isError, true);
  assert.match(res.content[0].text, /before dateTo/);
});

test('backend 403 → safe access-denied message, no leak', async () => {
  const server = buildServer(async () => { throw makeAxiosError(403); });
  const res = await invoke(server, 'analytics_ai_insights', { dateFrom: '2026-01-01', dateTo: '2026-02-01' });
  assert.equal(res.isError, true);
  assert.match(res.content[0].text, /Access denied/i);
});

test('backend 429 → safe rate-limit message', async () => {
  const server = buildServer(async () => { throw makeAxiosError(429); });
  const res = await invoke(server, 'analytics_ai_insights', { dateFrom: '2026-01-01', dateTo: '2026-02-01' });
  assert.equal(res.isError, true);
  assert.match(res.content[0].text, /Rate limit/i);
});

test('backend 500 → safe internal-error message', async () => {
  const server = buildServer(async () => { throw makeAxiosError(500); });
  const res = await invoke(server, 'analytics_ai_insights', { dateFrom: '2026-01-01', dateTo: '2026-02-01' });
  assert.equal(res.isError, true);
  assert.match(res.content[0].text, /internal error/i);
});

test('missing data node → error response', async () => {
  const server = buildServer(async () => ({ data: { success: true, message: 'ok' } }));
  const res = await invoke(server, 'analytics_ai_insights', { dateFrom: '2026-01-01', dateTo: '2026-02-01' });
  assert.equal(res.isError, true);
});

test('output never contains provider secrets / auth material', async () => {
  const server = buildServer(async () => envelope({
    ...OK_PAYLOAD,
    // even if the backend were to leak these, the tool must not surface them
    apiKey: 'sk-should-not-appear',
    authorization: 'Bearer nope',
  }));
  const res = await invoke(server, 'analytics_ai_insights', { dateFrom: '2026-01-01', dateTo: '2026-02-01' });
  const text = res.content[0].text;
  assert.doesNotMatch(text, /sk-should-not-appear/);
  assert.doesNotMatch(text, /Bearer nope/);
  assert.doesNotMatch(text, /apiKey|authorization/i);
});
