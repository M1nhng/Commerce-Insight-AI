# Sprint AI Analytics 2 — Production AI Hardening & Provider Expansion

Status legend: **PASS** = executed & verified · **NOT RUN** = not executed ·
**PRE-EXISTING** = present before this sprint · **OPT-IN** = runs only on request.

> **AI is optional.** The dashboard, analytics, MCP, authentication and security
> all continue functioning when every AI provider is unavailable. No AI provider
> is a single point of failure.

---

## 1. What this sprint added (on top of Sprint AI Analytics 1)

| Area | Change |
|---|---|
| Provider abstraction | `LlmProvider.supports(id)` (alias-aware selection); `LlmFailureReason` taxonomy carried on `LlmException`; shared `LlmHttpSupport` for status→reason mapping |
| Providers | **`AnthropicLlmProvider`** (native Messages API, JDK HTTP client, no SDK); `OpenAiCompatibleLlmProvider` now explicitly serves `openai` / `ollama` / `openai-compatible` aliases; `connect-timeout-ms` honoured |
| Selection | `LlmClient` resolves by `supports()`; unknown provider → unavailable, `complete()` → `LlmException(NOT_CONFIGURED)`; startup never fails |
| Observability | `AiMetrics` over the existing Micrometer `MeterRegistry` (Actuator, no new dep) + structured safe logging in `AiAnalyticsService` |
| Config | `app.ai.{connect-timeout-ms, anthropic-base-url, anthropic-version}`; `api-key` resolves `AI_API_KEY` → `OPENAI_API_KEY` → `ANTHROPIC_API_KEY` |
| Testing | local-HTTP-server provider tests (both providers, all failure modes), `LlmClientTest`, opt-in `RealProviderManualTest` |
| Frontend | "Generated just now" for a fresh result (cosmetic) |

Nothing in `AiAnalyticsService`'s dependencies changed except adding `AiMetrics`;
it still depends only on `LlmClient` for provider access — never on a concrete
provider class.

---

## 2. Architecture

```
AiAnalyticsController  @PreAuthorize("isAuthenticated()")  + RateLimitingFilter[AI_INSIGHTS]
        │
        ▼
AiAnalyticsService  ── AiMetrics (Micrometer) + safe structured logs
        │  validate range → 400 · feature off → 200 available:false
        ▼
   LlmClient   ── resolve by app.ai.provider via LlmProvider.supports()
        │
   ┌────┴───────────────┐
   ▼                    ▼
OpenAiCompatibleLlmProvider     AnthropicLlmProvider
 (openai · ollama · gateway)     (claude, native /v1/messages)
   │  JDK HttpClient, 1 call, bounded timeout, NO retry
   ▼
LlmException(LlmFailureReason)  → AiAnalyticsService → available:false (HTTP 200)
```

`AiAnalyticsService` → `LlmClient` → `LlmProvider` is the only path. Never
`Frontend/MCP/Controller/Repository → OpenAI/Anthropic`, never `MCP → DB`.

---

## 3. Provider abstraction

```java
public interface LlmProvider {
    String  id();                                  // "openai" | "anthropic"
    default boolean supports(String providerId);   // default: equals id(); overridden for aliases
    boolean isConfigured();                         // has everything it needs to call
    LlmCompletion complete(LlmRequest request);     // throws LlmException(LlmFailureReason)
}
```

* **`OpenAiCompatibleLlmProvider.supports()`** → `{openai, ollama, openai-compatible,
  compatible, custom}`.
* **`AnthropicLlmProvider.supports()`** → `{anthropic}`.
* `LlmClient.resolve()` picks the first provider whose `supports(app.ai.provider)`
  is true; if exactly one provider bean exists it is used regardless (dev
  convenience). No match + multiple beans → `Optional.empty()` →
  `isAvailable()==false`, `complete()` throws `LlmException(NOT_CONFIGURED)`.
* Provider-specific wire format (headers, body shape, JSON extraction) stays
  entirely inside each provider. Only status→reason lives in `LlmHttpSupport`.

`LlmFailureReason`: `NOT_CONFIGURED, TIMEOUT, UNAUTHORIZED, RATE_LIMITED,
PROVIDER_ERROR, INVALID_RESPONSE, NETWORK_ERROR`. It is used only for logs +
metrics tags — the API consumer never sees it (always `available:false`, HTTP 200).

