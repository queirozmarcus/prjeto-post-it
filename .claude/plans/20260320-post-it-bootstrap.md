# Plano: prjeto-post-it — Bootstrap Completo
Data: 2026-03-20
Status: CONCLUÍDO

## Contexto
Sistema de Post-its com Java 21 + Spring Boot 3.x, arquitetura hexagonal, PostgreSQL 16,
Vue.js (escolhido pelo usuário) + Nginx, Docker Compose com healthchecks.
Projeto do zero — apenas REQUISITOS.md existia.

## Decisões
- Frontend: Vue.js (confirmado pelo usuário)
- Arquitetura: Hexagonal (domain, application.ports, application.usecases, infrastructure.adapters)
- Erros: RFC 9457 (Problem Details)
- Migrations: Flyway V1__create_postit_table.sql
- Testes: Testcontainers (integração) + Object Mother (fixtures)

## Riscos
- Multi-stage Dockerfile precisa Maven wrapper (./mvnw) no projeto
- Frontend Nginx precisa proxy reverso para /api/ → backend:8080

## Etapas
1. ⏳ /dev-bootstrap post-it-service → Estrutura hexagonal + Flyway + docker-compose + Dockerfile
2. ⏳ /dev-feature "CRUD Post-its + RFC 9457" → Use cases + adapters REST + validação
3. ⏳ /dev-api postits → Contrato OpenAPI 3.1
4. ⏳ /qa-generate PostitService → Testes unitários (Object Mother) + integração (Testcontainers)
5. ⏳ Frontend Vue.js → Componentes + integração com API + Nginx
