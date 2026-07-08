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
- Docker Desktop (for PostgreSQL)
- Maven 3.9+ **or** use the IntelliJ bundled Maven

### Step 1 — Start PostgreSQL

```bash
docker-compose up -d postgres
```

> PostgreSQL will be available at `localhost:5050`  
> Database: `commerce_insight_dev` | User: `postgres` | Password: `postgres`

### Step 2 — Run the Application

**Windows — PowerShell:**
```powershell
# Set JAVA_HOME to your JDK 17 path, then run:
$env:JAVA_HOME = "D:\Folder_phan_mem\jdk17"   # adjust to your actual JDK path
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

**Windows — CMD:**
```cmd
set JAVA_HOME=D:\Folder_phan_mem\jdk17
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

**Linux / macOS:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

> 💡 **Tip:** Set `JAVA_HOME` permanently via Windows System Properties → Environment Variables  
> to avoid specifying it every time.
>
> **No Maven installation needed** — the Maven Wrapper (`mvnw.cmd`) downloads  
> Maven 3.9.11 automatically on first run.

### Step 3 — Verify

```
API:        http://localhost:8080
Swagger UI: http://localhost:8080/swagger-ui.html
Health:     http://localhost:8080/actuator/health
```

**Default admin credentials:**
```
Email:    admin@commerceinsight.ai
Password: Admin@123456
```

---

## Configuration

All environment-specific configuration is in:
- `src/main/resources/application.yml` — shared defaults (env-var overrides)
- `src/main/resources/application-dev.yml` — local development overrides
- `src/main/resources/application-prod.yml` — production (secrets via env vars)

**Never commit** real credentials. Use environment variables in production.

Key env vars for production:

| Variable | Description |
|---|---|
| `DB_HOST` | PostgreSQL host |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | Min 64-char alphanumeric string |
| `MCP_API_KEY` | MCP server shared secret |

---

## Database Migrations

Flyway runs automatically on startup. Migrations are in `src/main/resources/db/migration/`.

```powershell
# Repair checksum after editing a migration (dev only)
mvn flyway:repair -Dflyway.url=jdbc:postgresql://localhost:5432/commerce_insight_dev `
    -Dflyway.user=postgres -Dflyway.password=postgres
```

Migration naming convention: `V{n}__{description}.sql`

---

## Testing

**Windows:**
```powershell
# Run all tests
.\mvnw.cmd test

# Run with coverage
.\mvnw.cmd verify
```

**Linux / macOS:**
```bash
./mvnw test
./mvnw verify
```

See [12_TESTING.md](../docs/12_TESTING.md) for the full testing strategy.

---

## Status

✅ **Sprint 3 complete** — Foundation + Authentication module implemented and tested.

| Module | Status |
|---|---|
| Spring Boot Foundation | ✅ Done |
| JWT Authentication | ✅ Done |
| Flyway Migrations (V1–V11) | ✅ Done |
| Unit + Integration Tests (19/19) | ✅ Passing |
| Docker + docker-compose | ✅ Done |
