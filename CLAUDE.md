# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**prjeto-post-it** is a high-performance Post-it note management system built with **Java 21 + Spring Boot 3.x** and **Vue.js**, deployed via Docker Compose. The system follows **Hexagonal Architecture** (Ports and Adapters) with strict separation of concerns, versioned database migrations (Flyway), and RFC 9457 error handling (Problem Details).

### Core Technologies
- **Backend:** Java 21 + Spring Boot 3.4.3, Hexagonal Architecture
- **Database:** PostgreSQL 16 (Alpine, containerized with persistent volumes)
- **Frontend:** Vue.js 3 + TypeScript + Vite
- **Infrastructure:** Docker & Docker Compose with health checks
- **API:** REST with OpenAPI 3.0 (Swagger UI at `/swagger-ui.html`)
- **Database Migrations:** Flyway (versioned SQL scripts)
- **Testing:** JUnit 5, AssertJ, Testcontainers, Object Mother pattern

## Workspace Layout

```
prjeto-post-it/
├── backend/                          # Java / Spring Boot application
│   ├── src/
│   │   ├── main/java/com/postit/
│   │   │   ├── domain/               # Business rules (records, no framework deps)
│   │   │   ├── application/
│   │   │   │   ├── ports/            # Inbound/Outbound interfaces
│   │   │   │   └── usecases/         # Application logic
│   │   │   ├── infrastructure/
│   │   │   │   ├── adapters/in/      # REST controllers, DTOs
│   │   │   │   ├── adapters/out/     # Persistence adapters, JPA entities
│   │   │   │   └── config/           # Spring configuration, exception handlers
│   │   │   └── shared/exception/     # Domain exceptions
│   │   ├── main/resources/
│   │   │   ├── application.yml       # Spring Boot configuration
│   │   │   └── db/migration/         # Flyway versioned migrations (V*.sql)
│   │   └── test/                     # JUnit 5 tests, Testcontainers, Object Mother
│   ├── pom.xml                       # Maven configuration
│   └── Dockerfile                    # Multi-stage build (Maven → JRE Alpine)
├── frontend/                         # Vue.js application
│   ├── src/
│   │   ├── App.vue                   # Root component
│   │   ├── main.ts                   # Entry point
│   │   └── components/               # Vue components
│   ├── package.json                  # npm dependencies (Vue 3, Axios, Vite)
│   ├── vite.config.ts                # Vite build configuration
│   ├── tsconfig.json                 # TypeScript configuration
│   ├── index.html                    # HTML entry point
│   ├── nginx.conf                    # Nginx reverse proxy config (container)
│   └── Dockerfile                    # Multi-stage build (Node → Nginx)
├── docker-compose.yml                # Orchestration: PostgreSQL, Spring Boot API, Vue frontend
├── REQUISITOS.md                     # Complete requirements (functional, non-functional)
├── GEMINI.md                         # Gemini CLI context (reference only)
└── CLAUDE.md                         # This file
```

## Quick Start

### Prerequisites
- Docker & Docker Compose (v2+)
- WSL2 (Windows) or Linux
- Maven wrapper (`./mvnw`) is included; no manual installation needed
- Node.js LTS (for local frontend development)

### Run the Full System (Docker Compose)

```bash
cd /home/mq/iGitHub/prjeto-post-it
docker compose up -d

# Verify health
docker compose ps
docker logs postit-api       # Check Flyway migrations applied
docker logs postit-db        # Database startup logs
docker logs postit-frontend  # Nginx startup
```

**Services:**
- **API:** http://localhost:8080 (health check at `/actuator/health`)
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Frontend:** http://localhost:3000
- **Database:** localhost:5432 (user=user, password=password)

### Cleanup

```bash
docker compose down              # Stop containers
docker compose down -v           # Stop containers + remove volumes (data loss)
docker system prune -a --volumes # Full cleanup
```

## Backend Development

### Local Backend Development (without Docker)

```bash
cd backend

# Build
./mvnw clean package              # Full build with tests
./mvnw clean package -DskipTests  # Skip tests (faster)

# Run locally (requires PostgreSQL running on localhost:5432)
./mvnw spring-boot:run

# Run tests
./mvnw test                       # Unit tests only
./mvnw verify                     # All tests (unit + integration with Testcontainers)

# Single test
./mvnw test -Dtest=PostitUseCaseTest
./mvnw test -Dtest=PostitPersistenceAdapterIntegrationTest

# Code quality
./mvnw clean install             # Full build + install
```

### Hexagonal Architecture — Structure

The backend strictly follows the Hexagonal Architecture pattern:

| Layer | Location | Responsibility |
|-------|----------|-----------------|
| **Domain** | `domain/` | Business rules, records (no framework imports) |
| **Ports (Inbound)** | `application/ports/PostitServicePort.java` | Use case interface |
| **Ports (Outbound)** | `application/ports/PostitRepositoryPort.java` | Persistence interface |
| **Use Cases** | `application/usecases/PostitUseCase.java` | Application logic (implements inbound port) |
| **Adapters In** | `infrastructure/adapters/in/PostitController.java` | REST endpoints (HTTP adapter) |
| **Adapters Out** | `infrastructure/adapters/out/PostitPersistenceAdapter.java` | JPA repository implementation |
| **Config** | `infrastructure/config/` | Spring beans, exception handlers, OpenAPI |

**Key files:**
- `domain/Postit.java` — Domain record (no JPA annotations)
- `application/usecases/PostitUseCase.java` — Business logic implementation
- `infrastructure/adapters/in/PostitController.java` — REST layer (`/api/v1/postits`)
- `infrastructure/adapters/out/PostitPersistenceAdapter.java` — Implements `PostitRepositoryPort`
- `infrastructure/config/GlobalExceptionHandler.java` — RFC 9457 error responses

### API Contracts (REST)

All endpoints are prefixed with `/api/v1/postits`.

| Method | Endpoint | Status | Description |
|--------|----------|--------|-------------|
| **POST** | `/api/v1/postits` | 201 | Create note (Location header) |
| **GET** | `/api/v1/postits` | 200 | List all notes |
| **GET** | `/api/v1/postits/{id}` | 200 | Get single note |
| **PUT** | `/api/v1/postits/{id}` | 200/204 | Update note |
| **DELETE** | `/api/v1/postits/{id}` | 204 | Delete note |

**Error Format (RFC 9457):**
```json
{
  "type": "https://api.postits.local/errors/not-found",
  "title": "Post-it não encontrado",
  "status": 404,
  "detail": "Não foi possível encontrar uma nota com o ID: 999",
  "instance": "/api/v1/postits/999"
}
```

### Database Migrations (Flyway)

Migrations are versioned SQL files in `backend/src/main/resources/db/migration/`:

```
db/migration/
├── V1__create_postit_table.sql    # Initial schema
├── V2__add_index_on_color.sql     # Indexed column
└── V3__add_description_column.sql # New feature
```

**Rules:**
- Always create NEW migration files; never edit applied migrations
- Naming: `V{number}__{description}.sql` (e.g., `V2__add_indexes.sql`)
- For schema changes: use `ALTER TABLE`, not `CREATE TABLE IF NOT EXISTS`
- Flyway baseline is enabled; can start from existing schema

**Adding a migration:**
```bash
# Create new file in backend/src/main/resources/db/migration/
# e.g., V2__add_category_column.sql
ALTER TABLE postits ADD COLUMN category VARCHAR(50) DEFAULT 'general';

# Run next time application starts or manually:
./mvnw flyway:migrate
```

### Testing Strategy

The project uses **three layers of testing:**

| Layer | Tool | Location | Run |
|-------|------|----------|-----|
| **Unit Tests** | JUnit 5 + AssertJ + Mockito | `src/test/java/.../test/` | `mvn test` |
| **Integration Tests** | Testcontainers (PostgreSQL) | `src/test/java/...IntegrationTest.java` | `mvn verify` |
| **Object Mother** | Custom fixtures | `src/test/java/.../PostitObjectMother.java` | Used in tests |

**Example test structure (Given-When-Then):**
```java
@DisplayName("Should update postit when ID exists")
void shouldUpdatePostit_whenIdExists() {
    // Given
    Postit existing = PostitObjectMother.validPostit();

    // When
    Postit updated = useCase.update(existing.id(), newContent);

    // Then
    assertThat(updated.content()).isEqualTo(newContent);
}
```

**Integration test with Testcontainers:**
```java
@Testcontainers
class PostitPersistenceAdapterIntegrationTest extends AbstractIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    // Tests run against real PostgreSQL container
}
```

## Frontend Development

### Local Frontend Development (without Docker)

```bash
cd frontend

# Install dependencies
npm install

# Dev server (HMR enabled, http://localhost:5173)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

### Frontend Architecture

- **Framework:** Vue 3 (Composition API recommended)
- **Build Tool:** Vite 5 (fast refresh, TypeScript support)
- **HTTP Client:** Axios for API calls
- **UI Components:** Lucide Vue icons
- **Styling:** TBD (currently configured for custom CSS or Tailwind)

**API Integration:**
```typescript
// Example: Call backend API
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1'
});

