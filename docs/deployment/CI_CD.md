# CI / CD — Commerce Insight AI

Authoritative description of the GitHub Actions workflows. Current as of Sprint 16.
Historical rationale is in [`SPRINT_14_CICD.md`](SPRINT_14_CICD.md).

There are three workflows under [`.github/workflows/`](../../.github/workflows/):

| File | Trigger | Blocking? | Purpose |
|---|---|---|---|
| `ci.yml` | push to `main` / `develop` / `sprint*`, PRs to `main` / `develop` | **Yes** | Build + test every component + security scan + full E2E |
| `cd.yml` | after `CI` succeeds on `main`; or manual dispatch | No (build always; deploy self-skips) | Build & push production images to GHCR; guarded deploy skeleton |
| `dependency-audit.yml` | weekly (Mon 06:00 UTC); manual dispatch | No | `npm audit` + OWASP Dependency-Check, advisory only |

**No workflow references a repository secret for CI.** `cd.yml` uses only the
built-in `GITHUB_TOKEN` (for GHCR) unless deployment secrets are explicitly added.
No secret value is ever hard-coded or echoed.

---

## 1. `ci.yml` — verification pipeline

```yaml
on:
  push:        [main, develop, "sprint*"]
  pull_request: [main, develop]
concurrency: ci-<ref>  (cancel-in-progress)
permissions: contents: read
env: NODE_VERSION=22, JAVA_VERSION=17
```

### Jobs

| Job | Runner | Steps | Notes |
|---|---|---|---|
| **backend** | `ubuntu-latest` + `postgres:16-alpine` service (`commerce_insight` / `postgres` / `postgres`, health-gated) | `chmod +x mvnw`; `./mvnw -B -ntp clean verify` | Runs the full 509-test JUnit/Spring suite against the service Postgres (matches `application-test.yml`). Uploads `backend-surefire-reports` and `backend-jacoco-coverage` (`if: always()`, report only — **no coverage gate**). |
| **frontend** | `ubuntu-latest` | `npm ci`; `npx tsc --noEmit`; `npm test` (Vitest); `npm run lint`; `npm run build` (`VITE_API_BASE_URL=http://localhost:8080`) | `npm run lint` currently passes with 17 documented advisory warnings; the build fails on any chunk `> 500 kB` (none today). |
| **mcp** | `ubuntu-latest`, `env: MCP_API_KEY=ci-mcp-test-key-not-a-real-secret`, `NODE_ENV=test` | `npm ci`; `npm run type-check`; `npm test` (`node --test`); `npm run build` | `MCP_API_KEY` is a throwaway required by `src/config` at import time; it never reaches a real backend in this job. Node 22 because the test script uses `node --test` with a glob. |
| **security** | `ubuntu-latest` | `node scripts/security-check.mjs` | Static secret / token / unsafe-HTML / PII-leak scan over `backend/src`, `frontend/src`, `frontend/e2e`, `mcp-server/src`. **Fails the pipeline on any ERROR finding.** Current: 0 errors / 0 warnings. |
| **e2e** | `ubuntu-latest`, `needs: [backend, frontend, mcp, security]` | `npm ci`; `npx playwright install --with-deps chromium`; `docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d --build`; wait for backend + frontend health; `npm run test:e2e` (`E2E_BASE_URL=http://localhost:5173`); upload `playwright-report`; dump stack logs on failure; `down -v` | Runs against the **`e2e`** stack (thin fixed-user seed). This is the 25-test security suite. The 9-test dashboard/AI suite needs the `demo` dataset and is run separately, not in CI. |

### What CI deliberately does **not** do

- **No AI provider key.** `AI_REAL_PROVIDER_TEST` is never set; the backend's
  `RealProviderManualTest` stays skipped. Every LLM path in CI is exercised with
  in-process fakes / a local mock HTTP server.
- **No dependency-vulnerability gate.** OWASP / `npm audit` run in the separate
  weekly `dependency-audit.yml`; their findings are advisory (tracked as accepted
  risks in `docs/SPRINT_13A_PRODUCTION_READINESS.md`).
- **No deploy.** CI is verification only.

---

## 2. `cd.yml` — image build & guarded deploy

```yaml
on:
  workflow_run: { workflows: ["CI"], types: [completed], branches: [main] }
  workflow_dispatch: { inputs: reason }
concurrency: cd-production  (no cancel)
permissions: contents: read, packages: write
env: REGISTRY=ghcr.io
```

