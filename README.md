# Post-it

Sistema de gerenciamento de notas estilo post-it com autenticação, ownership e paginação.

![Status](https://img.shields.io/badge/status-active-success)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen)
![Vue](https://img.shields.io/badge/Vue-3.4.21-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)

---

## 📋 Índice

- [Sobre](#sobre)
- [Features](#features)
- [Stack Tecnológico](#stack-tecnológico)
- [Arquitetura](#arquitetura)
- [Quick Start](#quick-start)
- [Desenvolvimento](#desenvolvimento)
- [Testes](#testes)
- [API Documentation](#api-documentation)
- [Segurança](#segurança)
- [Documentação Técnica](#documentação-técnica)
- [Roadmap](#roadmap)
- [Licença](#licença)

---

## 🎯 Sobre

**Post-it** é uma aplicação web full-stack para gerenciamento de notas estilo post-it com:

- ✅ **Autenticação local** com JWT em HttpOnly cookie
- ✅ **Ownership** — cada usuário vê apenas suas próprias notas
- ✅ **Paginação eficiente** com índices otimizados
- ✅ **UI responsiva** com Vue 3 + TypeScript
- ✅ **Arquitetura hexagonal** no backend
- ✅ **Containerização completa** com Docker Compose
- ✅ **Security hardening** (CORS, CSP, BCrypt, RFC 9457)

---

## ✨ Features

### Core
- 📝 **CRUD de notas** — criar, ler, atualizar, deletar (máx 120 caracteres)
- 🎨 **Color picker** — 8 presets + seletor nativo
- 👤 **Autenticação** — registro + login com JWT
- 🔒 **Isolamento por usuário** — row-level security
- 📄 **Paginação** — query params (page, size, sort)

### UX
- 🎨 Smart text contrast (WCAG AA compliant)
- ⌨️ Keyboard shortcuts (Ctrl+Enter)
- 📏 Character counter (warning aos 100, limite 120)
- 📱 Responsive design (1-4 colunas)
- ✨ Animações suaves (staggered entry)
- 🔔 Error alerts com auto-dismiss
- 🕳️ Empty state messaging

### DevEx
- 🔥 Hot reload (Vite dev server)
- 🐳 Docker Compose com healthchecks
- 📚 OpenAPI/Swagger UI
- 🧪 Testcontainers para integration tests
- 📊 78 testes (100% core coverage)

---

## 🛠️ Stack Tecnológico

### Backend
- **Java 21** (Eclipse Temurin)
- **Spring Boot 3.4.3** (Web, Data JPA, Security, Actuator)
- **PostgreSQL 16** (Alpine)
- **Flyway** (Database migrations)
- **Maven 3.9.6**

### Frontend
- **Vue 3.4.21** (Composition API)
- **TypeScript 5.3.3** (strict mode)
- **Vite 5.1.4** (Build tool)
- **Axios 1.6.7** (HTTP client)
- **Lucide Vue** (Icons)

### Infrastructure
- **Docker Compose** (orchestration)
- **Nginx** (frontend serve)
- **Multi-stage builds** (Maven → JRE)

---

## 🏗️ Arquitetura

### Backend — Hexagonal Architecture

```
domain/           → Postit, User (records, zero framework deps)
application/
  ports/          → ServicePort, RepositoryPort (interfaces)
  usecases/       → Business logic (implements ports)
infrastructure/
  adapters/in/    → REST controllers, DTOs
  adapters/out/   → JPA repositories, entities
  config/         → Spring beans, security, OpenAPI
```

**Flow:** `Controller → UseCase → RepositoryPort → Adapter → JPA`

### Frontend — Composable Pattern

```
components/       → PostitForm, PostitCard, PostitGrid
composables/      → usePostits (state), useError (alerts)
services/         → postitApi (Axios client)
```

**Flow:** `Component → Composable → Service → Axios → Backend API`

### Database Schema

```sql
users (id, email, password_hash, name, role, created_at)
postits (id, content, color, user_id, created_at, updated_at)
  ↳ FK: user_id → users.id
  ↳ INDEX: idx_postits_user_created (user_id, created_at DESC)
```

---

## 🚀 Quick Start

### Pré-requisitos

- **Docker** 29.3+ e Docker Compose v2
- **Java 21** (para dev local do backend)
- **Node.js 20+** (para dev local do frontend)
- **Maven 3.9+** (ou use `./mvnw` wrapper)

### Opção 1: Full Stack no Docker (Produção-like)

```bash
# Clone o repositório
git clone <repo-url>
cd prjeto-post-it

# Copie o .env de exemplo
cp .env.example .env
# Edite o .env e defina JWT_SECRET (gere com: openssl rand -hex 32)

# Suba todos os serviços
docker compose up -d

# Acesse
# Frontend: http://localhost:3000
# API: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
```

### Opção 2: API Docker + Frontend Local (Recomendado para Dev)

```bash
# Terminal 1: API + DB
docker compose up -d db api

# Terminal 2: Frontend com hot reload
cd frontend
npm install
npm run dev
# → http://localhost:5173
```

**Vantagens:** Hot reload instantâneo, debug mais fácil, menos rebuild de imagens.

---

## 💻 Desenvolvimento

### Backend (Java)

```bash
cd backend

# Build com testes
./mvnw clean package

# Rodar localmente (precisa PostgreSQL na porta 5432)
./mvnw spring-boot:run

# Apenas unit tests
./mvnw test

# Todos os testes (incluindo Testcontainers)
./mvnw verify
```

### Frontend (Vue)

```bash
cd frontend

# Instalar dependências
npm install

# Dev server com hot reload
npm run dev

# Build de produção
npm run build

# Preview do build
npm run preview

# Type checking
npx tsc --noEmit
```

### Migrations (Flyway)

```bash
# Migrations estão em backend/src/main/resources/db/migration/
# Formato: V{n}__{description}.sql

# Aplicadas automaticamente no startup do Spring Boot
# Histórico: tabela flyway_schema_history

# REGRA: Nunca edite uma migration aplicada. Sempre crie uma nova.
```

### Configuração de Ambiente

#### Backend (`backend/src/main/resources/application.yml`)
```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
app:
  security:
    jwt-secret: ${JWT_SECRET}
    jwt-expiration-ms: 3600000  # 1 hora
```

#### Frontend (`frontend/.env`)
```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

---

## 🧪 Testes

### Backend

| Tipo | Comando | Descrição |
|------|---------|-----------|
| Unit | `./mvnw test` | Testes rápidos com mocks (13 tests) |
| Integration | `./mvnw verify` | Testcontainers + PostgreSQL real (15 tests) |
| Coverage | `./mvnw jacoco:report` | Relatório em `target/site/jacoco/` |

**Resultado atual:** 78 testes, 0 failures, 0 errors (~1:45min)

### Frontend

```bash
cd frontend

# Unit tests (TODO — próximo sprint)
npm run test

# E2E tests (TODO — próximo sprint)
npm run test:e2e

# Type checking
npx tsc --noEmit
```

### Fixtures

- **PostitObjectMother** — Test data builders para `Postit`
- **AbstractIntegrationTest** — Base class com Testcontainers setup compartilhado

---

## 📚 API Documentation

### Base URL
```
http://localhost:8080/api/v1
```

### Endpoints Públicos (sem autenticação)

#### Registro
```http
POST /auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "Senha123",
  "name": "Nome do Usuário"
}

→ 201 Created + Set-Cookie: jwt=...
```

#### Login
```http
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "Senha123"
}

→ 200 OK + Set-Cookie: jwt=...
```

#### Logout
```http
POST /auth/logout

→ 200 OK + Set-Cookie: jwt=deleted; Max-Age=0
```

### Endpoints Protegidos (requer cookie JWT)

#### Listar Post-its (paginado)
```http
GET /postits?page=0&size=20&sort=createdAt,desc
Cookie: jwt=<token>

→ 200 OK
{
  "content": [
    {
      "id": 1,
      "content": "Minha nota",
      "color": "#FF5733",
      "createdAt": "2026-04-03T10:00:00",
      "updatedAt": "2026-04-03T10:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

#### Criar Post-it
```http
POST /postits
Cookie: jwt=<token>
Content-Type: application/json

{
  "content": "Comprar café",
  "color": "#FFD700"
}

→ 201 Created
Location: /api/v1/postits/123
```

#### Buscar Post-it por ID
```http
GET /postits/{id}
Cookie: jwt=<token>

→ 200 OK (se owner) | 404 Not Found
```

#### Atualizar Post-it
```http
PUT /postits/{id}
Cookie: jwt=<token>
Content-Type: application/json

{
  "content": "Café já comprado!",
  "color": "#00FF00"
}

→ 200 OK | 404 Not Found
```

#### Deletar Post-it
```http
DELETE /postits/{id}
Cookie: jwt=<token>

→ 204 No Content | 404 Not Found
```

### Health Check
```http
GET /actuator/health
→ 200 OK {"status":"UP"}

GET /actuator/health/liveness
→ 200 OK {"status":"UP"}

GET /actuator/health/readiness
→ 200 OK {"status":"UP"}
```

### Swagger UI
Documentação interativa em: **http://localhost:8080/swagger-ui.html**

---

## 🔒 Segurança

### Autenticação
- **JWT HS384** em cookie HttpOnly, Secure (prod), SameSite=Strict
- **BCrypt** para passwords (cost 12)
- **Expiration** 1 hora (renovação manual via re-login)

### Hardening Aplicado
- ✅ **CORS** — whitelist explícito (localhost:8080, localhost:3000)
- ✅ **CSP** — `default-src 'self'; script-src 'self'`
- ✅ **Security Headers** — X-Content-Type-Options, X-Frame-Options (DENY), Referrer-Policy
- ✅ **Error Handling** — RFC 9457 sem stack traces (SEC-004)
- ✅ **Actuator** — apenas `/health` exposto (SEC-005)
- ✅ **SQL Injection** — JPA + named params (SEC-008)
- ✅ **User Enumeration** — timing attack mitigado (SEC-009)
- ✅ **Session Fixation** — JWT stateless (SEC-011)

### Pendentes (Roadmap)
- ⏳ **Rate Limiting** — Resilience4j (SEC-010)
- ⏳ **HTTPS** — TLS 1.3 em produção
- ⏳ **Secrets Management** — Vault ou AWS Secrets Manager

### Auditoria
Veja `docs/security/security-audit-sprint1.md` para auditoria completa de 12 vulnerabilidades.

---

## 📖 Documentação Técnica

### Documentos Principais
- **[CLAUDE.md](CLAUDE.md)** — Guia para desenvolvedores (comandos, arquitetura, troubleshooting)
- **[CHANGELOG.md](CHANGELOG.md)** — Histórico de mudanças (formato Keep a Changelog)
- **[IMPLEMENTATION-SUMMARY.md](IMPLEMENTATION-SUMMARY.md)** — Sumário da implementação inicial do frontend

### Documentos Técnicos
- **[ANALISE-PROJETO.md](ANALISE-PROJETO.md)** — Análise técnica detalhada (800+ linhas)
- **[FRONTEND-IMPLEMENTATION.md](FRONTEND-IMPLEMENTATION.md)** — Detalhes da implementação Vue 3
- **[FRONTEND-QUICKSTART.md](FRONTEND-QUICKSTART.md)** — Guia de 5 minutos para o frontend

### Architecture Decision Records (ADRs)
- **[ADR-001](docs/architecture/adr/ADR-001-autenticacao-local-jwt-cookie.md)** — Autenticação local vs. OAuth

### Diagramas
- **[auth-blueprint.md](docs/architecture/auth-blueprint.md)** — Sequência de login/register/logout

### Security
- **[security-audit-sprint1.md](docs/security/security-audit-sprint1.md)** — Auditoria de segurança
- **[security-remediation-summary.md](docs/security/security-remediation-summary.md)** — Sumário das correções

### Testing
- **[smoke-test-ownership.md](docs/testing/smoke-test-ownership.md)** — Testes manuais de isolamento

---

## 🗺️ Roadmap

### ✅ Concluído
- [x] Backend hexagonal (Spring Boot + PostgreSQL)
- [x] Frontend Vue 3 (CRUD completo)
- [x] Autenticação local (JWT + HttpOnly cookie)
- [x] Ownership (isolamento por usuário)
- [x] Paginação (índices otimizados)
- [x] Security hardening (11/12 itens)
- [x] Docker Compose (full stack)
- [x] Healthchecks (Actuator probes)
- [x] 78 testes (0 failures)

### 🎯 Próximos Sprints

#### Sprint: Performance & Observability
- [ ] Load testing com Gatling
- [ ] Prometheus + Grafana dashboards
- [ ] Distributed tracing (OpenTelemetry)
- [ ] Rate limiting (Resilience4j)

#### Sprint: CI/CD
- [ ] GitHub Actions pipeline
- [ ] Quality gates (SonarQube, Trivy)
- [ ] Automated deploy (staging/prod)
- [ ] Blue-green deployment

#### Sprint: Features
- [ ] Edit post-it (componente UI)
- [ ] Search/filter por conteúdo
- [ ] Sort por data/cor
- [ ] Export (CSV/JSON)
- [ ] Dark/light theme toggle

#### Sprint: Testing
- [ ] E2E tests (Playwright)
- [ ] Mutation testing (Pitest)
- [ ] Contract tests (Pact)
- [ ] Performance benchmarking

#### Sprint: Infra
- [ ] Kubernetes Helm chart
- [ ] HTTPS com Let's Encrypt
- [ ] Secrets management (Vault)
- [ ] Multi-region DR

---

## 🤝 Contribuindo

### Workflow de Git

1. Fork o projeto
2. Crie uma branch feature (`git checkout -b feature/nome-da-feature`)
3. Commit com Conventional Commits (`git commit -m "feat(scope): descrição"`)
4. Push para a branch (`git push origin feature/nome-da-feature`)
5. Abra um Pull Request

### Commit Message Convention

Seguimos [Conventional Commits](https://www.conventionalcommits.org/pt-br/):

```
feat(scope): adiciona feature X
fix(scope): corrige bug Y
docs: atualiza README
refactor(module): reestrutura Z
test(unit): adiciona cobertura para W
chore: atualiza dependências
```

### Code Review Checklist

- [ ] Código segue o style guide (Java/Vue)
- [ ] Testes passam (`mvn verify`, `npm test`)
- [ ] Cobertura mantida/aumentada
- [ ] Documentação atualizada
- [ ] CHANGELOG.md atualizado
- [ ] Commit message seguindo Conventional Commits

---

## 📜 Licença

[Definir licença] — TBD

---

## 👥 Autores

- **Desenvolvedor Principal** — [Seu Nome]
- **IA Assistant** — Claude Sonnet 4.5 (Anthropic)

---

## 🙏 Agradecimentos

- Spring Boot Team — Framework robusto e bem documentado
- Vue.js Team — Framework reativo incrível
- Testcontainers — Integration testing sem dor de cabeça
- Flyway — Migrations simples e confiáveis

---

**⚡ Start coding!**

```bash
docker compose up -d db api
cd frontend && npm run dev
```

**Visite:** http://localhost:5173 🚀
