# Plano: Login Local com Segurança
Data: 2026-03-28
Status: CONCLUÍDO ✅
Duração: ~1h (10 sprints, alguns em paralelo)

## Contexto
Implementar autenticação local (email + senha) no prjeto-post-it.
Stack: Java 21 + Spring Boot 3.4.3 + Vue 3 + PostgreSQL 16.
Arquitetura: Hexagonal. Token: JWT httpOnly cookie (1h).

## Decisões
- Auth: Spring Security + UserDetailsService + BCrypt (strength 12)
- Token: JWT httpOnly cookie, SameSite=Lax
- Rate limiting: bucket4j (5 tentativas/min por IP no /login)
- Erro 401 genérico no login (não revela se email existe)
- CSRF desabilitado (SPA stateless com cookie)
- CORS allowlist: localhost:3000

## Sprints

| # | Sprint | Agent | Status |
|---|--------|-------|--------|
| 1 | Blueprint de Arquitetura | architect | completed |
| 2 | Banco de Dados (Flyway V2) | dba | completed |
| 3 | Dependências e Estrutura | backend-dev | completed |
| 4 | Domínio de Usuário | backend-dev | completed |
| 5 | JWT Service | backend-dev | completed |
| 6 | Spring Security Config | backend-dev | completed |
| 7 | JWT Auth Filter | backend-dev | completed |
| 8 | Endpoints Register e Login | backend-dev | completed |
| 9 | Endpoints Me e Logout + Testes | backend-dev | completed |
| 10 | Frontend Auth | backend-dev (Vue) | completed |

## Dependências
- S2 + S3: paralelos após S1
- S4: após S2 + S3
- S5 + S6: paralelos após S4
- S7: após S5 + S6
- S8: após S7
- S9: após S8
- S10: após S9
