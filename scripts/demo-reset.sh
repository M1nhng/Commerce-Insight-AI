#!/usr/bin/env bash
# =============================================================================
# scripts/demo-reset.sh — wipe & rebuild the DEMO environment
# Commerce Insight AI · Sprint 13C
# =============================================================================
# DESTRUCTIVE, but ONLY for the demo stack: it targets exactly the
# docker-compose.demo.yml overlay and the cia-postgres-demo-data volume.
# It can never touch dev (commerce_insight_dev), e2e (commerce_insight_e2e)
# or prod (commerce_insight) — those live on different databases/volumes.
#
#   ./scripts/demo-reset.sh          # down -v (demo only) + fresh up
#   ./scripts/demo-reset.sh --keep-images   # skip rebuild
# =============================================================================
set -euo pipefail
cd "$(dirname "$0")/.."

COMPOSE=(docker compose -f docker-compose.yml -f docker-compose.demo.yml)

echo "▶ Tearing down the DEMO stack and its volume (cia-postgres-demo-data)…"
"${COMPOSE[@]}" down -v

BUILD_FLAG="--build"
[[ "${1:-}" == "--keep-images" ]] && BUILD_FLAG=""

echo "▶ Rebuilding a clean DEMO stack…"
exec ./scripts/demo-up.sh ${BUILD_FLAG:+--build}
