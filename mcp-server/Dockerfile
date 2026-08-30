# =============================================================================
# Commerce Insight AI — MCP Server Dockerfile
# Multi-stage: build the TS, then run the compiled JS on a slim runtime.
# =============================================================================
# The server speaks MCP over stdio; it also exposes GET /health on $PORT
# (default 3001) so an orchestrator can verify liveness.
# Referenced by docker-compose.yml (this file was missing before Sprint 13A).
# =============================================================================

# ── Stage 1: Build ────────────────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci --ignore-scripts

COPY tsconfig.json ./
COPY src ./src
RUN npm run build

# Drop dev dependencies for the runtime image.
RUN npm prune --omit=dev

# ── Stage 2: Runtime ─────────────────────────────────────────────────────
FROM node:20-alpine AS runtime

ENV NODE_ENV=production
WORKDIR /app

# Non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/dist ./dist
COPY package.json ./

USER appuser

EXPOSE 3001

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
    CMD wget -qO- "http://localhost:${PORT:-3001}/health" | grep -q '"status":"UP"' || exit 1

CMD ["node", "dist/index.js"]
