# 04 — Database Design
# Commerce Insight AI

> **Document Type**: Database Architecture
> **Version**: 1.0.0
> **Status**: Approved
> **Last Updated**: 2026-07-06
> **Owner**: Chief Solution Architect

---

## Table of Contents

1. [Purpose](#1-purpose)
2. [Naming Conventions](#2-naming-conventions)
3. [Entity List](#3-entity-list)
4. [Entity Definitions](#4-entity-definitions)
5. [Relationships](#5-relationships)
6. [Entity Relationship Diagram](#6-entity-relationship-diagram)
7. [Indexes](#7-indexes)
8. [Constraints](#8-constraints)
9. [Migration Strategy](#9-migration-strategy)
10. [Audit Strategy](#10-audit-strategy)
11. [Soft Delete Strategy](#11-soft-delete-strategy)
12. [Data Types Reference](#12-data-types-reference)

---

## 1. Purpose

This document defines the complete database schema for Commerce Insight AI. It is the authoritative reference for all Flyway migrations and JPA entity definitions.

All database changes MUST:
1. Be implemented as a Flyway migration script
2. Follow the naming convention defined in §2
3. Be reviewed against the constraints in §8

---

## 2. Naming Conventions

### 2.1 Table Names
- **Format**: `snake_case`, plural
- **Examples**: `users`, `products`, `order_items`, `refresh_tokens`

### 2.2 Column Names
- **Format**: `snake_case`
- **Primary Key**: `id` (always UUID)
- **Foreign Keys**: `{table_singular}_id` (e.g., `user_id`, `product_id`)
- **Timestamps**: `created_at`, `updated_at`, `deleted_at`
- **Booleans**: `is_{adjective}` or plain adjective (e.g., `active`, `locked`)
- **Enums stored as**: `VARCHAR(50)` with CHECK constraint

### 2.3 Index Names
- **Format**: `idx_{table}_{column(s)}`
- **Unique**: `uq_{table}_{column}`
- **Foreign Key**: `fk_{table}_{referenced_table}`

### 2.4 Migration File Names
- **Format**: `V{version}__{description}.sql`
- **Examples**: `V1__init_schema.sql`, `V2__seed_data.sql`, `V10__add_inventory_movements.sql`

---

## 3. Entity List

| Entity | Table | Module | Description |
|--------|-------|--------|-------------|
| User | `users` | user | Platform user accounts |
| Role | Enum (not a table) | user | ADMIN, MANAGER, STAFF |
| RefreshToken | `refresh_tokens` | auth | JWT refresh token records |
| Product | `products` | product | Product catalog |
| Category | `categories` | category | Product categories (tree) |
| Customer | `customers` | customer | Customer profiles |
| Order | `orders` | order | Customer orders |
| OrderItem | `order_items` | order | Line items within an order |
| Inventory | `inventory` | inventory | Current stock per product |
| InventoryMovement | `inventory_movements` | inventory | Stock change history |
| ConversationSession | `conversation_sessions` | ai | AI chat sessions |
| ConversationMessage | `conversation_messages` | ai | Individual chat messages |
| AuditLog | `audit_logs` | admin | System audit trail |
| SystemSetting | `system_settings` | admin | Configurable platform settings |

---

## 4. Entity Definitions

### 4.1 users

```sql
CREATE TABLE users (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255)  NOT NULL,
    password_hash VARCHAR(255)  NOT NULL,
    first_name    VARCHAR(100)  NOT NULL,
    last_name     VARCHAR(100)  NOT NULL,
    role          VARCHAR(50)   NOT NULL CHECK (role IN ('ADMIN', 'MANAGER', 'STAFF')),
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    locked        BOOLEAN       NOT NULL DEFAULT FALSE,
    failed_attempts INTEGER     NOT NULL DEFAULT 0,
    last_login_at TIMESTAMPTZ,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ
);
```

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID | PK, auto-generated |
| `email` | VARCHAR(255) | Unique, used for login |
| `password_hash` | VARCHAR(255) | BCrypt hash |
| `first_name` | VARCHAR(100) | |
| `last_name` | VARCHAR(100) | |
| `role` | VARCHAR(50) | Enum: ADMIN, MANAGER, STAFF |
| `active` | BOOLEAN | false = deactivated account |
| `locked` | BOOLEAN | true = locked after failed attempts |
| `failed_attempts` | INTEGER | Reset on successful login |
| `last_login_at` | TIMESTAMPTZ | Nullable |
| `created_at` | TIMESTAMPTZ | Auto-set |
| `updated_at` | TIMESTAMPTZ | Auto-updated via trigger |
| `deleted_at` | TIMESTAMPTZ | Soft delete |

### 4.2 refresh_tokens

```sql
CREATE TABLE refresh_tokens (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash   VARCHAR(64)  NOT NULL,
    user_id      UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    family_id    UUID         NOT NULL,
    expires_at   TIMESTAMPTZ  NOT NULL,
    revoked      BOOLEAN      NOT NULL DEFAULT FALSE,
    revoked_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

| Column | Type | Notes |
|--------|------|-------|
| `token_hash` | VARCHAR(64) | SHA-256 hex of the actual token |
| `family_id` | UUID | Groups tokens in a rotation chain |
| `revoked` | BOOLEAN | True if this token was used or invalidated |

### 4.3 categories

```sql
CREATE TABLE categories (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(150)  NOT NULL,
    slug        VARCHAR(150)  NOT NULL,
    description TEXT,
    parent_id   UUID          REFERENCES categories(id) ON DELETE RESTRICT,
    sort_order  INTEGER       NOT NULL DEFAULT 0,
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);
```

| Column | Type | Notes |
|--------|------|-------|
| `parent_id` | UUID | Nullable — null means root category |
| `slug` | VARCHAR(150) | URL-friendly name, unique |
| `sort_order` | INTEGER | For custom ordering in UI |

### 4.4 products

```sql
CREATE TABLE products (
    id            UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    sku           VARCHAR(100)   NOT NULL,
    name          VARCHAR(255)   NOT NULL,
    description   TEXT,
    price         DECIMAL(19,4)  NOT NULL CHECK (price >= 0),
    cost_price    DECIMAL(19,4)  CHECK (cost_price >= 0),
    image_url     VARCHAR(1000),
    category_id   UUID           REFERENCES categories(id) ON DELETE SET NULL,
    active        BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ
);
```

| Column | Type | Notes |
|--------|------|-------|
| `sku` | VARCHAR(100) | Unique identifier within the store |
| `price` | DECIMAL(19,4) | Selling price |
| `cost_price` | DECIMAL(19,4) | Optional — for margin calculation |
| `image_url` | VARCHAR(1000) | External URL |
| `category_id` | UUID | Nullable FK |

### 4.5 customers

```sql
CREATE TABLE customers (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name    VARCHAR(100)  NOT NULL,
    last_name     VARCHAR(100)  NOT NULL,
    email         VARCHAR(255),
    phone         VARCHAR(50),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city          VARCHAR(100),
    country       VARCHAR(100),
    postal_code   VARCHAR(20),
    notes         TEXT,
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ
);
```

### 4.6 orders

```sql
CREATE TABLE orders (
    id              UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number    VARCHAR(50)    NOT NULL,
    customer_id     UUID           REFERENCES customers(id) ON DELETE SET NULL,
    status          VARCHAR(50)    NOT NULL CHECK (status IN (
                        'PENDING','CONFIRMED','PROCESSING',
                        'SHIPPED','DELIVERED','CANCELLED','REFUNDED'
                    )),
    subtotal        DECIMAL(19,4)  NOT NULL DEFAULT 0,
    discount        DECIMAL(19,4)  NOT NULL DEFAULT 0,
    shipping_fee    DECIMAL(19,4)  NOT NULL DEFAULT 0,
    tax             DECIMAL(19,4)  NOT NULL DEFAULT 0,
    total           DECIMAL(19,4)  NOT NULL DEFAULT 0,
    notes           TEXT,
    shipped_at      TIMESTAMPTZ,
    delivered_at    TIMESTAMPTZ,
    cancelled_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);
```

| Column | Type | Notes |
|--------|------|-------|
| `order_number` | VARCHAR(50) | Human-readable (e.g., ORD-2026-001) |
| `status` | VARCHAR(50) | See order status state machine |
| `total` | DECIMAL(19,4) | subtotal - discount + shipping_fee + tax |

### 4.7 order_items

```sql
CREATE TABLE order_items (
    id           UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID           NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id   UUID           REFERENCES products(id) ON DELETE SET NULL,
    product_name VARCHAR(255)   NOT NULL,
    product_sku  VARCHAR(100)   NOT NULL,
    quantity     INTEGER        NOT NULL CHECK (quantity > 0),
    unit_price   DECIMAL(19,4)  NOT NULL CHECK (unit_price >= 0),
    discount     DECIMAL(19,4)  NOT NULL DEFAULT 0,
    total        DECIMAL(19,4)  NOT NULL,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);
```

**Note**: `product_name` and `product_sku` are intentionally denormalized — product data at time of order must be preserved even if the product is later modified.

### 4.8 inventory

```sql
CREATE TABLE inventory (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id        UUID         NOT NULL UNIQUE REFERENCES products(id) ON DELETE CASCADE,
    quantity          INTEGER      NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reserved_quantity INTEGER      NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    low_stock_threshold INTEGER    NOT NULL DEFAULT 10,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

| Column | Type | Notes |
|--------|------|-------|
| `quantity` | INTEGER | Total stock on hand |
| `reserved_quantity` | INTEGER | Held for confirmed orders not yet shipped |
| `low_stock_threshold` | INTEGER | Alert triggers when `quantity <= threshold` |

### 4.9 inventory_movements

```sql
CREATE TABLE inventory_movements (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id   UUID         NOT NULL REFERENCES products(id),
    user_id      UUID         REFERENCES users(id) ON DELETE SET NULL,
    type         VARCHAR(50)  NOT NULL CHECK (type IN (
                     'PURCHASE', 'SALE', 'ADJUSTMENT', 'RETURN', 'DAMAGE'
                 )),
    quantity     INTEGER      NOT NULL,
    before_qty   INTEGER      NOT NULL,
    after_qty    INTEGER      NOT NULL,
    reason       TEXT,
    reference_id UUID,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

| Column | Type | Notes |
|--------|------|-------|
| `quantity` | INTEGER | Positive = stock in, Negative = stock out |
| `reference_id` | UUID | Nullable — references order_id or import_job_id |

### 4.10 conversation_sessions

```sql
CREATE TABLE conversation_sessions (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(255),
    archived    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

### 4.11 conversation_messages

```sql
CREATE TABLE conversation_messages (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id   UUID         NOT NULL REFERENCES conversation_sessions(id) ON DELETE CASCADE,
    role         VARCHAR(20)  NOT NULL CHECK (role IN ('USER','ASSISTANT','TOOL_CALL','TOOL_RESULT')),
    content      TEXT         NOT NULL,
    tool_name    VARCHAR(100),
    tool_args    JSONB,
    token_count  INTEGER,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

### 4.12 audit_logs

```sql
CREATE TABLE audit_logs (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID          REFERENCES users(id) ON DELETE SET NULL,
    action       VARCHAR(100)  NOT NULL,
    entity_type  VARCHAR(100),
    entity_id    UUID,
    old_value    JSONB,
    new_value    JSONB,
    ip_address   VARCHAR(45),
    user_agent   TEXT,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
```

### 4.13 system_settings

```sql
CREATE TABLE system_settings (
    key         VARCHAR(100)   PRIMARY KEY,
    value       TEXT           NOT NULL,
    description TEXT,
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_by  UUID           REFERENCES users(id) ON DELETE SET NULL
);
```

**Example settings keys:**
- `ai.provider` → `openai`
- `ai.model` → `gpt-4o-mini`
- `ai.fallback_provider` → `gemini`
- `inventory.default_low_stock_threshold` → `10`

---

## 5. Relationships

| Parent Entity | Child Entity | Cardinality | FK Column | Delete Rule |
|--------------|--------------|-------------|-----------|-------------|
| users | refresh_tokens | 1:N | `user_id` | CASCADE |
| users | conversation_sessions | 1:N | `user_id` | CASCADE |
| categories | categories | 1:N (self) | `parent_id` | RESTRICT |
| categories | products | 1:N | `category_id` | SET NULL |
| customers | orders | 1:N | `customer_id` | SET NULL |
| products | order_items | 1:N | `product_id` | SET NULL |
| orders | order_items | 1:N | `order_id` | CASCADE |
| products | inventory | 1:1 | `product_id` | CASCADE |
| products | inventory_movements | 1:N | `product_id` | RESTRICT |
| conversation_sessions | conversation_messages | 1:N | `session_id` | CASCADE |

---

## 6. Entity Relationship Diagram

```
users ──────────────────────────── refresh_tokens
  │ (1:N via user_id)
  │
  ├──────────────────────────────── conversation_sessions
  │                                      │ (1:N via session_id)
  │                                      └── conversation_messages
  │
  └──────────────────────────────── audit_logs


categories ─────(parent_id, self-ref)──── categories
     │ (1:N via category_id)
     └────────────────────────────── products
                                        │ (1:1 via product_id)
                                        ├── inventory ─── inventory_movements
                                        │
                                        └── order_items ─── orders ─── customers
```

---

## 7. Indexes

### 7.1 Primary Indexes (Auto-created on PK)

All `id` columns are primary keys and have B-tree indexes automatically.

### 7.2 Unique Indexes

```sql
CREATE UNIQUE INDEX uq_users_email     ON users(email) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_products_sku    ON products(sku) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_categories_slug ON categories(slug) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_orders_number   ON orders(order_number);
CREATE UNIQUE INDEX uq_inventory_product ON inventory(product_id);
CREATE UNIQUE INDEX uq_rt_token_hash   ON refresh_tokens(token_hash);
```

### 7.3 Performance Indexes

```sql
-- User lookup
CREATE INDEX idx_users_role              ON users(role) WHERE deleted_at IS NULL;

-- Token lookup
CREATE INDEX idx_rt_user_id             ON refresh_tokens(user_id);
CREATE INDEX idx_rt_family_id           ON refresh_tokens(family_id);
CREATE INDEX idx_rt_expires_revoked     ON refresh_tokens(expires_at, revoked);

-- Product search
CREATE INDEX idx_products_category      ON products(category_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_active        ON products(active, deleted_at);
CREATE INDEX idx_products_name_trgm     ON products USING gin(name gin_trgm_ops);

-- Order analytics (most critical)
CREATE INDEX idx_orders_status          ON orders(status);
CREATE INDEX idx_orders_created_at      ON orders(created_at DESC);
CREATE INDEX idx_orders_customer        ON orders(customer_id);
CREATE INDEX idx_orders_status_date     ON orders(status, created_at);

-- Order items
CREATE INDEX idx_order_items_order      ON order_items(order_id);
CREATE INDEX idx_order_items_product    ON order_items(product_id);

-- Analytics aggregation
CREATE INDEX idx_orders_analytics       ON orders(created_at, status, total);

-- Inventory
CREATE INDEX idx_inv_mov_product_date   ON inventory_movements(product_id, created_at DESC);

-- AI conversations
CREATE INDEX idx_conv_sessions_user     ON conversation_sessions(user_id, created_at DESC);
CREATE INDEX idx_conv_messages_session  ON conversation_messages(session_id, created_at);

-- Audit logs
CREATE INDEX idx_audit_user_action      ON audit_logs(user_id, action, created_at DESC);
CREATE INDEX idx_audit_entity           ON audit_logs(entity_type, entity_id);
```

---

## 8. Constraints

### 8.1 Business Rule Constraints

| Table | Constraint | Rule |
|-------|-----------|------|
| `users` | `role IN (...)` | Only valid roles |
| `products` | `price >= 0` | Non-negative prices |
| `orders` | `status IN (...)` | Only valid statuses |
| `order_items` | `quantity > 0` | Minimum 1 unit |
| `order_items` | `unit_price >= 0` | Non-negative price |
| `inventory` | `quantity >= 0` | Stock cannot be negative |
| `inventory` | `reserved_quantity >= 0` | Cannot reserve negative |

### 8.2 Referential Integrity

All foreign keys are enforced at the database level. Delete rules are explicitly set per relationship (see §5).

### 8.3 Order Total Consistency

The `orders.total` column is always:
```
total = subtotal - discount + shipping_fee + tax
```
This is enforced in the application service layer (not as a DB trigger, for portability).

---

## 9. Migration Strategy

### 9.1 Flyway Configuration

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false
    validate-on-migrate: true
    out-of-order: false
    fail-on-missing-locations: true
```

### 9.2 Migration Rules

| Rule | Description |
|------|-------------|
| **Immutable** | Once a migration is applied, it MUST NOT be modified |
| **Sequential** | Version numbers must be sequential |
| **Atomic** | Each migration must be a complete, self-contained change |
| **Reversibility** | Document reversal steps in comments for critical migrations |
| **Data migrations** | Separate schema migrations from data migrations |

### 9.3 Migration File Plan

| Version | Description |
|---------|-------------|
| V1 | Enable extensions (pgcrypto, uuid-ossp, pg_trgm) |
| V2 | Create users, refresh_tokens tables |
| V3 | Create categories table |
| V4 | Create products table |
| V5 | Create customers table |
| V6 | Create orders, order_items tables |
| V7 | Create inventory, inventory_movements tables |
| V8 | Create conversation_sessions, conversation_messages |
| V9 | Create audit_logs, system_settings |
| V10 | Create all indexes |
| V11 | Seed default admin user, system settings |

---

## 10. Audit Strategy

### 10.1 Audit Fields on Entities

All entities that need change tracking have `created_at` and `updated_at` columns. These are auto-managed:

- `created_at`: Set on INSERT by `DEFAULT NOW()`
- `updated_at`: Updated on UPDATE via `@PreUpdate` in `BaseEntity.java`

### 10.2 Structured Audit Log

The `audit_logs` table captures significant events:

| Action Type | Example |
|-------------|---------|
| `USER_LOGIN` | User authenticated |
| `USER_LOGOUT` | User logged out |
| `USER_CREATED` | Admin created a new user |
| `USER_ROLE_CHANGED` | Admin changed user's role |
| `PRODUCT_CREATED` | Product added |
| `PRODUCT_DELETED` | Product soft-deleted |
| `ORDER_STATUS_CHANGED` | Order moved to SHIPPED |
| `IMPORT_COMPLETED` | CSV import finished |
| `EXPORT_GENERATED` | PDF export created |
| `AI_PROVIDER_CHANGED` | Admin switched LLM provider |

---

## 11. Soft Delete Strategy

### 11.1 Which Entities Use Soft Delete

| Entity | Soft Delete? | Reason |
|--------|-------------|--------|
| users | ✓ | Preserve audit history |
| products | ✓ | Preserve order history references |
| categories | ✓ | Preserve product references |
| customers | ✓ | Preserve order history |
| orders | ✗ | Orders are never deleted; only status changes |
| order_items | ✗ | Cascade from order |

### 11.2 Implementation Pattern

```java
// Conceptual (not code)
// BaseEntity has: UUID id, TIMESTAMPTZ createdAt, TIMESTAMPTZ updatedAt, TIMESTAMPTZ deletedAt

// All JPA queries automatically filter deleted records:
@Where(clause = "deleted_at IS NULL")   // on @Entity class
@SQLRestriction("deleted_at IS NULL")   // Spring Boot 3.x alternative
```

Soft delete operation:
```java
// Service layer
entity.setDeletedAt(Instant.now())
repository.save(entity)
```

---

## 12. Data Types Reference

| Concept | PostgreSQL Type | Java Type | Notes |
|---------|----------------|-----------|-------|
| Primary Key | UUID | `UUID` | `gen_random_uuid()` |
| Money | DECIMAL(19,4) | `BigDecimal` | Never use FLOAT for money |
| Timestamps | TIMESTAMPTZ | `Instant` | Always UTC |
| Enums | VARCHAR(50) | Java Enum | Stored as string |
| Long text | TEXT | `String` | Unbounded |
| Short strings | VARCHAR(N) | `String` | With defined max |
| JSON data | JSONB | `String` / DTO | Structured config, tool args |
| Counters | INTEGER | `Integer` | |
| Flags | BOOLEAN | `boolean` | |
