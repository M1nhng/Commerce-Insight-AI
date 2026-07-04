# Frontend — Commerce Insight AI

> React 19 · TypeScript · Vite · TailwindCSS · Shadcn UI · TanStack Query

---

## Overview

This module contains the **React Single Page Application** for Commerce Insight AI. It follows a **feature-first** architecture where each business domain owns its own components, hooks, services, and types.

---

## Project Structure

```
frontend/
├── index.html
├── vite.config.ts
├── tailwind.config.ts
├── tsconfig.json
├── package.json
│
└── src/
    ├── main.tsx                  # React entry point
    ├── App.tsx                   # Root component with router
    ├── vite-env.d.ts
    │
    ├── assets/                   # Static assets
    │   ├── images/
    │   └── icons/
    │
    ├── components/               # Shared, reusable components
    │   ├── ui/                   # Shadcn UI components
    │   ├── layout/               # Shell, Sidebar, Navbar, Footer
    │   └── common/               # Generic UI (Spinner, ErrorBoundary, etc.)
    │
    ├── features/                 # Domain feature modules
    │   ├── auth/                 # Login, Register, Forgot Password
    │   ├── dashboard/            # Main dashboard, KPI cards
    │   ├── products/             # Product list, detail, form
    │   ├── orders/               # Order list, detail, status
    │   ├── analytics/            # Charts, reports, trends
    │   ├── ai-insights/          # AI chat, insight cards
    │   ├── import-export/        # CSV upload, export panel
    │   └── settings/             # Profile, preferences, API keys
    │
    ├── hooks/                    # Shared custom hooks
    ├── lib/                      # Utilities, API client, validators
    ├── pages/                    # Route-level page components
    ├── providers/                # React context providers
    ├── router/                   # Route definitions
    ├── services/                 # API service layer (one per domain)
    ├── store/                    # Zustand global state slices
    └── types/                    # Shared TypeScript type definitions
```

---

## Feature Module Convention

Each feature under `src/features/` follows the same internal structure:

```
features/{feature}/
├── components/     # Feature-specific UI components
├── hooks/          # Feature-specific custom hooks
├── services/       # API calls for this feature
├── types/          # Feature-specific TypeScript types
└── index.ts        # Public API (barrel export)
```

---

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| React | 19 | UI framework |
| TypeScript | 5.x | Type safety |
| Vite | 6.x | Build tool & dev server |
| TailwindCSS | 4.x | Utility-first CSS |
| Shadcn UI | latest | Component library |
| TanStack Query | 5.x | Server state management |
| React Router | 6.x | Client-side routing |
| Axios | 1.x | HTTP client |
| Zustand | 4.x | Client state management |
| Zod | 3.x | Schema validation |

---

## Running Locally

```bash
# Install dependencies
npm install

# Copy environment variables
cp .env.example .env.local

# Start dev server
npm run dev
```

App will be available at: `http://localhost:5173`

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `VITE_API_BASE_URL` | Backend REST API URL | `http://localhost:8080` |
| `VITE_MCP_SERVER_URL` | MCP server URL | `http://localhost:3001` |
| `VITE_APP_NAME` | Application display name | `Commerce Insight AI` |

---

## Scripts

| Command | Description |
|---------|-------------|
| `npm run dev` | Start dev server |
| `npm run build` | Production build |
| `npm run preview` | Preview production build |
| `npm run lint` | ESLint check |
| `npm run type-check` | TypeScript type check |

---

## Status

🚧 **Structure initialized** — No features implemented yet.
