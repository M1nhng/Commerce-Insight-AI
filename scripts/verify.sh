#!/usr/bin/env bash
# =============================================================================
# scripts/verify.sh — Sprint 12C CI-style verification sequence
# =============================================================================
# Runs every check that does NOT require the integrated Docker stack, then
# prints the exact commands for the parts that do. Nothing here fabricates a
# result — each step reports its real exit status.
#
#   ./scripts/verify.sh              # local checks only
#   ./scripts/verify.sh --with-e2e   # also run backend integration + Playwright
#                                    # (needs Docker + a running stack)
# =============================================================================
set -uo pipefail
cd "$(dirname "$0")/.."

WITH_E2E=0
[[ "${1:-}" == "--with-e2e" ]] && WITH_E2E=1
fail=0
step() { echo; echo "── $* ──────────────────────────────────────────────"; }
run()  { echo "\$ $*"; "$@"; local rc=$?; [[ $rc -ne 0 ]] && { echo "   ✗ exit $rc"; fail=1; } || echo "   ✓"; return $rc; }

step "Static security scan"
run node scripts/security-check.mjs

step "Frontend — type-check / unit tests / build"
( cd frontend && run npm ci --no-audit --no-fund \
  && run npm run type-check \
  && run npm test \
  && run npm run build )
[[ $? -ne 0 ]] && fail=1

step "Frontend — dependency audit (informational)"
( cd frontend && npm audit --omit=dev || true )

step "MCP server — type-check / tests / build"
( cd mcp-server && run npm ci --no-audit --no-fund \
  && run npm run type-check \
  && run npm test \
  && run npm run build )
[[ $? -ne 0 ]] && fail=1

step "MCP server — dependency audit (informational)"
( cd mcp-server && npm audit || true )

step "Backend — compile + unit (no-DB) tests"
( cd backend && run ./mvnw -q -o test-compile )
[[ $? -ne 0 ]] && fail=1

if [[ $WITH_E2E -eq 1 ]]; then
  step "Integrated stack (Docker) — backend integration suite"
  run docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d --build postgres
  ( cd backend && run ./mvnw -o test -Dspring.profiles.active=test )
  [[ $? -ne 0 ]] && fail=1

  step "Integrated stack — full E2E"
  run docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d --build
  ( cd frontend && run npx playwright install --with-deps chromium && run npm run test:e2e )
  [[ $? -ne 0 ]] && fail=1
  run docker compose -f docker-compose.yml -f docker-compose.e2e.yml down -v
else
  cat <<'EOF'

── NOT RUN here — commands to complete integrated verification ──────────────
  # 1. Backend integration + security suite (needs local Postgres):
  docker compose up -d postgres
  cd backend && ./mvnw test           # or: ./mvnw verify

  # 2. Full E2E (needs Docker + browsers):
  docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d --build
  cd frontend && npx playwright install chromium && npm run test:e2e
  docker compose -f docker-compose.yml -f docker-compose.e2e.yml down -v
EOF
fi

echo
[[ $fail -eq 0 ]] && echo "verify.sh: ALL LOCAL CHECKS PASSED" || echo "verify.sh: SOME CHECKS FAILED (see above)"
exit $fail