### Jobs

| Job | Gate | Does |
|---|---|---|
| **guard** | `workflow_dispatch` **or** triggering CI run `conclusion == success` | Echoes the trigger + head SHA. Fails closed if CI did not pass. |
| **build-images** | `needs: guard` | Matrix over `backend` / `frontend` / `mcp-server`. Logs in to GHCR with `GITHUB_TOKEN`, builds each context, pushes `:latest` and `:<sha>` to `ghcr.io/m1nhng/commerce-insight-ai/<name>`. Frontend build-arg `VITE_API_BASE_URL` comes from repo/environment variable `vars.VITE_API_BASE_URL` (placeholder `https://api.example.com` if unset). GHA layer cache. |
| **deploy** | `needs: build-images`, `environment: production` | **Guarded skeleton.** Checks whether `secrets.DEPLOY_SSH_HOST` is set. If **not** (current state): prints a `::notice::` explaining that images were pushed and how to enable delivery, then succeeds without doing anything. If set: checks out the SHA, SSHes to the host (`appleboy/ssh-action`), `docker compose pull && up -d --remove-orphans && image prune`, then prints a manual smoke-check reminder. |

### Deployment status

**There is no live production environment.** `build-images` publishes images to
GHCR on every green `main`; `deploy` self-skips. To activate real delivery:

1. Provision a host with Docker + Compose, `docker-compose.yml` + a production
   overlay, and a `.env` carrying `SPRING_PROFILES_ACTIVE=prod`,
   `SPRING_DATASOURCE_URL`, `JWT_SECRET` (≥ 64 chars), `MCP_API_KEY`,
   `CORS_ALLOWED_ORIGINS` (see `application-prod.yml`).
2. Add repository secrets `DEPLOY_SSH_HOST`, `DEPLOY_SSH_USER`, `DEPLOY_SSH_KEY`,
   `DEPLOY_PATH`.
3. Add required reviewers / branch protection to the `production` environment.

Production safety already baked in: `prod` profile only (never `demo`), the demo
seed (`db/demo/**`) is not on the prod Flyway path, `SecretsValidator` refuses a
`prod` boot carrying a dev/demo secret, Swagger is disabled in `prod`, and
`/actuator` exposes only `health` / `info` publicly (`metrics` is `ROLE_ADMIN`).

---

## 3. `dependency-audit.yml` — advisory

```yaml
on:
  schedule: [ "0 6 * * 1" ]   # Mondays 06:00 UTC
  workflow_dispatch:
permissions: contents: read
```

| Job | Does |
|---|---|
| **npm-audit** | `npm audit --omit=dev` and `npm audit` for `frontend/`, `npm audit` for `mcp-server/`. All `|| true` — never fails. |
| **owasp** | `continue-on-error: true`. `./mvnw -Powasp -DskipTests verify` for the backend, using `secrets.NVD_API_KEY` if configured (avoids NVD rate limits). Uploads `owasp-dependency-check` report (30-day retention). |

Kept out of `ci.yml` because OWASP downloads a multi-hundred-MB NVD dataset and
the NVD API rate-limits keyless callers — that belongs on a schedule, not a PR.

---

## 4. Running the equivalents locally

| CI job | Local command |
|---|---|
| backend | `cd backend && ./mvnw -B -ntp clean verify` (needs a Postgres at `localhost:5432/commerce_insight`; `docker compose up -d postgres`) |
| frontend | `cd frontend && npm ci && npx tsc --noEmit && npm test && npm run lint && npm run build` |
| mcp | `cd mcp-server && npm ci && npm run type-check && npm test && npm run build` |
| security | `node scripts/security-check.mjs` |
| e2e | `docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d --build` then `cd frontend && npm run test:e2e` |

`scripts/verify.sh` chains the fast checks.

---

## 5. Artifacts produced by CI

| Artifact | From | Retention |
|---|---|---|
| `backend-surefire-reports` | backend job | 7 days |
| `backend-jacoco-coverage` (`jacoco.exec` + HTML report) | backend job | 7 days |
| `playwright-report` (+ `test-results/`) | e2e job | 7 days |
| `owasp-dependency-check` | dependency-audit (weekly) | 30 days |
| Container images `ghcr.io/m1nhng/commerce-insight-ai/{backend,frontend,mcp-server}:{latest,<sha>}` | cd build-images (on green `main`) | GHCR retention policy |
