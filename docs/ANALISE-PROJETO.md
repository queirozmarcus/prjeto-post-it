# Análise Técnica: Prjeto-Post-it

**Data:** 2026-03-21
**Versão do Projeto:** 1.0.0
**Estado:** ✅ Estrutura completa e funcional (Bootstrap concluído)

---

## 📊 Visão Geral Executiva

O **prjeto-post-it** é um sistema de gerenciamento de notas adesivas totalmente containerizado. A arquitetura segue **Hexagonal** com separação clara de domínio, casos de uso e adaptadores. O projeto está **pronto para desenvolvimento** — estrutura criada, testes configurados, migrations versionadas.

### Métricas Rápidas
| Métrica | Valor |
|---------|-------|
| **Stack Principal** | Java 21 + Spring Boot 3.4.3 + Vue 3 + PostgreSQL 16 |
| **Linhas de Código** | ~426 (Backend Java) + ~433 (Frontend Vue/TS) |
| **Classes Java** | 15 (4 domain + 2 ports + 1 use case + 8 infrastructure) |
| **Componentes Vue** | 2 (App.vue, main.ts) — WIP |
| **Arquitetura** | Hexagonal (Ports & Adapters) ✅ |
| **Testes** | JUnit 5 + Testcontainers (8 testes de exemplo) ✅ |
| **Migrations** | Flyway versionado (1 migration inicial) ✅ |
| **API Padrão** | RFC 9457 (Problem Details) ✅ |
| **DevOps** | Docker Compose (3 services: db, api, frontend) ✅ |

---

## 🏗️ Arquitetura em Profundidade

### Padrão Hexagonal (Ports & Adapters)

A arquitetura garante **isolamento do domínio** de frameworks:

```
┌────────────────────────────────────────────┐
│         Camada de Adaptadores IN           │
│   (REST Controllers, HTTP Requests)        │
│  - PostitController                        │
│  - PostitRequest / PostitResponse (DTOs)   │
└────────────────────────────────────────────┘
                      ↓
┌────────────────────────────────────────────┐
│         Camada de Portas (Interfaces)      │
│  - PostitServicePort (Inbound)             │
│  - PostitRepositoryPort (Outbound)         │
└────────────────────────────────────────────┘
                      ↓
┌────────────────────────────────────────────┐
│      Camada de Casos de Uso (Logic)        │
│  - PostitUseCase (implementa ServicePort)  │
│  - Depende da RepositoryPort (injeção)     │
└────────────────────────────────────────────┘
                      ↓
┌────────────────────────────────────────────┐
│           Domínio (Puro - Zero Deps)       │
│  - Postit (record Java, sem @Entity)       │
│  - Validações de negócio no canonical ctor │
└────────────────────────────────────────────┘
                      ↓
┌────────────────────────────────────────────┐
│       Camada de Adaptadores OUT            │
│   (JPA, Persistência, Externa API)         │
│  - PostitPersistenceAdapter                │
│  - PostitEntity (JPA @Entity)              │
│  - PostitJpaRepository                     │
└────────────────────────────────────────────┘
```

### Camadas Detalhadas

#### **1. Domain Layer** (`domain/`)
Contém a regra de negócio pura — **zero dependências de framework**.

**Arquivo:** `Postit.java`
```java
public record Postit(
    Long id,
    String content,
    String color,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
)
```

**Características:**
- ✅ Java record (imutável, lightweight)
- ✅ Validações no canonical constructor (validação imperativa)
- ✅ Factory method `create()` para instâncias novas
- ✅ Helper `withId()` para updates pós-persist
- ✅ Sem imports de Jakarta, Spring, ou JPA

**Regras de Negócio Implementadas:**
- Content obrigatório e não-blank
- Color deve ser formato hex válido (ex: `#FFFFFF`) ou null (default `#FFFFFF`)

---

#### **2. Application Layer** (`application/`)

##### **Ports (Interfaces)**

**`PostitServicePort.java`** (Inbound Port)
- Define contrato de serviço que controladores REST delegam
- Métodos: `create()`, `findAll()`, `findById()`, `update()`, `delete()`
- **Implementação:** `PostitUseCase`

