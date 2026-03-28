# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**prjeto-post-it** — Post-it note management system built with Java 21 + Spring Boot 3.4.3 (backend) and Vue 3 + TypeScript (frontend), deployed via Docker Compose with PostgreSQL 16.

## Commands

### Full System (Docker Compose)
```bash
docker compose up -d                         # Start all services (db → api → frontend)
docker compose down                          # Stop
docker compose down -v                       # Stop + delete volumes (data loss)
docker logs postit-api                       # Check Flyway migrations + startup
```

**Services:** API → http://localhost:8080 | Swagger → http://localhost:8080/swagger-ui.html | Frontend → http://localhost:3000

### Backend (Java / Maven)
```bash
cd backend
./mvnw clean package                         # Build with tests
./mvnw clean package -DskipTests             # Build without tests
./mvnw spring-boot:run                       # Run locally (needs PostgreSQL on localhost:5432)
./mvnw test                                  # Unit tests only
./mvnw verify                                # All tests including Testcontainers integration tests
./mvnw test -Dtest=PostitUseCaseTest         # Single test class
./mvnw test -Dtest=PostitUseCaseTest#shouldCreatePostit  # Single test method
```

### Frontend (Vue 3 / Vite)
```bash
cd frontend
npm install
npm run dev       # Dev server with HMR at http://localhost:5173
npm run build     # Production build
```

## Architecture

### Hexagonal Architecture (Backend)

```
domain/           → Postit.java (Java record, zero framework deps, self-validates)
application/
  ports/          → PostitServicePort (inbound), PostitRepositoryPort (outbound)
  usecases/       → PostitUseCase (implements ServicePort, injects RepositoryPort)
infrastructure/
  adapters/in/    → PostitController (REST), PostitRequest/Response (DTOs)
  adapters/out/   → PostitPersistenceAdapter (implements RepositoryPort via JPA)
  config/         → Spring beans, GlobalExceptionHandler (RFC 9457), OpenAPI
shared/exception/ → PostitNotFoundException
```

**Flow:** `Controller → UseCase → RepositoryPort → PersistenceAdapter → JPA`

The domain record `Postit` validates itself in the compact constructor (blank content, hex color format). Use `Postit.create(content, color)` for new instances and `postit.withId(id)` for updates.

### Frontend Architecture

```
services/postitApi.ts   → Axios client (baseURL from VITE_API_BASE_URL or localhost:8080/api/v1)
composables/usePostits.ts  → Reactive state + CRUD operations
composables/useError.ts    → Error handling composable
components/
  PostitCard.vue          → Single note display
  PostitForm.vue          → Create/edit form
  PostitGrid.vue          → Grid layout
```

### REST API Contracts

All endpoints at `/api/v1/postits`:

| Method | Endpoint | Status |
|--------|----------|--------|
| POST | `/api/v1/postits` | 201 + Location header |
| GET | `/api/v1/postits` | 200 |
| GET | `/api/v1/postits/{id}` | 200 / 404 |
| PUT | `/api/v1/postits/{id}` | 200 / 404 |
| DELETE | `/api/v1/postits/{id}` | 204 / 404 |

Errors follow RFC 9457 (Problem Details). `GlobalExceptionHandler` converts all exceptions.

### Database Migrations (Flyway)

Files in `backend/src/main/resources/db/migration/` — naming: `V{n}__{description}.sql`.

Current: `V1__create_postit_table.sql` (schema + `idx_postits_color` index).

**Rule: never edit an applied migration. Always create a new `V{n+1}__...sql` file.**

### Testing

| Layer | File | What it tests |
|-------|------|---------------|
| Unit | `PostitUseCaseTest` | Business logic with mocked `PostitRepositoryPort` |
| Integration | `PostitPersistenceAdapterIntegrationTest` | Real PostgreSQL via Testcontainers |
| Fixtures | `PostitObjectMother` | Pre-built `Postit` instances for tests |
| Base | `AbstractIntegrationTest` | Shared Testcontainers setup |

`mvn test` runs unit tests only. `mvn verify` runs all tests including Testcontainers (requires Docker).

## Key Constraints

- **Domain records are immutable** — updates return new instances via `withId()`, never mutate
- **Color field** accepts hex format only (`#RRGGBB`), default `#FFFFFF` — validated in domain
- **Frontend API base URL** configurable via `VITE_API_BASE_URL` env var (useful for Docker vs local dev)
- **Bootstrap scripts** (`bootstrap*.py`, `*.sh` in root) are project scaffolding artifacts — ignore them
