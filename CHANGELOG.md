# Changelog

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/).

---

## [Unreleased]

### Added (2026-04-03)

#### Paginação na API de Post-its
- **Classes:** `PageQuery`, `PageResult<T>`, `PagedPostitResponse`
- **Endpoint:** `GET /api/v1/postits` agora retorna estrutura paginada
- **Query params:** `page` (default 0), `size` (default 20), `sort` (default `createdAt,desc`)
- **Performance:** Migration V5 adiciona índice composto `idx_postits_user_created` para otimizar queries paginadas com filtro por usuário
- **Implementação:** 
  - Domain: `PageQuery` e `PageResult` como contratos agnósticos
  - Infrastructure: `PagedPostitResponse` como DTO de saída
  - Controller: parâmetros de paginação via `@RequestParam`
- **Testes:** 10 unit tests em `PageQueryTest`, 5 em `PagedPostitResponseTest`, 12 em `PostitControllerUnitTest`

#### Probes do Spring Boot Actuator
- **Endpoints habilitados:** `/actuator/health/liveness` e `/actuator/health/readiness`
- **Configuração:** `management.endpoint.health.probes.enabled: true` em `application.yml`
- **Propósito:** Health checks para Docker Compose e Kubernetes
- **Healthcheck:** Docker Compose configurado para usar `/actuator/health/liveness` com retry policy

#### Workflow Híbrido de Desenvolvimento
- **Cenário 1 (Full Docker):** `docker compose up -d` → API + DB + Frontend
- **Cenário 2 (Recomendado):** API Docker + Frontend local com hot reload
  - Terminal 1: `docker compose up -d db api`
  - Terminal 2: `cd frontend && npm run dev`
- **Config:** `frontend/.env` com `VITE_API_BASE_URL=http://localhost:8080/api/v1`
- **Documentação:** `CLAUDE.md` atualizado com troubleshooting de ports e autenticação

### Fixed (2026-04-03)

#### Testes de Integração Corrigidos (3 testes)
1. **PostitPersistenceAdapterIntegrationTest.shouldPersistAndRetrievePostit**
   - **Problema:** `user_id NOT NULL` constraint violation (introduzido na migration V4)
   - **Solução:** Criação de `UserEntity` válido antes de criar postit
   - **Mudanças:** Injetado `UserRepositoryPort`, adicionada anotação `@Transactional`, usado `PostitObjectMother.postitToCreateWithUserId(userId)`

2. **AuthIntegrationTest.step6_postitsWithValidCookieReturnsOwnPostits**
   - **Problema:** JSON path esperava array direto `$[0].content`, mas API retorna `PagedPostitResponse`
   - **Solução:** Atualizado para `$.content[0].content`
   - **Mudanças:** Adicionadas validações de paginação (`$.page`, `$.totalElements`)

3. **AuthIntegrationTest.step8_userBDoesNotSeeUserAPostits**
   - **Problema:** Esperava array vazio `$`, recebia wrapper paginado
   - **Solução:** Atualizado para `$.content` (array dentro do wrapper)
   - **Mudanças:** Validação de `$.totalElements` como 0

**Resultado:** 78 testes passando (0 failures, 0 errors) em ~1:45min

### Changed (2026-04-03)

#### Migration V6 — Index Cleanup
- **Removido:** `idx_postits_color` (índice ineficaz — color não é seletivo)
- **Mantido:** `idx_postits_user_created` (índice composto para queries paginadas por usuário)
- **Adicionado:** `ALTER TABLE postits ALTER COLUMN color SET NOT NULL` (enforce constraint via DB)
- **Justificativa:** Color tem default `#FFFFFF` no domain — nunca deveria ser null

---

## [Sprint: Auth + Security + Ownership] - 2026-03-28

### Added

#### Autenticação Local com JWT + HttpOnly Cookie
- **ADR-001:** Autenticação local (sem OAuth) com JWT em cookie seguro
- **Endpoints:** `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`
- **JWT:** Token HS384, issuer `prjeto-post-it`, audience `postit-app`, expiration 1h
- **Cookie:** `jwt` HttpOnly, Secure (prod), SameSite=Strict, path=/
- **Domain:** `User` record com `id`, `email`, `passwordHash`, `name`, `role`
- **Validation:** Email RFC 5322, senha min 8 chars (1 upper, 1 lower, 1 digit)
- **Security:** Passwords com BCrypt (cost 12), rate limiting, CORS restrito