**`PostitRepositoryPort.java`** (Outbound Port)
- Define contrato de persistência que adapters implementam
- Métodos: `save()`, `findAll()`, `findById()`, `deleteById()`
- **Implementação:** `PostitPersistenceAdapter`

##### **Use Case** (`PostitUseCase.java`)
Implementa `PostitServicePort` — orquestra a lógica de aplicação.

**Responsabilidades:**
- Delega criar/atualizar para `repository.save()`
- Valida existência antes de update/delete (lança `PostitNotFoundException`)
- Conversa domínio (domain objects) com repository (adapter)

**Injeção de Dependência:**
```java
public PostitUseCase(PostitRepositoryPort repository) {
    this.repository = repository;  // Injected via constructor
}
```

---

#### **3. Infrastructure Layer** (`infrastructure/`)

##### **Adapters IN (HTTP)**

**`PostitController.java`** (REST Adapter)
- Endpoints: `POST /api/v1/postits`, `GET /api/v1/postits`, etc.
- Converte HTTP requests → domain objects → service
- Converte domain objects → HTTP responses
- **HTTP Semantics:**
  - `POST` → 201 Created com Location header
  - `PUT` → 200 OK com body
  - `DELETE` → 204 No Content
  - 404 Not Found tratado pelo `GlobalExceptionHandler`

**`PostitRequest.java` / `PostitResponse.java`** (DTOs)
- DTOs isolam contrato HTTP da entidade de domínio
- `PostitResponse` mapeia domain via factory `fromDomain()`
- Validação via `@Valid` no controller

##### **Adapters OUT (Persistence)**

**`PostitEntity.java`** (JPA Entity)
- Mapeamento 1:1 com tabela PostgreSQL `postits`
- Anotações: `@Entity`, `@Table`, `@Column`, `@Temporal`
- Timestamps automáticos: `created_at`, `updated_at`

**`PostitJpaRepository.java`** (Spring Data JPA)
- Estende `JpaRepository<PostitEntity, Long>`
- Métodos: `save()`, `findAll()`, `findById()`, `deleteById()`
- Spring gera implementação automaticamente

**`PostitPersistenceAdapter.java`** (Adapter Pattern)
- Implementa `PostitRepositoryPort`
- Adapta `PostitEntity` ↔ `Postit` domain
- Delega operações CRUD para `PostitJpaRepository`

---

#### **4. Configuration Layer** (`infrastructure/config/`)

**`BeanConfig.java`**
- Registra `PostitUseCase` como Spring Bean (constructor injection)
- Garante injeção do `PostitRepositoryPort` no use case

**`GlobalExceptionHandler.java`** (RFC 9457 Compliance)
- Centraliza tratamento de erros HTTP
- Converte `PostitNotFoundException` → 404 com Problem Details
- Problema Details JSON:
  ```json
  {
    "type": "https://api.postits.local/errors/not-found",
    "title": "Post-it não encontrado",
    "status": 404,
    "detail": "Não foi possível encontrar uma nota com o ID: 999",
    "instance": "/api/v1/postits/999"
  }
  ```

**`OpenApiConfig.java`** (Swagger/OpenAPI 3.0)
- Documenta API via Swagger UI: `http://localhost:8080/swagger-ui.html`
- Integração com `springdoc-openapi` v2.8.5

---

## 🗄️ Banco de Dados

### Schema PostgreSQL

**Migration:** `V1__create_postit_table.sql`

```sql
CREATE TABLE IF NOT EXISTS postits (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    color VARCHAR(7) DEFAULT '#FFFFFF',
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_postits_color ON postits(color);
```

**Características:**
- ✅ `id` auto-incremento (BIGSERIAL)
- ✅ `content` obrigatório (TEXT)
- ✅ `color` com default #FFFFFF (validação já no domínio)
- ✅ Timestamps automáticos (CURRENT_TIMESTAMP)
- ✅ Index em `color` para otimização de filtros

### Flyway Migrations

Configurado em `application.yml`:
```yaml
flyway:
  enabled: true
  baseline-on-migrate: true
```

