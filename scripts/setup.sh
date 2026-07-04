#!/usr/bin/env bash
# =============================================================================
# setup.sh — One-Command Development Environment Setup
# Commerce Insight AI
# =============================================================================
# Usage: ./scripts/setup.sh
# =============================================================================

set -euo pipefail

echo "================================================"
echo " Commerce Insight AI — Dev Setup"
echo "================================================"

# 1. Check prerequisites
echo ""
echo "[1/5] Checking prerequisites..."
command -v java >/dev/null 2>&1 || { echo "ERROR: Java 17+ is required."; exit 1; }
command -v node >/dev/null 2>&1 || { echo "ERROR: Node.js 20+ is required."; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "ERROR: Docker is required."; exit 1; }
echo "  ✓ Java: $(java -version 2>&1 | head -n 1)"
echo "  ✓ Node: $(node --version)"
echo "  ✓ Docker: $(docker --version)"

# 2. Start infrastructure
echo ""
echo "[2/5] Starting PostgreSQL..."
docker-compose up -d postgres
echo "  Waiting for PostgreSQL to be ready..."
sleep 5

# 3. Configure environment files
echo ""
echo "[3/5] Configuring environment files..."
if [ ! -f "frontend/.env.local" ]; then
  cp frontend/.env.example frontend/.env.local
  echo "  ✓ Created frontend/.env.local"
else
  echo "  ⚠ frontend/.env.local already exists, skipping."
fi
if [ ! -f "mcp-server/.env" ]; then
  cp mcp-server/.env.example mcp-server/.env
  echo "  ✓ Created mcp-server/.env"
else
  echo "  ⚠ mcp-server/.env already exists, skipping."
fi

# 4. Install Node.js dependencies
echo ""
echo "[4/5] Installing Node.js dependencies..."
(cd frontend && npm install)
echo "  ✓ Frontend dependencies installed."
(cd mcp-server && npm install)
echo "  ✓ MCP server dependencies installed."

# 5. Run Flyway migrations
echo ""
echo "[5/5] Running database migrations..."
# TODO: Uncomment when backend pom.xml has Flyway Maven plugin configured
# (cd backend && ./mvnw flyway:migrate -Dspring.profiles.active=dev)
echo "  ⚠ Flyway migration via Maven is not yet configured."
echo "  Run: cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev"

echo ""
echo "================================================"
echo " Setup complete!"
echo ""
echo " Next steps:"
echo "   Backend:    cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev"
echo "   Frontend:   cd frontend && npm run dev"
echo "   MCP Server: cd mcp-server && npm run dev"
echo "================================================"
