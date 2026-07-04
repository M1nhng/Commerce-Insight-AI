#!/usr/bin/env bash
# =============================================================================
# migrate.sh — Flyway Database Migration Helper
# Commerce Insight AI
# =============================================================================
# Usage: ./scripts/migrate.sh [profile]
#        profile defaults to "dev"
# =============================================================================

set -euo pipefail

PROFILE="${1:-dev}"

echo "Running Flyway migrations with profile: $PROFILE"

cd backend
./mvnw flyway:migrate \
  -Dspring.profiles.active="$PROFILE" \
  -Dflyway.locations=classpath:db/migration

echo "Migrations complete."