---

## 4. Supported providers

| `app.ai.provider` | Served by | Key | Endpoint |
|---|---|---|---|
| `openai` | `OpenAiCompatibleLlmProvider` | `OPENAI_API_KEY` (Bearer) | `{base-url}/chat/completions` |
| `ollama` | `OpenAiCompatibleLlmProvider` | none | `{base-url}/chat/completions` (`http://localhost:11434/v1`) |
| _any OpenAI-compatible gateway_ | `OpenAiCompatibleLlmProvider` | gateway key or none | `{base-url}/chat/completions` |
| `anthropic` | `AnthropicLlmProvider` | `ANTHROPIC_API_KEY` (`x-api-key`) | `{anthropic-base-url}/v1/messages` |
| anything else (`gemini`, …) | — | — | feature reports **unavailable**, app still starts |

Gemini is **not** implemented this sprint (no compatible impl existed). The
`GEMINI_API_KEY` placeholder in `.env.example` is reserved for a future
`LlmProvider`.

---

## 5. Configuration (`app.ai.*`)

| Key | Env | Default | Notes |
|---|---|---|---|
| `enabled` | `AI_INSIGHTS_ENABLED` | `false` | master switch |
| `provider` | `AI_PROVIDER` | `openai` | `openai` / `ollama` / `anthropic` / gateway |
| `model` | `AI_MODEL` | `gpt-4o-mini` | passed verbatim |
| `api-key` | `AI_API_KEY` → `OPENAI_API_KEY` → `ANTHROPIC_API_KEY` | *(empty)* | never logged; empty ok for Ollama |
| `base-url` | `AI_BASE_URL` | `https://api.openai.com/v1` | OpenAI-compatible chat URL |
| `anthropic-base-url` | `ANTHROPIC_BASE_URL` | `https://api.anthropic.com` | Anthropic Messages base |
| `anthropic-version` | `ANTHROPIC_VERSION` | `2023-06-01` | Anthropic API version header |
| `timeout-ms` | `AI_TIMEOUT_MS` | `20000` | whole-call request timeout |
| `connect-timeout-ms` | `AI_CONNECT_TIMEOUT_MS` | `5000` | TCP connect timeout |
| `max-output-tokens` | `AI_MAX_OUTPUT_TOKENS` | `900` | cost cap |
| `temperature` | `AI_TEMPERATURE` | `0.2` | |
| `max-range-days` | — | `365` | request validation → 400 |
| `max-context-chars` | — | `12000` | context-size backstop |

No secret has a committed default. `SecretsValidator` is unchanged — a missing
AI key never blocks startup.

### OpenAI setup
```
AI_INSIGHTS_ENABLED=true
AI_PROVIDER=openai
AI_MODEL=gpt-4o-mini
OPENAI_API_KEY=sk-...
```

### Ollama setup (local, keyless)
```
AI_INSIGHTS_ENABLED=true
AI_PROVIDER=ollama
AI_MODEL=llama3.1
AI_BASE_URL=http://localhost:11434/v1
AI_API_KEY=            # empty — no Authorization header is sent
```

### Anthropic setup
```
AI_INSIGHTS_ENABLED=true
AI_PROVIDER=anthropic
AI_MODEL=claude-3-5-haiku-latest
ANTHROPIC_API_KEY=sk-ant-...
# ANTHROPIC_BASE_URL / ANTHROPIC_VERSION optional
```
The Anthropic provider sends the shared system prompt as the top-level
`system` field and pre-fills the assistant turn with `{` so the reply is a bare
JSON object (no "Here is the JSON:" preamble); the brace is re-attached before
parsing.

---

## 6. Timeout & retry policy

* Connect timeout `app.ai.connect-timeout-ms` (default 5 s); request timeout
  `app.ai.timeout-ms` (default 20 s, sprint-recommended range up to 30 s).
* **No automatic retries** anywhere: provider = 0, `LlmClient` = 0, frontend
  mutation = 0 (`useGenerateAiInsights` `retry: 0`), TanStack Query mutation
  default `retry: 0`. AI generation is already rate-limited; retrying multiplies
  cost + latency.

---

## 7. Rate limiting