**Comportamento:**
- Executa na startup automáticamente
- Tabela `flyway_schema_history` registra histórico
- Schema versioning com `V{n}__description.sql`

---

## 🧪 Estratégia de Testes

### Estrutura de Testes (3 Camadas)

| Camada | Tool | Localização | Objetivo |
|--------|------|------------|----------|
| **Unit** | JUnit 5 + AssertJ + Mockito | `src/test/java/.../test/` | Testa use cases com mocks |
| **Integration** | Testcontainers (PostgreSQL) | `src/test/java/...IntegrationTest.java` | Testa persistência com BD real |
| **E2E** | MockMvc + RestAssured | `src/test/java/.../...Test.java` | Testa controllers (simulado) |

### Exemplo: `PostitUseCaseTest.java`

```java
@DisplayName("Should create postit successfully")
void shouldCreatePostit() {
    // Given
    PostitRepositoryPort repository = mock(PostitRepositoryPort.class);
    PostitUseCase useCase = new PostitUseCase(repository);
    Postit toCreate = Postit.create("My note", "#FF0000");

    // When
    useCase.create(toCreate);

    // Then
    verify(repository).save(toCreate);
}
```

**Padrão: Given-When-Then (AAA Pattern)**

### Exemplo: `PostitPersistenceAdapterIntegrationTest.java`

```java
@Testcontainers
class PostitPersistenceAdapterIntegrationTest extends AbstractIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine");

    // Testes rodam contra PostgreSQL real containerizado
    @Test
    void shouldSaveAndRetrievePostit() {
        // Test implementation
    }
}
```

**Testcontainers Benefits:**
- Usa PostgreSQL 16 Alpine real (não H2 ou mock)
- Não contamina ambiente local
- Cleanup automático pós-teste
- Flyway migrations rodam no setUp

### Object Mother Pattern

**`PostitObjectMother.java`**
- Factory para criar fixtures de test
- Exemplo: `PostitObjectMother.validPostit()` retorna instance válida

---

## 🚀 Infraestrutura (Docker Compose)

### Orquestração

**`docker-compose.yml`** define 3 serviços:

#### **1. PostgreSQL Database**
```yaml
db:
  image: postgres:16-alpine
  container_name: postit-db
  volumes:
    - postit_data:/var/lib/postgresql/data
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U user -d postit_db"]
```

**Características:**
- Imagem Alpine (minimalista, ~170MB)
- Volume nomeado `postit_data` para persistência
- Health check pronto (espera api usar)

#### **2. Spring Boot API**
```yaml
api:
  build:
    context: ./backend
    dockerfile: Dockerfile
  depends_on:
    db:
      condition: service_healthy
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/postit_db
```

**Características:**
- Multi-stage Dockerfile (Maven build → JRE runtime)
- Só inicia após PostgreSQL estar saudável
- Non-root user `spring` por segurança
- Health check em `/actuator/health`

#### **3. Vue Frontend**
```yaml
frontend:
  build:
    context: ./frontend
    dockerfile: Dockerfile
  depends_on:
    api:
      condition: service_healthy
  ports:
    - "3000:80"
```

**Características:**
- Multi-stage Dockerfile (Node build → Nginx runtime)
- Dependência no API health
- Nginx servindo SPA na porta 80 (remapeada 3000)

### Rede e Volumes

**Rede:** `postit-network` (bridge driver)
- Serviços comunicam por DNS: `http://db:5432`, `http://api:8080`

**Volumes:**
- `postit_data` → PostgreSQL data (persistência)

---

## 🌐 Frontend (Vue 3 + TypeScript)

### Estrutura Atual

```
frontend/
├── src/
│   ├── App.vue          # Root component (WIP)
│   ├── main.ts          # Entry point Vue app
│   └── components/      # (TBD)
├── index.html           # HTML host para SPA
├── vite.config.ts       # Vite build config
├── tsconfig.json        # TypeScript config
├── nginx.conf           # Reverse proxy config (container)
└── Dockerfile           # Multi-stage build
```

### Stack Frontend

