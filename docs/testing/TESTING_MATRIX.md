# Testing Matrix — Commerce Insight AI

Living inventory of the automated test layers. Numbers are **measured** on
Sprint 15 (see `SPRINT_15_TESTING.md` for the run log). Coverage percentages are
JaCoCo line coverage, not statement or branch.

| Layer | Framework | Scope | Location | Count | Status |
|---|---|---|---|---:|---|
| Backend unit | JUnit 5 + Mockito | Services, providers, validators, mappers-by-intent | `backend/src/test/**` (`*Test.java`, no `@SpringBootTest`) | — | ✅ |
| Backend integration | Spring Boot Test + MockMvc + real PostgreSQL | REST contracts, security envelopes, auth/RBAC, actuator, DB round-trips | `backend/src/test/**` (`*IntegrationTest.java`) | — | ✅ |
| Backend total | `./mvnw -o test` | everything above | `backend/` | **509** | ✅ 0 fail / 0 err / 1 opt-in skip |
| Frontend unit | Vitest + Testing Library | components, hooks, `lib/apiError`, `authTokens`, router guards, XSS rendering | `frontend/src/**/*.{test,spec}.{ts,tsx}` | **48** | ✅ 0 fail |
| MCP | `node --test` (via `tsx`) | MCP tool providers (REST-only), input validation, error normalisation, no-secret-leak | `mcp-server/src/**/*.test.ts` | **59** | ✅ 0 fail |
| E2E — security | Playwright (Chromium) | 9.1 login · 9.2 protected routes · 9.3 RBAC · 9.4 403 · 9.5 401→refresh · 9.6 concurrent 401 · 9.7 refresh failure · 9.8 429 · 9.9 import UX · 9.10 order UX | `frontend/e2e/{auth,rbac,token-refresh,rate-limit-and-features}.spec.ts` | **25** | ✅ 25/25 (e2e stack) |
| E2E — dashboard/AI | Playwright (Chromium) | dashboard KPIs + charts, degraded analytics section, breadcrumb, lazy-route guard, AI generate/loading/success/unavailable/403/429 | `frontend/e2e/dashboard.spec.ts` | **9** | ✅ 9/9 (demo stack) / 7/9 (e2e stack — 2 need the 600-order demo dataset) |
| Security scan | `scripts/security-check.mjs` (Node) | static secret / token / unsafe-HTML / PII-leak scan across `backend/src`, `frontend/src`, `frontend/e2e`, `mcp-server/src` | repo root | — | ✅ 0 errors / 0 warnings |
| Backend coverage | JaCoCo 0.8.12 (`jacoco-maven-plugin`) | line coverage report (no build gate) | `backend/target/site/jacoco/` | — | 75.8% overall / 81.5% excl. generated mappers |
| Performance | `curl` timing + `EXPLAIN ANALYZE` (ad-hoc, demo stack) | analytics endpoint latency, query plans, export generation, import job creation | `docs/performance/SPRINT_15_PERFORMANCE.md` | — | ✅ analytics < 30 ms; export orders XLSX ~1.05 s |
| Build | `./mvnw -o package` · `npm run build` (×2) | production artefacts | CI + local | — | ✅ |
| Docker | `docker compose … config` + `up -d` | demo + e2e stacks, 4 services each | `docker-compose*.yml` | — | ✅ 4/4 healthy |
| CI | GitHub Actions | backend · frontend · mcp · security · e2e (+ jacoco artefact) | `.github/workflows/ci.yml` | 5 jobs | ✅ all stages present, no AI key required |

## Notable non-goals / deliberate gaps

- **JaCoCo has no minimum-coverage gate.** Report only — a consolidation sprint
  should not fail CI on an arbitrary threshold.
- **`RealProviderManualTest`** (backend) is `@EnabledIfEnvironmentVariable(AI_REAL_PROVIDER_TEST=true)`
  — the "1 skipped" in the backend suite. It never runs in CI and needs a real
  OpenAI/Anthropic key.
- **Generated MapStruct `*MapperImpl` classes** drag overall coverage down
  (~0–2% each, 0 hand-written logic). The "excl. generated mappers" figure is the
  meaningful one.
- **Import bulk-throughput** was not benchmarked this sprint (ad-hoc CSV fixtures
  failed parser validation); import correctness is covered by the
  `*ImportService` test suite (73–84% line coverage).
