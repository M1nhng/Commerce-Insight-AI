# ADR-001: Modular Monolith over Microservices

**Status**: Accepted
**Date**: 2026-07-04

## Context

The Commerce Insight AI platform needs a backend architecture that is:
- Maintainable by a small team (1–3 developers)
- Fast to develop (portfolio timeline)
- Deployable as a single unit initially
- Extensible to microservices if the project grows

The main candidates were:
1. **Microservices** — maximum scalability, complex DevOps
2. **Standard Monolith** — fast development, poor maintainability at scale
3. **Modular Monolith** — structured monolith with clear module boundaries

## Decision

We adopt a **Modular Monolith** architecture.

Each business domain (`auth`, `user`, `product`, `order`, `analytics`, `importexport`, `ai`, `notification`) is implemented as a self-contained package under `com.commerceinsight` with its own controller, service, repository, domain, dto, and mapper layers.

**Strict rules enforce the module boundary:**
- No cross-module repository access
- Modules communicate only through service interfaces
- No business logic in controllers
- DTOs only at API boundaries (never entities)

## Consequences

**Positive:**
- Single deployable artifact — simple Docker setup
- Clear module boundaries — easy to extract to microservices later
- Fast development — no inter-service networking complexity
- Flyway manages the single database schema cleanly

**Negative:**
- All modules share the same database (acceptable at this scale)
- Scaling individual modules requires splitting the app (future work)
- Team must enforce module boundaries through code review