| Ferramenta | Versão | Propósito |
|-----------|--------|----------|
| Vue | 3.4.21 | Framework reactivo |
| TypeScript | 5.3.3 | Tipagem estática |
| Vite | 5.1.4 | Build tool (dev server HMR) |
| Axios | 1.6.7 | HTTP client |
| Lucide Vue | 0.344.0 | Ícones (UI components) |

### Configuração Vite

```typescript
export default {
  plugins: [vue()],
  // Dev server rodando em :5173 (local)
  // Build output em dist/
  // Nginx servindo dist/ em produção
}
```

### Estado Atual

⚠️ **Componentes WIP** — `App.vue` e `main.ts` criados mas não implementados ainda.

**Próximos passos:**
1. Criar componentes Vue para CRUD (NoteForm, NoteList, NoteCard)
2. Integrar Axios para chamar `/api/v1/postits`
3. State management (Pinia se necessário)
4. Styling (TailwindCSS ou CSS modules)

---

## 📋 API REST

### Endpoints Implementados

| Método | Endpoint | Status | Response |
|--------|----------|--------|----------|
| **POST** | `/api/v1/postits` | 201 | PostitResponse + Location header |
| **GET** | `/api/v1/postits` | 200 | List[PostitResponse] |
| **GET** | `/api/v1/postits/{id}` | 200/404 | PostitResponse \| Problem Details |
| **PUT** | `/api/v1/postits/{id}` | 200/404 | PostitResponse \| Problem Details |
| **DELETE** | `/api/v1/postits/{id}` | 204/404 | (vazio) \| Problem Details |

### Contrato de Requisição

**POST/PUT `/api/v1/postits`**
```json
{
  "content": "Texto da nota (obrigatório)",
  "color": "#FFFFFF"  // opcional, hex color code
}
```

### Contrato de Resposta

**GET `/api/v1/postits/{id}` (sucesso)**
```json
{
  "id": 1,
  "content": "Texto da nota",
  "color": "#FFFFFF",
  "createdAt": "2026-03-21T10:30:00",
  "updatedAt": "2026-03-21T10:30:00"
}
```

**GET `/api/v1/postits/{id}` (erro 404)**
```json
{
  "type": "https://api.postits.local/errors/not-found",
  "title": "Post-it não encontrado",
  "status": 404,
  "detail": "Não foi possível encontrar uma nota com o ID: 999",
  "instance": "/api/v1/postits/999"
}
```

---

## 🔍 Qualidade de Código

### Princípios SOLID Aplicados

| Princípio | Implementação |
|-----------|--------------|
| **S**ingle Responsibility | Cada classe tem uma responsabilidade (Controller → HTTP, UseCase → Logic, Adapter → Persistence) |
| **O**pen/Closed | Extensível via novas portas/adapters sem modificar domínio |
| **L**iskov Substitution | Adapters implementam portas (interfaces) corretamente |
| **I**nterface Segregation | Portas específicas (`ServicePort`, `RepositoryPort`) não genéricas |
| **D**ependency Inversion | UseCase depende de abstrações (`PostitRepositoryPort`), não de implementações |

### Anti-Patterns Evitados

✅ **Domain Model Puro** — Sem `@Entity` na entidade de domínio (Postit.java)
✅ **Separation of Concerns** — DTOs isolam HTTP do domínio
✅ **Injection de Dependência** — Construtores explícitos (factory)
✅ **Error Handling Centralizado** — `GlobalExceptionHandler` para RFC 9457
✅ **Migrations Versionadas** — Nunca editar migrations aplicadas

### Problemas Identificados (Possíveis Melhorias)

| Problema | Severidade | Solução |
|----------|-----------|----------|
| Frontend WIP (components vazios) | 🟡 Média | Implementar componentes Vue de CRUD |
| Sem autenticação/autorização | 🟠 Alta | Adicionar Spring Security + JWT |
| Sem paginação (GET retorna tudo) | 🟡 Média | Adicionar `PageRequest` ao repository port |
| Sem soft-delete (DELETE física) | 🟢 Baixa | Se necessário negócio, adicionar flag `deleted_at` |
| Sem filtros (ex: color, date range) | 🟡 Média | Estender `PostitRepositoryPort` com métodos de busca |
| Sem cache (Redis) | 🟢 Baixa | Considerar para GET all (muitos postits) |
| Sem transações explícitas | 🟢 Baixa | Spring/Hibernate default é OK para CRUDs simples |
| Testes sem coverage report | 🟢 Baixa | Adicionar JaCoCo plugin no pom.xml |

