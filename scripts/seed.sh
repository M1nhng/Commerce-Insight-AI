#!/usr/bin/env bash
# =============================================================================
# seed.sh — Database Seeder Helper
# Commerce Insight AI
# =============================================================================
# Usage: ./scripts/seed.sh
#        Runs V2__seed_data.sql via backend dev profile.
# =============================================================================

set -euo pipefail

echo "Seeding database with sample data..."

# TODO: Implement seeding strategy when V2__seed_data.sql is populated.
#       Options:
#         a) Run the Spring Boot seed endpoint (POST /api/admin/seed)
#         b) Execute V2 SQL directly via psql
#         c) Use Spring Boot CommandLineRunner in dev profile

echo "  ⚠ Seeder not yet implemented."
echo "  Seed data will be added in V2__seed_data.sql once schema is finalized."
