-- V4: Aplica constraint NOT NULL em postits.user_id
--
-- DECISÃO: Deletar postits sem user_id (órfãos de desenvolvimento)
--
-- Justificativa:
--   1. O sistema só aceitou postits sem user_id durante o desenvolvimento inicial,
--      antes da autenticação ser implementada (Sprint 3) e o ownership ser aplicado
--      (Sprint 5). Em produção, o endpoint POST /api/v1/postits exige autenticação
--      desde o primeiro deploy — portanto nenhum postit legítimo de produção terá
--      user_id NULL.
--
--   2. Dados sem owner são irrecuperáveis por design: não há como atribuí-los a um
--      usuário correto sem informação arbitrária. Um UPDATE com user_id = 1 (ou
--      qualquer outro) seria factualmente incorreto e criaria falsos dados de ownership.
--
--   3. Esta migration é idempotente no ambiente de produção: DELETE WHERE user_id IS NULL
--      não afetará nenhuma linha caso o sistema tenha sido inicializado já com Sprint 5
--      deployado (o que é o caso do primeiro deploy de produção).
--
-- Risco: BAIXO — apenas remove dados de desenvolvimento. Zero impacto em produção limpa.

-- Remove postits sem usuário associado (dados órfãos da fase de desenvolvimento)
DELETE FROM postits WHERE user_id IS NULL;

-- Aplica constraint NOT NULL — user_id agora é obrigatório em todos os postits
ALTER TABLE postits ALTER COLUMN user_id SET NOT NULL;
