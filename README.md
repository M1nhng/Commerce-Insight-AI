# Commerce Insight AI

> AI-powered Ecommerce Analytics Platform

[![Backend CI](https://img.shields.io/github/actions/workflow/status/your-org/commerce-insight-ai/ci.yml?label=backend-ci&style=flat-square)](/.github/workflows/ci.yml)
[![Frontend CI](https://img.shields.io/github/actions/workflow/status/your-org/commerce-insight-ai/ci.yml?label=frontend-ci&style=flat-square)](/.github/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](./LICENSE)
[![Status](https://img.shields.io/badge/status-under_development-orange?style=flat-square)](#)

---

## Overview

**Commerce Insight AI** is a portfolio-grade, full-stack ecommerce analytics platform that integrates AI-powered insights, real-time dashboards, and a Model Context Protocol (MCP) server for AI agent interoperability.

The platform enables merchants to track products, orders, sales trends, and receive AI-generated business recommendations — all through a modern, responsive interface.

---

## Architecture

```
commerce-insight-ai/
├── backend/          # Spring Boot 3.5 — Modular Monolith REST API
├── frontend/         # React 19 + Vite + TailwindCSS + Shadcn UI
├── mcp-server/       # Node.js MCP Server (Model Context Protocol)
├── docker/           # Docker configs (Postgres, Nginx)
├── scripts/          # Dev utility scripts
├── docs/             # Project documentation
└── .github/          # CI/CD workflows & templates
```

### Architecture Pattern

- **Backend**: Modular Monolith — domain modules with strict layer separation
- **Frontend**: Feature-first with shared component library
- **MCP Server**: Thin protocol adapter — communicates only via REST API, never directly to DB

---

## Modules

| Module | Stack | Description |
|--------|-------|-------------|
| [backend](./backend/README.md) | Spring Boot 3.5, Java 17, PostgreSQL | REST API with JWT auth, domain modules |
| [frontend](./frontend/README.md) | React 19, TypeScript, Vite, TailwindCSS | SPA with Shadcn UI & TanStack Query |
| [mcp-server](./mcp-server/README.md) | Node.js, TypeScript, MCP SDK | AI agent integration via Model Context Protocol |

---

## Domain Modules (Backend)

| Domain | Responsibility |
|--------|---------------|
| `auth` | JWT authentication, token management |
| `user` | User profiles, roles, management |
| `product` | Product catalog, categories, inventory |
| `order` | Order lifecycle, line items, status |
| `analytics` | KPIs, sales trends, dashboard metrics |
| `importexport` | CSV/Excel data import & export |
| `ai` | AI prompt processing, insight generation |
| `notification` | In-app & email notifications |
| `shared` | Common DTOs, exceptions, base classes |

---

## Tech Stack

### Backend
- **Framework**: Spring Boot 3.5
- **Language**: Java 17
- **Database**: PostgreSQL 16
- **Migrations**: Flyway
- **Security**: Spring Security + JWT
- **Mapping**: MapStruct
- **Validation**: Jakarta Validation

### Frontend
- **Framework**: React 19
- **Language**: TypeScript
- **Bundler**: Vite
- **Styling**: TailwindCSS + Shadcn UI
- **Data Fetching**: TanStack Query
- **HTTP**: Axios

### MCP Server
- **Runtime**: Node.js
- **Language**: TypeScript
- **Protocol**: Model Context Protocol (MCP SDK)

### Infrastructure
- **Containerization**: Docker + Docker Compose
- **Proxy**: Nginx (production)
- **CI/CD**: GitHub Actions

---

## Quick Start

### Prerequisites
- Java 17+
- Node.js 20+
- Docker & Docker Compose
- Maven 3.9+

### 1. Clone the repository
```bash
git clone https://github.com/your-org/commerce-insight-ai.git
cd commerce-insight-ai
```

### 2. Start infrastructure
```bash
docker-compose up -d postgres
```

### 3. Run the backend
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. Run the frontend
```bash
cd frontend
npm install
npm run dev
```

### 5. Run the MCP server
```bash
cd mcp-server
npm install
npm run dev
```

Or use the one-command setup:
```bash
./scripts/setup.sh
```

---

## Environment Variables

Copy and configure environment files before running:

```bash
cp frontend/.env.example frontend/.env.local
cp mcp-server/.env.example mcp-server/.env
```

Backend configuration is managed via `backend/src/main/resources/application-dev.yml`.

---

## Documentation

| Document | Description |
|----------|-------------|
| [00 Master Prompt](./docs/00_MASTER_PROMPT.md) | AI development guidance |
| [01 Project Vision](./docs/01_PROJECT_VISION.md) | Product goals and vision |
| [02 PRD](./docs/02_PRD.md) | Product Requirements Document |
| [03 Architecture](./docs/03_ARCHITECTURE.md) | System architecture decisions |
| [04 Database](./docs/04_DATABASE.md) | Schema design & ERD |
| [05 Backend](./docs/05_BACKEND.md) | Backend implementation guide |
| [06 Frontend](./docs/06_FRONTEND.md) | Frontend implementation guide |
| [07 API](./docs/07_API.md) | REST API specification |
| [08 Auth](./docs/08_AUTH.md) | Authentication & authorization |
| [09 MCP](./docs/09_MCP.md) | MCP server specification |
| [10 AI](./docs/10_AI.md) | AI integration guide |
| [11 Import/Export](./docs/11_IMPORT_EXPORT.md) | Data import/export guide |
| [12 Testing](./docs/12_TESTING.md) | Testing strategy |
| [13 DevOps](./docs/13_DEVOPS.md) | Deployment & infrastructure |
| [14 Guidelines](./docs/14_GUIDELINE.md) | Coding standards |
| [15 Roadmap](./docs/15_ROADMAP.md) | Feature roadmap |

---

## Project Status

🚧 **Under Development** — See [ROADMAP](./docs/15_ROADMAP.md) for planned features.

---

## License

This project is licensed under the MIT License. See [LICENSE](./LICENSE) for details.