Unchanged from Sprint AI Analytics 1: the existing in-memory bucket4j
`RateLimitingFilter` `AI_INSIGHTS` group, keyed by authenticated principal,
**10 requests / 3600 s** (`app.rate-limit.ai-insights`; 20/h in the demo
profile). Exceeded → HTTP 429 + `Retry-After` + `RATE_LIMIT_EXCEEDED`. No second
rate limiter, no Redis.

---

## 8. Error handling

| Situation | HTTP | Body |
|---|---|---|
| Provider timeout / 5xx / 401 / 429 / bad JSON / empty / unreachable / unknown provider | **200** | `{"available":false,"summary":"AI insights are temporarily unavailable.","insights":[],"recommendations":[]}` |
| `dateFrom ≥ dateTo` / missing bound / range > `max-range-days` | **400** | `VALIDATION_ERROR` envelope |
| No token | **401** | `AUTHENTICATION_REQUIRED` |
| Rate limit | **429** | `RATE_LIMIT_EXCEEDED` + `Retry-After` |

Never exposed to any consumer: stack traces, API keys, `Authorization` /
`x-api-key` headers, JWTs, provider response bodies, SQL, internal class names.
`LlmException` messages are generic (`"AI provider returned HTTP 500"`,
`"AI provider timed out"`); response bodies are discarded on non-2xx.

---

## 9. Observability

`AiMetrics` uses the Micrometer `MeterRegistry` that Spring Boot Actuator already
provides (exposed at `/actuator/metrics`). No new dependency, no Prometheus stack.

| Meter | Type | Tags |
|---|---|---|
| `ai.insights.requests` | counter | — |
| `ai.insights.success` | counter | `provider`, `model` |
| `ai.insights.unavailable` | counter | `provider`, `model` |
| `ai.insights.validation_failures` | counter | — |
| `ai.insights.provider_failures` | counter | `provider`, `model`, `reason` |
| `ai.insights.latency` | timer | `provider`, `model`, `result` |

Tags are strictly low-cardinality and non-sensitive: `provider` ∈
{`openai`,`anthropic`,`none`,`unknown`}, `model` is a config constant, `result` ∈
{`success`,`provider_failure`}, `reason` ∈ `LlmFailureReason` names. **Never
tagged**: user id, email, customer id, IP, prompt, request/response body.

**Structured logs** (INFO/WARN, no sensitive data):
```
AI insights request started   provider=openai model=gpt-4o-mini rangeDays=30
AI insights request completed provider=openai model=gpt-4o-mini result=success latencyMs=812
AI insights provider failure  provider=anthropic model=claude-3-5-haiku-latest reason=TIMEOUT latencyMs=20001
```
Never logged: prompt, analytics context, API key, `Authorization`, JWT, customer
data, raw provider response.

---

## 10. Security

* `node scripts/security-check.mjs` → **0 errors / 0 warnings** (PASS).
* No provider key / MCP key / JWT secret in frontend, MCP, Git, logs, metrics
  tags, or API responses.
* Keys are read only by the backend (`AiProperties`). Frontend and MCP never
  receive them.
* Prompt-injection defence unchanged (Sprint 1): `AiAnalyticsPromptBuilder`
  marks the analytics context untrusted; product / customer / SKU strings can
  never act as instructions (tests in `AiAnalyticsPromptBuilderTest`).
* AI context is aggregates only — no PII (tests in
  `AiAnalyticsContextBuilderTest`).
* MCP remains REST-only: no LLM SDK, no DB driver, no key; `analytics_ai_insights`
  returns the safe subset `{available, summary, insights, recommendations,
  generatedAt}` — never `provider`/`model`/raw error.

---

## 11. Testing

| Suite | Command | Result |
|---|---|---|
| Backend full | `./mvnw -o test` | **494 run · 0 failures · 0 errors · 1 skipped** — PASS |
| ↳ `OpenAiCompatibleLlmProviderTest` (local HTTP server) | `-Dtest=OpenAiCompatibleLlmProviderTest` | 10 / 0 / 0 |
| ↳ `AnthropicLlmProviderTest` (local HTTP server) | `-Dtest=AnthropicLlmProviderTest` | 10 / 0 / 0 |
| ↳ `LlmClientTest` | `-Dtest=LlmClientTest` | 7 / 0 / 0 |
| ↳ `AiAnalyticsServiceTest` (+ metrics assertions) | `-Dtest=AiAnalyticsServiceTest` | 9 / 0 / 0 |
| ↳ `AiAnalyticsControllerIntegrationTest` (stub provider, real PG) | | 8 / 0 / 0 |
| ↳ `RealProviderManualTest` | | **1 skipped** (opt-in, §12) |
| MCP | `npm test` | **59 / 59** — PASS |
| Frontend unit | `npx vitest run` | **47 / 47** — PASS |
| Frontend types / lint / build | `tsc --noEmit` / `npm run lint` / `npm run build` | 0 / exit 0 (17 pre-existing warnings) / OK — PASS |
| Security | `node scripts/security-check.mjs` | **0 / 0** — PASS |