// Fetch postits
api.get('/postits').then(response => {
  // Handle response
});
```

## Docker & Deployment

### Multi-Stage Build Strategy

Both `backend/Dockerfile` and `frontend/Dockerfile` use multi-stage builds to minimize image size:

**Backend:**
- **Stage 1 (Build):** Maven 3.9.6 + JDK 21 → compile & package
- **Stage 2 (Runtime):** Eclipse Temurin 21 JRE Alpine → run JAR (non-root user)

**Frontend:**
- **Stage 1 (Build):** Node 20 → build Vue + Vite
- **Stage 2 (Runtime):** Nginx Alpine → serve static files

### Docker Compose Orchestration

The system is defined in `docker-compose.yml` with:
- **PostgreSQL 16 Alpine:** Persistent volume (`postit_data`), health checks
- **Spring Boot API:** Multi-stage build, depends on db health
- **Vue Frontend:** Multi-stage build, depends on API health
- **Shared Network:** `postit-network` (bridge driver)

**Health checks ensure readiness:**
- **DB:** `pg_isready -U user -d postit_db`
- **API:** `wget --quiet --tries=1 --spider http://localhost:8080/actuator/health`
- **Frontend:** Port 3000 available

### Environment Variables

The system uses environment variables for configuration (never hardcoded secrets):

| Variable | Default | Usage |
|----------|---------|-------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/postit_db` | Backend → PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `user` | PostgreSQL auth |
| `SPRING_DATASOURCE_PASSWORD` | `password` | PostgreSQL auth |
| `SPRING_FLYWAY_URL` | (same as datasource) | Flyway migration URL |
| `SPRING_FLYWAY_USER` | (same as datasource) | Flyway auth |
| `SPRING_FLYWAY_PASSWORD` | (same as datasource) | Flyway auth |

**Never commit `.env` with secrets.** Use `.env.example` to document required variables.

## Code Style & Conventions

### Java / Spring Boot

- **Language Version:** Java 21 (LTS) — leveraging records and virtual threads
- **Package Structure:**
  - `com.postit.domain` — Business rules
  - `com.postit.application.ports` — Port interfaces
  - `com.postit.application.usecases` — Use case implementations
  - `com.postit.infrastructure.adapters.in` — HTTP adapters
  - `com.postit.infrastructure.adapters.out` — Persistence adapters
  - `com.postit.infrastructure.config` — Spring configuration
  - `com.postit.shared.exception` — Domain exceptions

- **Naming Conventions:**
  - Classes: `PascalCase` (e.g., `PostitController`, `PostitUseCase`)
  - Methods: `camelCase` (e.g., `createPostit`, `listAllPostits`)
  - Constants: `UPPER_SNAKE_CASE`
  - DTOs: `{Entity}Request`, `{Entity}Response` (e.g., `PostitRequest`, `PostitResponse`)
  - Exceptions: `{Rule}Exception`, `{Resource}NotFoundException`

- **Domain Model:** Use Java records where possible; keep domain entities free of JPA annotations
- **Error Handling:** All REST exceptions converted to RFC 9457 Problem Details by `GlobalExceptionHandler`
- **Flyway Migrations:** Always versioned; never edit applied migrations
- **Logging:** SLF4J via Spring Boot defaults (configure in `application.yml`)

### TypeScript / Vue 3

- **Language Version:** TypeScript 5.3.3
- **Framework:** Vue 3 with Composition API (preferred over Options API)
- **Build Tool:** Vite 5.1.4
- **Naming:**
  - Components: `PascalCase` (e.g., `PostitCard.vue`, `NoteList.vue`)
  - Functions: `camelCase` (e.g., `fetchPostits`, `deleteNote`)
  - Constants: `UPPER_SNAKE_CASE`
  - Composables: `use*` prefix (e.g., `usePostitApi.ts`)

- **HTTP Requests:** Axios with centralized API client
- **State Management:** TBD (Vue Pinia recommended if complex state needed)
- **Styling:** TBD (Tailwind CSS or CSS modules)

### Git & Commits

**Commit Message Format (Conventional Commits):**
```
type(scope): descrição breve (máx 70 chars, imperativo, sem ponto final)

Explicação opcional do quê/por quê em português.
- Use bullets para múltiplas mudanças
- Explique "por quê", não só "o quê"

Closes #issue_number (se aplicável)
```

**Types:** `feat` | `fix` | `docs` | `refactor` | `perf` | `test` | `chore` | `ci`

**Examples:**
```
feat(api): adiciona endpoint de busca por cor

