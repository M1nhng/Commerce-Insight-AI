# Sprint 14 — CI/CD

## 1. Problem

`.github/workflows/cd.yml` had been an `echo "TODO: Implement deployment steps."`
placeholder since Sprint 0. CI itself (`ci.yml`) was already a complete, real
pipeline from Sprint 13A and needed no change.

## 2. What exists (unchanged)

### `ci.yml` — verification, on every push to `main`/`develop`/`sprint*` and PRs
| Job | Does |
|---|---|
| `backend` | `./mvnw -B -ntp clean verify` against a `postgres:16-alpine` service DB (`commerce_insight`, matches `application-test.yml`) |
| `frontend` | `npm ci` → `tsc --noEmit` → `npm test` → `npm run lint` → `npm run build` (`VITE_API_BASE_URL` a build-time public value) |
| `mcp` | `npm ci` → `npm run type-check` → `npm test` → `npm run build` (throwaway `MCP_API_KEY`) |
| `security` | `node scripts/security-check.mjs` |
| `e2e` | `docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d --build` + Playwright, then `down -v` |

`permissions: contents: read`. **No repository secrets referenced. Nothing echoed.**

### `dependency-audit.yml` — weekly, non-blocking
`npm audit` (frontend + mcp) and OWASP Dependency-Check (backend, needs
`NVD_API_KEY`). Advisory only.

## 3. What Sprint 14 added — `cd.yml` (rewritten)

```
on:
  workflow_run: { workflows: ["CI"], types: [completed], branches: [main] }
  workflow_dispatch: { inputs: { reason } }
permissions: { contents: read, packages: write }
concurrency: { group: cd-production, cancel-in-progress: false }
```

### Job `guard`
Runs only if `github.event_name == 'workflow_dispatch'` **or**
`github.event.workflow_run.conclusion == 'success'`. Prints the trigger and the
head SHA. Nothing deploys off a red CI run or a feature branch.

### Job `build-images` (`needs: guard`)
Matrix over `backend` / `frontend` / `mcp-server`:
* `docker/login-action@v3` → `ghcr.io` with `username: github.actor`,
  `password: secrets.GITHUB_TOKEN` (built-in, scoped by `packages: write`).
* `docker/build-push-action@v6` builds each context and pushes
  `ghcr.io/<owner>/<repo>/<service>:latest` and `:<head-sha>`
  (`<owner>/<repo>` lower-cased in a prior step).
* Frontend gets `build-args: VITE_API_BASE_URL=${{ vars.VITE_API_BASE_URL || 'https://api.example.com' }}`
  — a repo/environment **Variable** (public by design), never a secret.
* GHA layer cache (`cache-from/to: type=gha`).

**GHCR is the concrete, always-available deployment target** — it needs no
credential beyond `GITHUB_TOKEN`, so this job actually runs and produces
release artifacts on every green `main`.

### Job `deploy` (`needs: build-images`, `environment: production`)
A **guarded skeleton** — server delivery has no concrete host yet:
* Step `target` sets `configured=true|false` from whether `secrets.DEPLOY_SSH_HOST`
  is non-empty (secrets can't be read in a job-level `if:`).
* If `false` → prints an `::notice::` explaining images were published to GHCR
  and listing the secrets/inputs needed to enable delivery, then exits 0.
  **It does not claim a deployment happened.**
* If `true` → checks out the head SHA and runs `appleboy/ssh-action@v1` against
  `DEPLOY_SSH_HOST` / `DEPLOY_SSH_USER` / `DEPLOY_SSH_KEY`, `cd $DEPLOY_PATH`,
  `docker login ghcr.io` (with `GITHUB_TOKEN`), `docker compose pull`,
  `docker compose up -d --remove-orphans`, `docker image prune -f`. `IMAGE_TAG`
  is exported as the head SHA. A final step prints the health-check to verify
  externally.

## 4. Security properties

| Requirement | How it's met |
|---|---|
| Explicit trigger | `workflow_run` after CI + manual `workflow_dispatch` only |
| Deploy only `main` | `workflow_run.branches: [main]`; `guard` re-checks conclusion; `environment: production` (add branch restriction + reviewers in repo settings) |
| Never from a feature branch | trigger is branch-scoped to `main` |
| No hard-coded secrets | none present; `git grep` clean |
| GitHub Secrets for prod values | `DEPLOY_SSH_*`, `DEPLOY_PATH` via `secrets.*`; app secrets supplied by the host `.env` |
| No token leak | `GITHUB_TOKEN` piped via `--password-stdin`; no `echo` of any secret; `set -euo pipefail` in the remote script |
| Env protection | `environment: production` (configure required reviewers) |
| No demo DB in prod | `SPRING_PROFILES_ACTIVE=prod`; `db/demo/**` is not on the prod Flyway path; the demo overlay/compose is never referenced by `cd.yml` |
| Demo profile forbidden | `SPRING_PROFILES_ACTIVE=demo` appears nowhere in `cd.yml` |

## 5. Production runtime contract (`application-prod.yml`)

Backend refuses to start (`SecretsValidator`) if a committed dev secret or a
sub-256-bit `JWT_SECRET` is detected under the `prod` profile. Required env:

| Var | Purpose |
|---|---|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | DB connection |
| `JWT_SECRET` | ≥ 256-bit signing key (no default) |
| `MCP_API_KEY` | MCP shared secret (no default) |
| `CORS_ALLOWED_ORIGINS` | comma list of allowed SPA origins |
| `CORS_ALLOW_CREDENTIALS` (opt, default `false`) | |
| `TRUSTED_PROXIES` (opt) | CIDR list for `X-Forwarded-For` trust |
| `RATE_LIMIT_ENABLED` (opt, default `true`) | |
| `JWT_ACCESS_EXPIRATION_MS` / `JWT_REFRESH_EXPIRATION_DAYS` / `JWT_ISSUER` (opt) | |

Actuator is restricted to `health` (`show-details: never`); Swagger is disabled
in prod.

## 6. Enabling live deployment (checklist)

1. Stand up a host with Docker + Compose; place `docker-compose.yml` + a
   **production overlay** there (overlay: `image: ghcr.io/<owner>/<repo>/<svc>:${IMAGE_TAG:-latest}`,
   `SPRING_PROFILES_ACTIVE=prod`, real ports, restart policies) plus a `.env`
   with the §5 values.
2. Add repo secrets: `DEPLOY_SSH_HOST`, `DEPLOY_SSH_USER`, `DEPLOY_SSH_KEY`,
   `DEPLOY_PATH`.
3. Add repo/environment Variable `VITE_API_BASE_URL` = public API origin.
4. On the `production` environment: required reviewers + `main`-only.
5. Push to `main`; once CI is green, `CD` runs, publishes images, and (with the
   secrets set) rolls the stack forward.

## 7. Verification performed this sprint

| Check | Result |
|---|---|
| `cd.yml` / `ci.yml` / `dependency-audit.yml` YAML parse | **PASS** |
| `docker compose -f docker-compose.yml config` | **PASS** |
| `docker compose -f docker-compose.yml -f docker-compose.demo.yml config` | **PASS** |
| Frontend image builds with `VITE_API_BASE_URL` build-arg (via `docker compose ... up -d --build frontend`) | **PASS** — demo stack rebuilt & healthy |
| Backend image builds & boots (`prod`-style JAVA_OPTS) | **PASS** — `cia-backend` healthy after rebuild |
| Live delivery via `deploy` job SSH path | **NOT RUN** — no cloud/host credentials available; job self-skips with a notice by design |
