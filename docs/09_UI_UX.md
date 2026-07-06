# 09 — UI/UX Design
# Commerce Insight AI

> **Document Type**: Frontend Design System
> **Version**: 1.0.0
> **Status**: Approved
> **Last Updated**: 2026-07-06
> **Owner**: Chief Solution Architect

---

## Table of Contents

1. [Purpose](#1-purpose)
2. [Design Principles](#2-design-principles)
3. [Theme & Color System](#3-theme--color-system)
4. [Typography](#4-typography)
5. [Layout & Navigation](#5-layout--navigation)
6. [Dashboard Design](#6-dashboard-design)
7. [Responsive Design](#7-responsive-design)
8. [Dark Mode](#8-dark-mode)
9. [Charts & Data Visualization](#9-charts--data-visualization)
10. [Component Patterns](#10-component-patterns)
11. [Accessibility](#11-accessibility)
12. [Page-by-Page Design Spec](#12-page-by-page-design-spec)

---

## 1. Purpose

This document defines the UI/UX design system for Commerce Insight AI. It specifies the visual language, component patterns, layout structure, and interaction design that every frontend developer must follow.

This document is the single source of truth for:
- Color system and theme tokens
- Typography scale
- Component behavior and layout
- Responsive breakpoints
- Dark mode implementation
- Accessibility requirements

---

## 2. Design Principles

### 2.1 Core Principles

| Principle | Description | Application |
|-----------|-------------|-------------|
| **Data-First** | Data must be immediately legible and actionable | High contrast numbers, clear labels, no decorative noise |
| **Professional Clarity** | Looks like enterprise SaaS, not a student project | Clean whitespace, consistent spacing, quality typography |
| **Dark by Default** | Dark theme is the primary experience | Deep background, subtle borders, vibrant accent |
| **Purposeful Motion** | Animations communicate state, not decoration | Loading states, transitions, hover feedback |
| **Consistency** | Same component, same pattern, every page | Design system adherence |

### 2.2 Visual Identity

- **Personality**: Modern, confident, data-focused, professional
- **Feel**: Bloomberg Terminal meets Linear.app — power with elegance
- **NOT**: Playful, cartoon, excessive gradients, busy
- **Reference inspirations**: Vercel Dashboard, Linear, Raycast, Stripe Dashboard

---

## 3. Theme & Color System

### 3.1 Color Tokens (CSS Custom Properties)

```css
/* Dark Theme (Default) */
:root[data-theme="dark"] {
  /* Backgrounds */
  --bg-base:        #0a0a0f;    /* Deepest background */
  --bg-surface:     #111118;    /* Card/panel background */
  --bg-elevated:    #1a1a24;    /* Elevated elements, dropdowns */
  --bg-overlay:     #22223a;    /* Hover states, selected rows */

  /* Borders */
  --border-subtle:  #1e1e2e;    /* Dividers, separators */
  --border-default: #2a2a3d;    /* Card borders */
  --border-strong:  #3a3a5c;    /* Focus rings, active borders */

  /* Text */
  --text-primary:   #e8e8f0;    /* Main body text */
  --text-secondary: #8888a4;    /* Labels, captions */
  --text-muted:     #55556a;    /* Placeholders, disabled */
  --text-inverse:   #0a0a0f;    /* Text on accent backgrounds */

  /* Accent — Indigo-Violet */
  --accent-50:      #eef2ff;
  --accent-100:     #e0e7ff;
  --accent-400:     #818cf8;    /* Light accent */
  --accent-500:     #6366f1;    /* Primary accent */
  --accent-600:     #4f46e5;    /* Hover accent */
  --accent-700:     #4338ca;    /* Active accent */

  /* Semantic */
  --success:        #22c55e;    /* Green */
  --success-bg:     #14532d22;
  --warning:        #f59e0b;    /* Amber */
  --warning-bg:     #78350f22;
  --error:          #ef4444;    /* Red */
  --error-bg:       #7f1d1d22;
  --info:           #38bdf8;    /* Sky */
  --info-bg:        #0c4a6e22;

  /* Chart palette */
  --chart-1:        #6366f1;    /* Indigo */
  --chart-2:        #22d3ee;    /* Cyan */
  --chart-3:        #a78bfa;    /* Violet */
  --chart-4:        #34d399;    /* Emerald */
  --chart-5:        #fb923c;    /* Orange */
}

/* Light Theme */
:root[data-theme="light"] {
  --bg-base:        #f8f9fc;
  --bg-surface:     #ffffff;
  --bg-elevated:    #f1f3f9;
  --bg-overlay:     #e8ecf5;
  --border-subtle:  #e8ecf5;
  --border-default: #d4d8e8;
  --border-strong:  #b0b7d0;
  --text-primary:   #111118;
  --text-secondary: #5c6080;
  --text-muted:     #9099b8;
  --accent-500:     #4f46e5;
  /* ... rest mirrors dark theme accent colors */
}
```

### 3.2 Spacing Scale

```
4px   → 0.25rem  (space-1) — icon gap, tight spacing
8px   → 0.5rem   (space-2) — small gap
12px  → 0.75rem  (space-3) — compact padding
16px  → 1rem     (space-4) — standard padding
24px  → 1.5rem   (space-6) — card padding
32px  → 2rem     (space-8) — section gap
48px  → 3rem     (space-12) — page section
64px  → 4rem     (space-16) — hero spacing
```

### 3.3 Border Radius

```
sm:  4px   — tags, badges, inputs
md:  8px   — buttons, small cards
lg:  12px  — cards, panels
xl:  16px  — modals, dialogs
2xl: 24px  — large feature cards
```

---

## 4. Typography

### 4.1 Font Stack

| Role | Font | Fallback |
|------|------|----------|
| **UI / Body** | Inter | system-ui, sans-serif |
| **Code / Mono** | JetBrains Mono | 'Courier New', monospace |

Both loaded from Google Fonts via `index.html`.

### 4.2 Type Scale

| Name | Size | Weight | Line Height | Usage |
|------|------|--------|-------------|-------|
| `display-lg` | 48px / 3rem | 700 | 1.1 | Hero numbers (KPI cards) |
| `display-md` | 36px / 2.25rem | 700 | 1.2 | Page KPIs |
| `heading-1` | 30px / 1.875rem | 600 | 1.3 | Page titles |
| `heading-2` | 24px / 1.5rem | 600 | 1.4 | Section headings |
| `heading-3` | 20px / 1.25rem | 600 | 1.4 | Card titles |
| `heading-4` | 16px / 1rem | 600 | 1.5 | Subsection |
| `body-lg` | 16px / 1rem | 400 | 1.6 | Body text |
| `body-md` | 14px / 0.875rem | 400 | 1.6 | Default UI text |
| `body-sm` | 13px / 0.8125rem | 400 | 1.5 | Labels, captions |
| `caption` | 12px / 0.75rem | 400 | 1.4 | Helper text, timestamps |
| `code` | 13px / 0.8125rem | 400 | 1.6 | Code, SKUs, IDs |

---

## 5. Layout & Navigation

### 5.1 App Shell Structure

```
┌────────────────────────────────────────────────────────────┐
│                    Header (64px height)                     │
│  [Logo] [Page Title]              [Search] [Notifs] [User] │
├───────────┬────────────────────────────────────────────────┤
│           │                                                 │
│  Sidebar  │              Main Content Area                  │
│  (240px)  │              (flex-1, scrollable)               │
│           │                                                 │
│  Collapsed│                                                 │
│  (64px)   │                                                 │
│           │                                                 │
│           │                                                 │
└───────────┴────────────────────────────────────────────────┘
```

### 5.2 Sidebar Navigation

The sidebar is the primary navigation element.

**Desktop (≥ 1024px):** Always visible, collapsible to icon-only mode.

**Tablet (768px - 1023px):** Collapsed by default, toggle button to expand as overlay.

**Mobile (< 768px):** Hidden by default, accessible via hamburger menu as a full-width drawer.

### 5.3 Sidebar Items

```
────────────────
 [CIA Logo]  Commerce Insight AI
────────────────
OVERVIEW
  📊 Dashboard
────────────────
OPERATIONS
  📦 Products
  🏷️  Categories
  👥 Customers
  🛒 Orders
  📋 Inventory
────────────────
INTELLIGENCE
  📈 Analytics
  🤖 AI Assistant
────────────────
DATA
  📥 Import
  📤 Export
────────────────
SYSTEM
  ⚙️  Settings
  👤 Admin        (ADMIN only)
────────────────
```

### 5.4 Header Bar

```
Left:   [≡ Sidebar Toggle]  [Breadcrumb path]
Center: (empty — or global search in future)
Right:  [🔔 Notifications]  [Avatar Menu → Profile / Switch Theme / Logout]
```

---

## 6. Dashboard Design

### 6.1 KPI Cards Row (Top)

Four equal-width cards at the top of the dashboard:

```
┌────────────────┐ ┌────────────────┐ ┌────────────────┐ ┌────────────────┐
│ Total Revenue  │ │  Total Orders  │ │    Customers   │ │    Products    │
│                │ │                │ │                │ │                │
│  $124,580      │ │    1,842       │ │      367       │ │     248        │
│                │ │                │ │                │ │                │
│ ↑ 12.4%        │ │  ↑ 8.2%        │ │  ↑ 5.1%        │ │  ↓ 2.1%        │
│ vs last month  │ │  vs last month │ │  vs last month │ │  vs last month │
└────────────────┘ └────────────────┘ └────────────────┘ └────────────────┘
```

**KPI Card Design:**
- Background: `--bg-surface`
- Border: `--border-default`
- Icon: colored icon (accent color), right-aligned
- Number: `display-md` weight, `--text-primary`
- Change indicator: Green/Red with arrow icon
- Subtle trend sparkline: 7-day mini chart (optional)

### 6.2 Main Chart — Revenue Trend

Full-width area chart:
- X-axis: dates (day/week/month selectable)
- Y-axis: revenue in currency
- Area fill: gradient from `--accent-500` to transparent
- Line: `--accent-400`
- Tooltip: dark glassmorphism card
- Period selector: Day / Week / Month / Quarter toggle pills

### 6.3 Lower Row — Two Columns

```
┌─────────────────────────────────┐  ┌─────────────────────────────────┐
│     Top 5 Products (Bar)        │  │     Recent Orders (Table)        │
│                                 │  │                                 │
│ ████████████ Product A $12k    │  │ #ORD-001  John D.   $128  SHIPPED│
│ ███████      Product B $8.5k   │  │ #ORD-002  Jane S.   $89   PENDING│
│ █████        Product C $6.2k   │  │ #ORD-003  Bob K.    $245  CONF.  │
│ ████         Product D $5.8k   │  │ ...                              │
│ ██           Product E $3.1k   │  │                 [View All →]     │
└─────────────────────────────────┘  └─────────────────────────────────┘
```

### 6.4 AI Insight Widget

```
┌───────────────────────────────────────────────────────────────────────┐
│ 🤖 AI Insight                                              [Ask more] │
│───────────────────────────────────────────────────────────────────────│
│ "Your revenue this month is up 12.4% compared to last month,          │
│  driven primarily by Electronics (↑28%) and Accessories (↑15%).       │
│  Product 'Wireless Headphones Pro' is your top earner at $12,400."    │
│                                              Powered by: OpenAI GPT-4o│
└───────────────────────────────────────────────────────────────────────┘
```

---

## 7. Responsive Design

### 7.1 Breakpoints

| Name | Min Width | Layout Changes |
|------|-----------|---------------|
| `xs` | 0px | Mobile — stacked, 1 column |
| `sm` | 640px | Slightly wider, still mobile |
| `md` | 768px | Tablet — 2-column grid starts |
| `lg` | 1024px | Desktop — full sidebar, 4-column grid |
| `xl` | 1280px | Wide desktop — expanded layouts |
| `2xl` | 1536px | Ultrawide — max content width capped |

### 7.2 Responsive Grid

| Screen | KPI Cards | Chart | Bottom Row |
|--------|-----------|-------|-----------|
| Mobile | 1 per row (stacked) | Full width | Stacked |
| Tablet | 2 per row | Full width | Stacked |
| Desktop | 4 per row | Full width | 2 columns |

### 7.3 Max Content Width

Content area has a maximum width of `1440px` centered with `margin: 0 auto`.
This prevents overly wide layouts on ultrawide monitors.

---

## 8. Dark Mode

### 8.1 Default Mode

**Dark mode is the default** and the primary design target.

### 8.2 Theme Toggle

- Available in the user avatar dropdown menu
- Persisted to `localStorage` via `ThemeProvider`
- System preference respected on first visit via `prefers-color-scheme`

### 8.3 Implementation Pattern

```typescript
// ThemeProvider stores 'dark' | 'light' | 'system'
// Applied to <html data-theme="dark"> or <html data-theme="light">
// CSS custom properties switch automatically via [data-theme] selector
```

### 8.4 Dark Mode Considerations

| Element | Dark | Light |
|---------|------|-------|
| Shadows | Subtle, very dark (--bg-base opacity) | Standard box shadows |
| Charts | `--chart-*` palette on dark bg | Same palette |
| Tables | Alternating rows: `--bg-surface` / `--bg-elevated` | White / Light gray |
| Inputs | `--bg-elevated` background | White background |
| Code blocks | `--bg-base` | Light gray |

---

## 9. Charts & Data Visualization

### 9.1 Chart Library

**Recharts** is the chart library. All charts use Recharts components with custom styling.

### 9.2 Chart Type Reference

| Data Type | Chart Type | Component |
|-----------|-----------|-----------|
| Revenue over time | Area Chart | `<AreaChart>` with gradient fill |
| Revenue by category | Horizontal Bar | `<BarChart layout="vertical">` |
| Top products | Horizontal Bar | `<BarChart layout="vertical">` |
| Order status distribution | Donut Chart | `<PieChart>` |
| Period comparison | Grouped Bar | `<BarChart>` |
| Trend with 2 metrics | Line Chart | `<LineChart>` multi-series |

### 9.3 Chart Design Standards

```
All charts:
  - Background: transparent (inherits card background)
  - Grid lines: --border-subtle, dashed
  - Axis labels: --text-secondary, caption size
  - Tooltip: bg --bg-elevated, border --border-default, rounded-lg
  - Animation: 400ms ease-out on mount
  - Responsive: <ResponsiveContainer width="100%" height={300}>

Area Chart specific:
  - Area fill: linear gradient (accent → transparent at 10% opacity)
  - Stroke: --accent-400, strokeWidth: 2
  - Dots: shown on hover only

Bar Chart specific:
  - Fill: --chart-1 through --chart-5
  - Radius: [4, 4, 0, 0] (top corners rounded)
  - Gap between bars: 8px
```

---

## 10. Component Patterns

### 10.1 Data Table

Standard table pattern used across Products, Orders, Customers, etc.:

```
┌──────────────────────────────────────────────────────────────────────┐
│ [Filter/Search input]                        [+ New Product] [Export]│
├──────────────────────────────────────────────────────────────────────┤
│ □  Name ↑     SKU      Category    Price    Stock   Status   Actions │
├──────────────────────────────────────────────────────────────────────┤
│ □  Product A  SKU-001  Electronics $49.99   124     Active   [⋮]    │
│ □  Product B  SKU-002  Accessories $19.99   8 ⚠️    Active   [⋮]    │
├──────────────────────────────────────────────────────────────────────┤
│ Showing 1-10 of 248              [← Prev] [1][2][3]...[25] [Next →] │
└──────────────────────────────────────────────────────────────────────┘
```

**Table features:**
- Sortable columns (click header)
- Checkbox multi-select
- Row hover highlight
- Actions dropdown menu per row
- Pagination with page size selector (10, 25, 50)
- Loading skeleton (not spinner)

### 10.2 Status Badges

```
PENDING     → bg: --warning-bg,  text: --warning    (amber)
CONFIRMED   → bg: --info-bg,     text: --info        (sky)
PROCESSING  → bg: --info-bg,     text: --info        (sky)
SHIPPED     → bg: --info-bg,     text: --accent-400  (indigo)
DELIVERED   → bg: --success-bg,  text: --success     (green)
CANCELLED   → bg: --error-bg,    text: --error       (red)
REFUNDED    → bg: --error-bg,    text: --warning     (amber)
ACTIVE      → bg: --success-bg,  text: --success
INACTIVE    → bg: --bg-overlay,  text: --text-muted
```

### 10.3 Form Design

```
Label (body-sm, --text-secondary)
┌─────────────────────────────────────────┐
│ Input value                           ▼ │  ← input/select
└─────────────────────────────────────────┘
  Helper text or error message (caption)
```

- Labels are always visible (no floating labels)
- Error state: red border + red error text below
- Required fields marked with asterisk in label
- Full-width inputs in forms
- Submit buttons: primary solid button, aligned right

### 10.4 Loading States

| State | Pattern |
|-------|---------|
| Page load | Skeleton cards in the shape of the content |
| Table load | 5-row skeleton table |
| Button action | Spinner inside button, button disabled |
| Chart load | Pulsing skeleton rectangle |
| AI typing | Three-dot animation |

**Never use a full-page spinner.** Skeleton loading is always preferred.

### 10.5 Empty States

When a list/table is empty:

```
         ┌─────────────────────────────┐
         │                             │
         │    [Illustrated icon]       │
         │                             │
         │  No products yet            │
         │                             │
         │  Add your first product to  │
         │  start tracking inventory.  │
         │                             │
         │     [+ Add Product]         │
         │                             │
         └─────────────────────────────┘
```

---

## 11. Accessibility

### 11.1 WCAG 2.1 AA Compliance Targets

| Criterion | Requirement |
|-----------|-------------|
| Color contrast (text) | Minimum 4.5:1 ratio (AA) |
| Color contrast (large text) | Minimum 3:1 ratio |
| Keyboard navigation | All interactive elements reachable via Tab |
| Focus visible | Clear focus ring on all focusable elements |
| Screen readers | All images have alt text; icons have aria-label |
| Form labels | All form inputs have associated `<label>` |
| Error messages | Associated with inputs via `aria-describedby` |
| Dynamic content | `aria-live` regions for loading states and alerts |

### 11.2 Focus Ring Style

```css
/* Custom focus ring — visible in both themes */
:focus-visible {
  outline: 2px solid var(--accent-500);
  outline-offset: 2px;
  border-radius: 4px;
}
```

### 11.3 Semantic HTML Requirements

| Element | Usage |
|---------|-------|
| `<main>` | Primary content area |
| `<nav>` | Sidebar navigation |
| `<header>` | App header |
| `<h1>` | One per page (page title) |
| `<h2>`, `<h3>` | Section and card headings |
| `<button>` | All interactive trigger elements |
| `<a>` | Navigation links only |
| `<table>` | Data tables with `<th scope>` |
| `<form>` | All forms with proper labels |

---

## 12. Page-by-Page Design Spec

### 12.1 Login Page

- Full viewport, centered card (max-width: 400px)
- Dark background with subtle gradient
- Logo at top
- Email + Password inputs
- "Forgot password?" link
- Submit button full width
- No registration link (admin creates users)

### 12.2 Dashboard Page

- Page heading: "Dashboard" with date range picker
- 4 KPI cards (full design in §6)
- Revenue Trend chart (full width)
- Top Products + Recent Orders (2-col)
- AI Insight widget (full width, bottom)

### 12.3 Products Page

- Page heading + "Add Product" button
- Filter bar: Search, Category filter, Status filter
- Product data table (see §10.1)
- Clicking row → Product detail side panel (drawer)

### 12.4 Orders Page

- Page heading + date range filter
- Order status tab pills: All | Pending | Confirmed | Processing | Shipped | Delivered | Cancelled
- Order data table
- Order detail side panel with line items

### 12.5 Analytics Page

- Tab navigation: Revenue | Products | Customers | Categories
- Date range picker + compare toggle
- Per-tab: large chart + summary table

### 12.6 AI Assistant Page

- Split layout: Conversation history (left/top) + Input (bottom)
- Message bubbles: User (right-aligned, accent) + AI (left-aligned, surface)
- Tool usage indicator: subtle chip showing which tools were called
- New Conversation button
- Session list in sidebar panel

### 12.7 Import Page

- Step wizard: 1. Upload → 2. Preview → 3. Import
- Drag-and-drop file upload zone
- Preview table (first 10 rows)
- Import result: success/error breakdown with error table

### 12.8 Admin Page

- Tab navigation: Users | Audit Log | System Settings
- Users: table with role badges + actions
- Audit Log: searchable timeline of events
- System Settings: key-value form for AI provider, API keys (masked)
