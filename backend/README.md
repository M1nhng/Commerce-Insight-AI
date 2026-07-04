# Backend — Commerce Insight AI

> Spring Boot 3.5 · Java 17 · PostgreSQL · Modular Monolith

---

## Overview

This module contains the **REST API backend** for Commerce Insight AI. It is implemented as a **Modular Monolith** — a single deployable Spring Boot application organized into strict, self-contained domain modules.

Each domain module is isolated with its own layers. No cross-module repository access is allowed; modules communicate through service interfaces only.

---

## Project Structure

```
backend/
└── src/
    ├── main/
    │   ├── java/com/commerceinsight/
    │   │   ├── CommerceInsightApplication.java   # Spring Boot entry point
    │   │   ├── config/                           # Spring configuration classes
    │   │   ├── exception/                        # Global exception handler
    │   │   ├── security/                         # JWT filter, UserDetails, token util
    │   │   │
    │   │   ├── auth/                             # Domain: Authentication & JWT
    │   │   │   ├── controller/
    │   │   │   ├── service/
    │   │   │   ├── repository/
    │   │   │   ├── domain/
    │   │   │   ├── dto/
    │   │   │   │   ├── request/
    │   │   │   │   └── response/
    │   │   │   └── mapper/
    │   │   │
    │   │   ├── user/                             # Domain: User management
    │   │   ├── product/                          # Domain: Product catalog
    │   │   ├── order/                            # Domain: Order management
    │   │   ├── analytics/                        # Domain: KPIs & dashboards
    │   │   ├── importexport/                     # Domain: CSV/Excel import-export
    │   │   ├── ai/                               # Domain: AI insight generation
    │   │   ├── notification/                     # Domain: Notifications
    │   │   └── shared/                           # Common utilities, base classes
    │   │
    │   └── resources/
    │       ├── application.yml                   # Base configuration
    │       ├── application-dev.yml               # Dev profile overrides
    │       ├── application-prod.yml              # Prod profile overrides
    │       └── db/migration/                     # Flyway SQL migrations
    │           ├── V1__init_schema.sql
    │           ├── V2__seed_data.sql
    │           └── V3__indexes.sql
    │
    └── test/
        └── java/com/commerceinsight/            # Integration & unit tests
```

---

## Architecture Rules

| Rule | Description |
|------|-------------|
| **No entity exposure** | Entities are never returned to the frontend. DTOs only. |
| **DTO only** | Every API response uses a response DTO. |
| **Mapper only** | All entity↔DTO conversions go through MapStruct mappers. |
| **Service only** | Business logic lives exclusively in the service layer. |
| **No cross-repo** | Modules cannot access another module's repository. |
| **No logic in controller** | Controllers handle HTTP only — delegate everything to services. |

---

## Domain Modules

| Module | Package | Responsibility |
|--------|---------|---------------|
| `auth` | `com.commerceinsight.auth` | JWT login, register, token refresh |
| `user` | `com.commerceinsight.user` | User CRUD, profiles, role management |
| `product` | `com.commerceinsight.product` | Product catalog, categories, SKUs |
| `order` | `com.commerceinsight.order` | Order creation, line items, status |
| `analytics` | `com.commerceinsight.analytics` | KPIs, revenue charts, trend reports |
| `importexport` | `com.commerceinsight.importexport` | CSV/Excel import & export |
| `ai` | `com.commerceinsight.ai` | AI prompt processing, insight storage |
| `notification` | `com.commerceinsight.notification` | In-app & email notification dispatch |
| `shared` | `com.commerceinsight.shared` | DTOs, exceptions, base entities, utils |

---

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.5.x | Framework |
| Java | 17 | Language |
| PostgreSQL | 16 | Primary database |
| Flyway | 10.x | Schema migration |
| Spring Security | 6.x | Security framework |
| JJWT | 0.12.x | JWT tokens |
| MapStruct | 1.6.x | DTO mapping |
| Lombok | latest | Boilerplate reduction |
| Jakarta Validation | 3.x | Input validation |
| Maven | 3.9+ | Build tool |

---

## Running Locally

### Prerequisites
- Java 17+
- Docker (for PostgreSQL)
- Maven 3.9+

### Steps

```bash
# 1. Start PostgreSQL
docker-compose up -d postgres

# 2. Run with dev profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

API will be available at: `http://localhost:8080`

Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## Configuration

All environment-specific configuration is in:
- `src/main/resources/application-dev.yml` — local development
- `src/main/resources/application-prod.yml` — production (secrets via env vars)

**Never commit** real credentials. Use environment variables in production.

---

## Database Migrations

Flyway runs automatically on startup.

```bash
# Run migrations manually via helper script
../scripts/migrate.sh
```

Migration files follow the naming convention: `V{n}__{description}.sql`

---

## Testing

```bash
# Run all tests
./mvnw test

# Run with coverage report
./mvnw verify
```

See [12_TESTING.md](../docs/12_TESTING.md) for the full testing strategy.

---

## Status

🚧 **Structure initialized** — No business logic implemented yet.