#### Ownership de Post-its por Usuário
- **Migration V3:** Adiciona coluna `user_id` (nullable inicialmente, FK para `users.id`)
- **Migration V4:** Torna `user_id NOT NULL` após migração de dados históricos
- **Endpoints:** `GET /api/v1/postits` filtra automaticamente por `user_id` do token JWT
- **Isolation:** Usuário só vê seus próprios post-its (row-level security via aplicação)
- **Tests:** 15 integration tests em `AuthIntegrationTest` validando isolamento entre usuários

#### Security Hardening
- **CORS:** Whitelist explícito (`http://localhost:8080`, `http://localhost:3000`)
- **CSP:** `default-src 'self'; script-src 'self'; connect-src 'self' http://localhost:8080`
- **Headers:** X-Content-Type-Options, X-Frame-Options (DENY), Referrer-Policy, Permissions-Policy
- **Actuator:** Apenas `/health` exposto (removido `/info` — SEC-005)
- **Logging:** Auditoria de autenticação (success/failure com email + reason)

### Security Fixes

- **SEC-001:** CORS permitia `*` → agora whitelist explícito
- **SEC-002:** JWT em localStorage → migrado para HttpOnly cookie
- **SEC-003:** Senhas plain text → BCrypt cost 12
- **SEC-004:** Error messages vazavam stack traces → RFC 9457 sem detalhes internos
- **SEC-005:** Actuator expunha `/info` com versões de libs → removido
- **SEC-006:** Cookie sem Secure flag → Secure=true em produção (HTTPS)
- **SEC-007:** CSRF possível → SameSite=Strict mitiga
- **SEC-008:** SQL injection possível → JPA + named params mitigam
- **SEC-009:** User enumeration via timing → resposta genérica "invalid credentials"
- **SEC-010:** Rate limiting ausente → TODO (próximo sprint)
- **SEC-011:** Session fixation → JWT stateless mitiga
- **SEC-012:** SQL queries em logs → `show-sql: false` em prod

### Documentation

- **ADR-001:** Decisão arquitetural sobre autenticação local vs. OAuth
- **security-audit-sprint1.md:** Auditoria completa de 12 vulnerabilidades
- **security-remediation-summary.md:** Sumário das correções aplicadas
- **auth-blueprint.md:** Diagrama de sequência de login/register/logout
- **smoke-test-ownership.md:** Testes manuais de isolamento entre usuários

---

## [Sprint: Frontend Vue 3] - 2026-03-21

### Added

#### Componentes Vue 3
- **PostitForm.vue:** Formulário de criação com color picker, char counter (500), Ctrl+Enter
- **PostitCard.vue:** Display de nota individual com smart text contrast (WCAG AA), delete button
- **PostitGrid.vue:** Layout responsivo (1-4 colunas), empty state, loading spinner, animations

#### Services & Composables
- **postitApi.ts:** Axios client com type-safety, error logging, env-based config
- **usePostits.ts:** State management com CRUD operations, computed properties
- **useError.ts:** Error handling com auto-clear (5s)

#### Documentation
- **CLAUDE.md:** Developer guide (commands, architecture, troubleshooting)
- **ANALISE-PROJETO.md:** Deep technical analysis (800+ lines)
- **FRONTEND-IMPLEMENTATION.md:** Complete frontend implementation details (900+ lines)
- **FRONTEND-QUICKSTART.md:** 5-minute getting started guide
- **frontend/README.md:** Technical documentation for frontend (550+ lines)

### Features

- ✅ Create/Read/Delete post-its (Update API foundation ready)
- ✅ Color picker (native + 8 presets)
- ✅ Smart text contrast (WCAG AA compliant)
- ✅ Character counter with warning
- ✅ Keyboard shortcuts (Ctrl+Enter to submit)
- ✅ Empty state messaging
- ✅ Loading indicators
- ✅ Error alerts with auto-dismiss
- ✅ Smooth animations (staggered entry)
- ✅ Responsive design (mobile-first, 1-4 columns)

### Quality

