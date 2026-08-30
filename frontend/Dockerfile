# =============================================================================
# Commerce Insight AI — Frontend Dockerfile
# Multi-stage: Builder (Node 22 Alpine) builds the React app,
#              Runtime (Nginx Alpine) serves the static assets.
# =============================================================================

# ── Stage 1: Build ────────────────────────────────────────────────────────
FROM node:22-alpine AS builder

WORKDIR /app

# Backend API base URL is baked in at build time (Vite inlines import.meta.env.*).
# Override per environment:  docker build --build-arg VITE_API_BASE_URL=https://api.example.com
# Not a secret — Vite variables are always public. Never put the MCP key or a
# JWT secret here.
ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# Copy package files first (layer cache optimization)
COPY package.json package-lock.json* ./

# Install dependencies
RUN npm ci --ignore-scripts

# Copy source and build
COPY . .
RUN npm run build

# ── Stage 2: Runtime (Nginx) ──────────────────────────────────────────────
FROM nginx:alpine AS runtime

# Remove default nginx static assets
RUN rm -rf /usr/share/nginx/html/*

# Copy built assets from builder
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom nginx config for React Router (SPA)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