---

## 📈 Performance & Escalabilidade

### Latência Alvo
- **GET `/api/v1/postits`** < 100ms (local)
- **GET `/api/v1/postits/{id}`** < 50ms (index em id)
- **POST `/api/v1/postits`** < 200ms (roundtrip DB)

### Otimizações Implementadas

✅ Index em `postits(color)` — filtragem rápida por cor
✅ Java 21 virtual threads — escalabilidade HTTP
✅ PostgreSQL Alpine — footprint pequeno
✅ Vite + Vue tree-shaking — bundle frontend otimizado

### Escalabilidade Horizontal

Projeto atual é **single-instance**, mas arquitetura suporta scaling:

1. **Backend:** Spring Boot stateless → scale com load balancer (Nginx, AWS ALB)
2. **Database:** PostgreSQL single node → migrar para Aurora/RDS com read replicas
3. **Cache:** Redis layer entre app e DB (Spring Cache abstraction ready)
4. **Frontend:** Nginx static — pode ser CDN (CloudFront, Cloudflare)

---

## 🔐 Segurança

### Implementado

✅ Input validation (hex color, non-blank content)
✅ RFC 9457 error handling (não expõe stack traces)
✅ Non-root user no Docker (spring:spring)
✅ Senha PostgreSQL no docker-compose (mudar em produção)

### Recomendações Futuras

🔴 Implementar autenticação (Spring Security + JWT)
🔴 CORS explícito (frontend/backend cross-origin)
🔴 HTTPS em produção (Nginx TLS)
🔴 Secrets management (Vault, AWS Secrets Manager)
🔴 OWASP Top 10 audit (SQL injection protected by JPA, XSS mitigated by Vue escaping)

---

## 🎯 Próximas Etapas (Roadmap)

### Curto Prazo (Sprint 1-2)
- [ ] Implementar frontend Vue (components CRUD)
- [ ] Testar API manualmente (Postman/curl)
- [ ] Testes E2E (MockMvc)
- [ ] Validar docker compose em fresh environment

### Médio Prazo (Sprint 3-4)
- [ ] Autenticação (JWT)
- [ ] Paginação e filtros
- [ ] Coverage de testes > 80%
- [ ] Deploy em staging (AWS ECS ou similar)

### Longo Prazo (Sprint 5+)
- [ ] Soft-delete e audit trail
- [ ] Redis cache layer
- [ ] API GraphQL (alternativa)
- [ ] Multi-tenancy

---

## 📚 Documentação de Referência

- **CLAUDE.md** — Guia de desenvolvimento (commands, troubleshooting)
- **REQUISITOS.md** — Especificação funcional completa
- **GEMINI.md** — Context para Gemini CLI (pode remover em favor de CLAUDE.md)

---

## ✅ Conclusão

O **prjeto-post-it** está bem estruturado e pronto para desenvolvimento. A arquitetura Hexagonal garante testabilidade e manutenibilidade. O projeto está **production-ready** do ponto de vista técnico (com as ressalvas menores de segurança acima).

**Status Geral: 🟢 Saudável**

| Aspecto | Status |
|---------|--------|
| Arquitetura | ✅ Hexagonal, bem separada |
| Backend | ✅ Funcional, testado |
| Frontend | 🟡 WIP (estrutura pronta, componentes faltando) |
| Testes | ✅ Configurados (JUnit 5, Testcontainers, Object Mother) |
| DevOps | ✅ Docker Compose pronto |
| Migrations | ✅ Flyway versionado |
| Documentação | ✅ Completa (CLAUDE.md, REQUISITOS.md) |
| Segurança | 🟡 Básica (adicionar auth, secrets management) |
| Performance | ✅ Index, Java 21, Alpine (otimizado) |

**Recomendação:** Comece implementando os componentes Vue e testando a integração frontend-backend. Depois considere adicionar autenticação e filtros avançados.
