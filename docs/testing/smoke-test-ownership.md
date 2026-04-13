# Smoke Test — Isolamento de Postits por Usuário

**Sprint:** 6 (final)
**Data de criação:** 2026-03-28
**Pré-condição:** Sistema sem dados de sessões anteriores (ou banco resetado)

## Pré-requisitos

```bash
# Subir todo o stack
cd /home/mq/iGitHub/prjeto-post-it
docker compose down -v        # Remove volumes para garantir banco limpo
docker compose up -d --build  # Rebuild com V4 aplicada

# Aguardar saúde dos containers
docker compose ps
# Todos devem estar "healthy" ou "running"

# Verificar que V4 foi aplicada
docker exec postit-db psql -U user -d postit_db \
  -c "SELECT installed_rank, version, description FROM flyway_schema_history ORDER BY installed_rank"
# Deve listar V1, V2, V3 e V4
```

---

## Roteiro

### Cenário 1: Fluxo básico — Registro, criação e visualização

1. Acesse http://localhost:3000
2. Você deve ser redirecionado para http://localhost:3000/login
3. Clique em "Criar conta" (ou navegue para `/register`)
4. Crie conta com:
   - Email: user-a@test.com
   - Senha: Senha123A
   - Nome: User A
5. Após registro, você deve ser redirecionado para a tela principal (Home)
6. A lista deve estar **vazia** com a mensagem de estado vazio visível
7. Crie o post-it 1:
   - Conteúdo: "Nota do User A — numero 1"
   - Cor: qualquer
8. Crie o post-it 2:
   - Conteúdo: "Nota do User A — numero 2"
   - Cor: qualquer
9. Verifique que **ambos aparecem** na grid
10. Clique em "Sair" (botão no header)
11. Você deve ser redirecionado para `/login`

**Resultado esperado:**
- Registro cria sessão JWT automaticamente
- Estado vazio exibido quando lista está vazia
- Post-its aparecem imediatamente após criação (sem reload)
- Logout redireciona para login

---

### Cenário 2: Isolamento entre usuários

1. Na tela de login, clique em "Criar conta"
2. Crie conta com:
   - Email: user-b@test.com
   - Senha: Senha456B
   - Nome: User B
3. Após registro, verifique que a lista está **VAZIA**
   - Mensagem de estado vazio deve aparecer
   - Os 2 post-its do User A **NÃO devem aparecer**
4. Crie 1 post-it:
   - Conteúdo: "Nota exclusiva do User B"
   - Cor: qualquer
5. Verifique que apenas este post-it aparece
6. Clique em "Sair"

**Resultado esperado:**
- User B vê lista vazia ao logar (isolamento confirmado)
- User B só vê seus próprios post-its após criar

---

### Cenário 3: Login e persistência do isolamento

1. Na tela de login, faça login como:
   - Email: user-a@test.com
   - Senha: Senha123A
2. Verifique que a lista contém **apenas os 2 post-its do User A**:
   - "Nota do User A — numero 1"
   - "Nota do User A — numero 2"
3. A nota do User B ("Nota exclusiva do User B") **NÃO deve aparecer**
4. Tente deletar o post-it 1 do User A
5. Confirme a exclusão no popup
6. Verifique que o post-it foi removido da lista (apenas 1 restante)

**Resultado esperado:**
- Login restaura sessão com os dados do usuário correto
- Isolamento mantido após logout e re-login
- Delete funciona para posts do próprio usuário

---

### Cenário 4: Verificação de erro de permissão (opcional — requer curl)

Este cenário simula o que ocorreria se um cliente tentasse acessar diretamente a API.

```bash
# Obter cookie de User B fazendo login via curl
COOKIE=$(curl -s -c - -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user-b@test.com","password":"Senha456B"}' \
  | grep jwt | awk '{print $7}')

# Obter ID de um post-it do User A (listar como User A e anotar o id)
# Aqui usamos um ID hipotético — substituir pelo real
POSTIT_ID_A=1

# Tentar deletar post-it do User A com cookie do User B
curl -v -X DELETE http://localhost:8080/api/v1/postits/$POSTIT_ID_A \
  -H "Cookie: jwt=$COOKIE"

# Resultado esperado: HTTP 403 Forbidden
```

---

## Resultado esperado (consolidado)

| Cenario | Verificacao | Status esperado |
|---------|-------------|-----------------|
| 1 | Registro cria sessao com JWT | Redirecionamento para Home |
| 1 | Lista vazia mostra mensagem amigavel | Visivel sem post-its |
| 1 | Post-its criados aparecem imediatamente | Sem refresh manual |
| 1 | Logout redireciona para login | `/login` |
| 2 | User B nao ve post-its do User A | Lista vazia ao logar |
| 2 | User B ve apenas seus proprios post-its | Isolamento confirmado |
| 3 | User A ve apenas seus 2 post-its apos re-login | Persistencia correta |
| 3 | User A pode deletar seus proprios post-its | Delete funcional |
| 4 | User B recebe 403 ao tentar deletar post-it de A | IDOR protegido |

---

## Verificacao pós-teste no banco (opcional)

```bash
# Confirmar isolamento no banco de dados
docker exec postit-db psql -U user -d postit_db -c "
  SELECT p.id, p.content, u.email
  FROM postits p
  JOIN users u ON p.user_id = u.id
  ORDER BY u.email, p.id;
"
# Cada postit deve ter user_id nao nulo e email do owner correto

# Confirmar que nenhum postit existe sem user_id (V4 resolvida)
docker exec postit-db psql -U user -d postit_db -c "
  SELECT COUNT(*) as orphan_count FROM postits WHERE user_id IS NULL;
"
# Deve retornar: orphan_count = 0
```