Permite filtrar postits por cor hexadecimal.
Melhora performance com índice PostgreSQL.

Closes #42
```
```
test(persistence): adiciona testes de integração com Testcontainers
```

## Common Development Tasks

### Running a Single Test
```bash
cd backend

# Run specific test class
./mvnw test -Dtest=PostitUseCaseTest

# Run specific test method
./mvnw test -Dtest=PostitUseCaseTest#shouldCreatePostit

# Run integration tests only
./mvnw verify -Dtest=*IntegrationTest
```

### Adding a New Feature (Hexagonal Style)

1. **Define Domain Model** in `domain/`:
   - Add new record or class without framework dependencies

2. **Define Port** in `application/ports/`:
   - Create `NewFeaturePort.java` interface if new capability needed

3. **Implement Use Case** in `application/usecases/`:
   - Implement the port, inject `PostitRepositoryPort` as needed

4. **Create Adapter In** in `infrastructure/adapters/in/`:
   - REST controller exposing the use case

5. **Create Adapter Out** (if needed) in `infrastructure/adapters/out/`:
   - Repository implementation or external service adapter

6. **Write Tests:**
   - Unit tests for use case (mocked ports)
   - Integration tests for persistence adapter (Testcontainers)
   - E2E tests for controller (MockMvc or Testcontainers)

### Adding a Database Migration

```bash
cd backend/src/main/resources/db/migration

# Create new migration file
cat > V2__add_new_column.sql << 'EOF'
ALTER TABLE postits ADD COLUMN new_field VARCHAR(100);
CREATE INDEX idx_postits_new_field ON postits(new_field);
EOF

# Next application startup will apply it
```

### Debugging Backend

```bash
# Enable debug logging
SPRING_PROFILES_ACTIVE=debug ./mvnw spring-boot:run

# Or add to application.yml
# logging:
#   level:
#     com.postit: DEBUG
#     org.springframework.web: DEBUG
```

### Debugging Frontend

```bash
# Dev server has built-in source maps + hot reload
npm run dev

# Open browser DevTools (F12) → Vue DevTools extension
# Sources tab shows original TypeScript, not minified code
```

## Troubleshooting

### PostgreSQL Connection Issues
```bash
# Check if container is healthy
docker compose ps
docker logs postit-db

# Manually test connection
docker exec postit-db psql -U user -d postit_db -c "SELECT 1"
```

### Spring Boot Startup Fails
```bash
# Check logs for Flyway errors
docker logs postit-api

# Verify migrations applied
docker exec postit-db psql -U user -d postit_db \
  -c "SELECT installed_rank, version, description FROM flyway_schema_history"
```

### Frontend Cannot Reach API
```bash
# Check if backend is healthy
curl http://localhost:8080/actuator/health

# Check CORS headers (may need configuration)
docker logs postit-frontend
```

### Port Already in Use
```bash
# Kill process on port 8080 (macOS/Linux)
lsof -ti:8080 | xargs kill -9

# Kill process on port 3000
lsof -ti:3000 | xargs kill -9
```

## Performance Notes

- **Latency Target:** GET `/api/v1/postits` < 100ms (local)
- **Database Index:** `postits(color)` indexed for quick filtering
- **Java 21 Benefits:** Virtual threads for async operations (if implemented), records for reduced GC pressure
- **PostgreSQL Alpine:** Lightweight image (smaller memory footprint than full PostgreSQL)
- **Frontend Build:** Vite + Vue 3 tree-shaking produces minimal bundle size

## Next Steps for Development

1. **Complete Frontend UI** (Vue 3 components for CRUD operations)
2. **Add API Integration Tests** (contract tests with Pact if consuming other APIs)
3. **Implement Pagination** (GET `/api/v1/postits?page=0&size=10`)
4. **Add Authentication** (Spring Security + JWT if required)
5. **Performance Testing** (k6 or Gatling for load testing)
6. **Security Audit** (OWASP Top 10 validation, input sanitization)

## References

- **Hexagonal Architecture:** `domain/` and `infrastructure/` separation ensures business logic is framework-agnostic
- **RFC 9457:** https://tools.ietf.org/html/rfc9457 (Problem Details specification)
- **Spring Boot Docs:** https://spring.io/projects/spring-boot
- **Vue 3 Docs:** https://vuejs.org/guide/
- **PostgreSQL:** https://www.postgresql.org/docs/
- **Flyway:** https://flywaydb.org/documentation
- **Docker Compose:** https://docs.docker.com/compose/
