#!/usr/bin/env bash
# =============================================================================
# scripts/demo-up.sh — start the local DEMO stack
# Commerce Insight AI · Sprint 13C
# =============================================================================
# Brings up the full existing stack (Postgres + Spring Boot + React + MCP) with
# the `demo` Spring profile and the deterministic demo dataset
# (db/demo/R__seed_demo_data.sql, applied automatically by Flyway on first boot).
#
#   ./scripts/demo-up.sh            # build if needed + start, wait for health
#   ./scripts/demo-up.sh --build    # force image rebuild
#
# Demo credentials (DEMO ONLY — never production):
#   ADMIN    demo-admin@commerceinsight.demo    / DemoAdmin!2024
#   MANAGER  demo-manager@commerceinsight.demo  / DemoManager!2024
#   STAFF    demo-staff@commerceinsight.demo    / DemoStaff!2024
#
# Reset everything:  ./scripts/demo-reset.sh
# =============================================================================
set -euo pipefail
cd "$(dirname "$0")/.."

COMPOSE=(docker compose -f docker-compose.yml -f docker-compose.demo.yml)
BUILD_FLAG=""
[[ "${1:-}" == "--build" ]] && BUILD_FLAG="--build"

echo "▶ Starting the Commerce Insight DEMO stack (profile: demo)…"
"${COMPOSE[@]}" up -d ${BUILD_FLAG}

echo "▶ Waiting for the backend to become healthy (first boot runs 31 migrations + the demo seed)…"
for i in $(seq 1 60); do
  if curl -fsS http://localhost:8080/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
    echo "  ✓ backend healthy"
    break
  fi
  sleep 5
  [[ $i -eq 60 ]] && { echo "  ✗ backend did not become healthy in time"; "${COMPOSE[@]}" logs --tail=100 backend; exit 1; }
done

echo
echo "  Frontend : http://localhost:5173"
echo "  API      : http://localhost:8080/api/v1"
echo "  Swagger  : http://localhost:8080/swagger-ui.html   (disabled outside dev/demo)"
echo "  MCP      : http://localhost:3001/health"
echo
echo "  Log in as  demo-admin@commerceinsight.demo / DemoAdmin!2024"
echo "  Full walkthrough: docs/demo/SPRINT_13C_DEMO_DATA.md"
