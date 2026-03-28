# Plano: Segurança + Secrets + Ownership de Postits
Data: 2026-03-28
Status: CONCLUÍDO ✅
Duração: ~2h (6 sprints, S1+S3 em paralelo)

## Contexto
Três itens pós-autenticação:
1. Auditoria e remediação OWASP
2. Gestão segura de secrets (JWT_SECRET, .env)
3. Associação de postits ao usuário autenticado

## Decisões
- S1 + S3 executam em paralelo (independentes)
- V3 migration: user_id nullable (dados existentes)
- V4 migration: NOT NULL após backfill — resolve o risco no Sprint 6
- IDOR resolvido no Sprint 5 (ownership check 403)
- Security headers via Spring Security (não Nginx)

## Sprints

| # | Sprint | Agent | Status |
|---|--------|-------|--------|
| 1 | Auditoria de Segurança | security-test-engineer | completed |
| 2 | Remediação de Segurança | backend-dev | completed |
| 3 | Gestão de Secrets | devops-engineer | completed |
| 4 | Migração V3 + Domínio | dba + backend-dev | completed |
| 5 | Use Cases + API com Ownership | backend-dev | completed |
| 6 | Frontend + V4 Migration (NOT NULL) + E2E | backend-dev + dba | completed |

## Dependências
- S1 + S3: paralelos (início imediato)
- S2: após S1
- S4: após S2 (e S3 concluído)
- S5: após S4
- S6: após S5 — inclui V4 migration para NOT NULL constraint

## Risco Resolvido
V3 adiciona user_id nullable. Sprint 6 entrega:
- V4__set_user_id_not_null.sql: DELETE postits sem owner + ALTER COLUMN NOT NULL
- Garante integridade referencial completa ao final