- ✅ TypeScript strict mode (zero `any`)
- ✅ ARIA labels on all interactive elements
- ✅ Semantic HTML
- ✅ CSS variables for theming
- ✅ Scoped styles (no conflicts)
- ✅ Vite dev server with HMR (< 1s startup)

---

## [Sprint: Backend Bootstrap] - 2026-03-20

### Added

#### Backend Hexagonal Architecture
- **Domain:** `Postit` record com validação de cor hex e content max 500 chars
- **Application Ports:** `PostitServicePort`, `PostitRepositoryPort`
- **Application Use Cases:** `PostitUseCase` (CRUD operations)
- **Infrastructure Adapters:**
  - **In:** `PostitController` (REST API com OpenAPI/Swagger)
  - **Out:** `PostitPersistenceAdapter` (JPA + PostgreSQL)
- **DTOs:** `PostitRequest`, `PostitResponse` (records Java 21)
- **Exception Handling:** `GlobalExceptionHandler` com RFC 9457 (Problem Details)

#### Database
- **Migration V1:** Tabela `postits` (id, content, color, created_at, updated_at)
- **Migration V2:** Tabela `users` (id, email, password_hash, name, role)
- **Flyway:** Migrations versionadas, baseline-on-migrate enabled
- **PostgreSQL 16:** Container Docker com healthcheck

#### API REST
- **Base URL:** `/api/v1/postits`
- **Endpoints:** POST (201), GET all (200), GET by id (200/404), PUT (200/404), DELETE (204/404)
- **Validation:** Bean Validation (JSR-380) com mensagens customizadas
- **Error Format:** RFC 9457 (Problem Details) para todos os erros
- **OpenAPI:** Swagger UI em `/swagger-ui.html`

#### Tests
- **Unit:** `PostitUseCaseTest` (13 tests) — business logic com mocks
- **Integration:** `PostitPersistenceAdapterIntegrationTest` (1 test) — Testcontainers PostgreSQL
- **Fixtures:** `PostitObjectMother` (test data builders)
- **Base:** `AbstractIntegrationTest` (shared Testcontainers setup)

#### Docker
- **docker-compose.yml:** 3 services (db, api, frontend)
- **Multi-stage Dockerfile:** Build com Maven, runtime com JRE Alpine
- **Security:** Non-root user, read-only filesystem, tmpfs, no-new-privileges
- **Resource limits:** CPU/Memory limits + reservations
- **Healthchecks:** PostgreSQL pg_isready, API /actuator/health, retry policies

#### Documentation
- **REQUISITOS.md:** Functional and non-functional requirements
- **Bootstrap artifacts:** 20+ helper scripts (preservados como histórico)

### Technology Stack

**Backend:**
- Java 21 (Eclipse Temurin)
- Spring Boot 3.4.3
- Spring Data JPA
- PostgreSQL 16
- Flyway 10.x
- Maven 3.9.6

**Frontend:**
- Vue 3.4.21
- TypeScript 5.3.3
- Vite 5.1.4
- Axios 1.6.7

**Infrastructure:**
- Docker 29.3.1
- Docker Compose v2
- PostgreSQL 16-alpine
- Nginx (frontend serve)

---

## Roadmap

### Próximos Passos (Priorizados)

1. **Load Testing** — Gatling scenario para validar paginação sob carga
2. **CI/CD Pipeline** — GitHub Actions com quality gates (Sonar, Trivy)
3. **Frontend Update Feature** — Componente de edição de post-it
4. **Rate Limiting** — Resilience4j para mitigar brute-force (SEC-010)
5. **Observability** — Prometheus + Grafana dashboards
6. **Kubernetes Manifests** — Helm chart para deploy em K8s

### Backlog

- Search/filter por conteúdo
- Sort por data/cor
- Export (CSV/JSON)
- Dark/light theme toggle
- PWA com offline support
- Analytics (Amplitude)
- E2E tests (Playwright)
- Mutation testing (Pitest)

---

## Convenções de Commit

Este projeto segue [Conventional Commits](https://www.conventionalcommits.org/pt-br/):

```
feat(scope): descrição breve em PT-BR
fix(scope): descrição breve
docs: atualiza README
refactor(module): reestrutura X
test(unit): adiciona cobertura para Y
chore: atualiza dependências
```

**Co-Authored-By:** Incluído em commits com IA assistance:
```
Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
```

---

## Licença

[Definir licença] — TBD