The `1 skipped` is `RealProviderManualTest` — deliberately gated
(`@EnabledIfEnvironmentVariable`), never run in CI.

The provider tests use a JDK `com.sun.net.httpserver.HttpServer` (`MockLlmHttpServer`)
— no external API. They assert: request path, `Authorization` (OpenAI) vs
`x-api-key`+`anthropic-version` (Anthropic), absence of `Authorization` for
keyless Ollama, body shape (system/user roles, `response_format` / `system`
field / assistant prefill), and every failure mapping (200-valid, malformed
JSON, empty content, 401, 429, 500, request timeout, connection refused).

---

## 12. Real-provider opt-in

`RealProviderManualTest` (`backend/src/test/.../llm/`) is annotated
`@EnabledIfEnvironmentVariable(named = "AI_REAL_PROVIDER_TEST", matches = "true")`
— **skipped by default and in CI**.

```
AI_REAL_PROVIDER_TEST=true \
AI_PROVIDER=openai AI_MODEL=gpt-4o-mini OPENAI_API_KEY=sk-... \
./mvnw -o test -Dtest=RealProviderManualTest
```
It makes exactly **one** real completion call and asserts a non-blank response.
It never prints the API key, the prompt, or the provider response. A
misconfiguration fails with a generic assertion message.

---

## 13. Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| Card always shows "temporarily unavailable" | `AI_INSIGHTS_ENABLED` not `true`, or no key for the selected provider. Check `/actuator/metrics/ai.insights.unavailable`. |
| `available:false` right after enabling, provider=`gemini` | Unknown provider — not implemented. Use `openai` / `ollama` / `anthropic`. |
| Anthropic returns `INVALID_RESPONSE` | Wrong `ANTHROPIC_VERSION`, or a model that ignores the `{`-prefill. Try `claude-3-5-haiku-latest`. |
| Every call `TIMEOUT` | Raise `AI_TIMEOUT_MS` (Anthropic/large models can need 25–30 s); check `AI_CONNECT_TIMEOUT_MS` and network egress. |
| Ollama `UNAUTHORIZED` | You set a key — leave `AI_API_KEY` empty for `provider=ollama`. |
| 429 from `/analytics/ai-insights` | Per-principal rate limit (10/h). Raise `app.rate-limit.ai-insights.capacity` if intended. |

---

## 14. Production recommendations

* Keep `AI_INSIGHTS_ENABLED=false` until a provider + key are provisioned; the
  dashboard is fully functional without it.
* Set `AI_API_KEY` (or the provider-specific key) via the platform's secret
  store — never in `.env` committed to Git, never in a `VITE_` var.
* `AI_TIMEOUT_MS` 20 000–30 000; `AI_CONNECT_TIMEOUT_MS` 5 000. Do not add retries.
* Watch `ai.insights.provider_failures{reason}` and `ai.insights.latency` in your
  metrics backend; alert on a sustained rise in `TIMEOUT` / `PROVIDER_ERROR`.
* Restrict `/actuator/metrics` (already `when-authorized` for health; metrics
  exposure should sit behind the gateway in prod).
* A provider outage is non-fatal by design — no runbook action beyond noting it.

---

## 15. Known limitations

* Only OpenAI-compatible + Anthropic providers ship. Gemini / Vertex / Bedrock
  are future `LlmProvider` implementations.
* No live LLM call was made in CI (no key available) — the provider HTTP path is
  covered by the local-mock-server tests and the opt-in `RealProviderManualTest`.
* The Anthropic `{`-prefill JSON coercion depends on the model honouring assistant
  prefill; the response parser still degrades safely if it does not.
* `frontend/.env.example`'s `VITE_FEATURE_AI_INSIGHTS` flag is still unused (the
  card always renders and degrades gracefully) — left as-is (pre-existing).
