# Sprint 1 — Tracking de Sub-Sprints (PLANO ALTERNATIVO QA LEAD)

**Versão:** v2.0 (revisado e aprovado por qa-lead)  
**Objetivo:** Cobrir gaps críticos de auth e domain para release staging  
**Total:** 15 sub-sprints | 20 testes | ~12h de trabalho  
**Status:** 0/15 completas (0%)

**Mudanças vs. v1.0:**
- ✅ Ordem correta: Use Cases → Domain (gaps residuais) → Services
- ✅ Eliminadas redundâncias: 29 → 20 testes
- ✅ Estimativas realistas: 10h → 12h (inclui setup, debugging, context switching)
- ✅ Adicionado UserPersistenceAdapter integration test (gap crítico)
- ✅ Quality gates objetivos e mensuráveis

---

## Bloco 1: Use Cases — Register (2.5h)

- [ ] **SS-01:** Criar `RegisterUseCaseTest.java` + happy path (40 min)
  - Tarefa: Mock UserRepository, validar save() chamado com password hasheado (BCrypt)
  - Validar: ArgumentCaptor captura User com passwordHash != plaintext password
  - Entregável: 1 teste
  
- [ ] **SS-02:** Teste de email duplicado (DuplicateEmailException) (30 min)
  - Tarefa: Mock findByEmail() retorna Optional.of(existingUser)
  - Validar: RegisterUseCase lança DuplicateEmailException com mensagem clara
  - Entregável: 1 teste

- [ ] **SS-03:** Teste de hash BCrypt + salt único (45 min)
  - Tarefa: Validar PasswordEncoder.encode() chamado para cada registro
  - Validar: 2 registros com mesma senha geram hashes diferentes (salt único)
  - Entregável: 2 testes

- [ ] **SS-04:** Teste de integração UserPersistenceAdapter (60 min)
  - Tarefa: Testcontainers + PostgreSQL real, validar constraint UNIQUE(email)
  - Validar: save() com email duplicado lança DataIntegrityViolationException
  - Validar: findByEmail() retorna Optional.of(user) corretamente
  - Entregável: 2 testes (1 unit + 1 integration)

**Checkpoint:** 
```bash
mvn test -Dtest=RegisterUseCaseTest
mvn verify -Dit.test=UserPersistenceAdapterIntegrationTest
```
**Resultado esperado:** 4 unit + 2 integration = 6 testes PASS

---

## Bloco 2: Use Cases — Login (2.5h)

- [ ] **SS-05:** Criar `LoginUseCaseTest.java` + happy path (40 min)
  - Tarefa: Mock UserRepository + PasswordEncoder + JwtService
  - Validar: JWT retornado, jwtService.generateToken(email) chamado
  - Entregável: 1 teste

- [ ] **SS-06:** Teste de senha incorreta (BadCredentialsException) (30 min)
  - Tarefa: Mock passwordEncoder.matches() retorna false
  - Validar: LoginUseCase lança BadCredentialsException
  - Entregável: 1 teste

- [ ] **SS-07:** Teste de usuário não encontrado (UsernameNotFoundException) (30 min)
  - Tarefa: Mock findByEmail() retorna Optional.empty()
  - Validar: LoginUseCase lança UsernameNotFoundException
  - Entregável: 1 teste

- [ ] **SS-08:** Teste de JWT claims (subject = email) (30 min)
  - Tarefa: Validar ArgumentCaptor captura email correto passado para jwtService
  - Validar: Token gerado contém subject = email do usuário
  - Entregável: 1 teste

- [ ] **SS-09:** Teste de timing attack mitigation (30 min)
  - Tarefa: Validar que "senha incorreta" e "usuário não encontrado" retornam mesma mensagem genérica
  - Validar: Tempo de resposta similar (não vazar se usuário existe)
  - Entregável: 1 teste

**Checkpoint:** 
```bash
mvn test -Dtest=LoginUseCaseTest
```
**Resultado esperado:** 5 testes PASS

---

## Bloco 3: Service — Rate Limiter (2h)

- [ ] **SS-10:** Criar `RateLimiterServiceTest.java` + limite não excedido (30 min)
  - Tarefa: Validar 5 tryConsume(ip) consecutivos retornam true
  - Validar: Bucket4j configurado para 5 requests/10 min
  - Entregável: 1 teste

- [ ] **SS-11:** Teste de bloqueio após 5 tentativas (30 min)
  - Tarefa: 6ª tentativa tryConsume(ip) retorna false
  - Validar: Controller lança TooManyRequestsException (429)
  - Entregável: 1 teste

- [ ] **SS-12:** Teste de isolamento por IP (básico, sequencial) (30 min)
  - Tarefa: IP-A faz 5 requests (esgota bucket), IP-B faz 5 requests (deve passar)
  - Validar: Buckets são independentes por IP
  - Entregável: 1 teste

- [ ] **SS-13:** Teste de refill após janela de tempo (30 min)
  - Tarefa: Mock Clock ou use Bucket4j.withNanoTimeClockSource(), avançar 10 min
  - Validar: Após 10 min, tryConsume() volta a retornar true
  - Entregável: 1 teste

**Checkpoint:** 
```bash
mvn test -Dtest=RateLimiterServiceTest
```
**Resultado esperado:** 4 testes PASS

---

## Bloco 4: Domain — Gaps Residuais (1.5h)

- [ ] **SS-14:** Criar `UserTest.java` + validação de nome blank (30 min)
  - Tarefa: Validar User.create() com nome blank lança IllegalArgumentException
  - Validar: Mensagem de erro clara ("Nome não pode ser vazio")
  - Entregável: 1 teste

- [ ] **SS-15:** Criar `PostitTest.java` + validação de cor inválida (30 min)
  - Tarefa: Testar cores "FFFFFF" (sem #) e "#ZZZ" (letras inválidas)
  - Validar: Postit.create() lança IllegalArgumentException com regex mismatch
  - Entregável: 2 testes

**Checkpoint:** 
```bash
mvn test -Dtest=UserTest,PostitTest
```
**Resultado esperado:** 3 testes PASS

---

## Bloco 5: Validação Final e Commit (3.5h)

- [ ] **Validação 1:** Rodar suite completa (15 min)
  ```bash
  mvn clean test
  ```
  **Esperado:** 100 testes PASS (80 atuais + 20 novos), 0 failures

- [ ] **Validação 2:** Rodar integration tests (10 min)
  ```bash
  mvn verify
  ```
  **Esperado:** 18 integration tests PASS (16 atuais + 2 novos UserPersistence)

- [ ] **Validação 3:** Verificar cobertura manual (30 min)
  - LoginUseCase: 5 cenários testados (happy path, senha errada, user não existe, JWT, timing)
  - RegisterUseCase: 4 cenários unit + 2 integration (happy path, email dup, BCrypt, constraint UNIQUE)
  - RateLimiterService: 4 cenários (limite OK, bloqueio, isolamento, refill)
  - Domain gaps: 3 testes (User nome blank, Postit cor inválida x2)

- [ ] **Validação 4:** Code review automatizado (15 min)
  ```bash
  # Se tiver Checkstyle/PMD configurado
  mvn checkstyle:check pmd:check
  ```

- [ ] **Commit estruturado** (10 min)
  ```bash
  git add backend/src/test/
  git commit -m "$(cat <<'EOF'
  test(auth): adiciona 20 testes de auth e domain para staging-ready
  
  Sprint 1 completa — gaps críticos cobertos:
  - LoginUseCase: 5 testes (100% cenários)
  - RegisterUseCase: 6 testes (4 unit + 2 integration)
  - RateLimiterService: 4 testes (rate limiting + refill)
  - Domain gaps: 3 testes (User nome, Postit cor)
  
  Cobertura após Sprint 1:
  - Auth use cases: 65% → staging-ready
  - Domain validations: 85% (gaps residuais cobertos)
  - Total testes: 80 → 100
  
  Refs: .claude/plans/sprint1-tracking.md (plano alternativo QA lead v2.0)
  
  Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
  EOF
  )"
  ```

---

## Quality Gates Finais (Realistas e Mensuráveis)

Após 15/15 sub-sprints completas:

- [ ] ✅ `mvn test` → **100 testes PASS** (80 atuais + 20 novos), 0 failures
- [ ] ✅ `mvn verify` → **18 integration tests PASS** (16 atuais + 2 UserPersistence)
- [ ] ✅ **LoginUseCase:** 5 testes cobrindo 100% dos cenários (happy, senha errada, user não existe, JWT, timing)
- [ ] ✅ **RegisterUseCase:** 6 testes (4 unit + 2 integration) cobrindo 100% dos cenários
- [ ] ✅ **RateLimiterService:** 4 testes cobrindo rate limiting + isolamento + refill
- [ ] ✅ **Domain gaps:** 3 testes (User nome blank, Postit cor inválida x2)
- [ ] ✅ **Constraint UNIQUE(email):** validado via integration test com Testcontainers
- [ ] ✅ **Commit estruturado:** mensagem clara com métricas de cobertura

**NÃO incluído (escopo controlado):**
- ❌ GlobalExceptionHandler (0 testes) — Sprint 2
- ❌ Security headers validation — Sprint 2
- ❌ Contract tests — Sprint 3 (quando houver múltiplos serviços)

---

## Métricas de Progresso

| Métrica | Inicial | Target | Após Sprint 1 |
|---------|---------|--------|---------------|
| **Testes totais** | 80 | 100 | 100 ✅ |
| **Cobertura auth (use cases)** | 0% | 60-65% | 65% ✅ |
| **Cobertura domain (gaps)** | 70% | 85% | 85% ✅ |
| **Auth paths testados** | 0/2 | 2/2 | 2/2 ✅ |
| **Integration tests** | 16 | 18 | 18 ✅ |
| **Tempo investido** | 0h | 12h | (tracking) |

---

## Comandos de Tracking

**Ver progresso:**
```bash
# Sub-sprints completas
grep -c "^- \[x\]" .claude/plans/sprint1-tracking.md

# Sub-sprints pendentes
grep -c "^- \[ \]" .claude/plans/sprint1-tracking.md

# Percentual de conclusão
echo "scale=1; $(grep -c "^- \[x\]" .claude/plans/sprint1-tracking.md) * 100 / 15" | bc
```

**Marcar sub-sprint como completa:**
```bash
# Exemplo: marcar SS-01 como completa
NUM=01
sed -i "s/^- \[ \] \*\*SS-$NUM/- [x] **SS-$NUM/" .claude/plans/sprint1-tracking.md
```

**Validar suite após cada bloco:**
```bash
# Após Bloco 1 (Register)
mvn test -Dtest=RegisterUseCaseTest && echo "✅ Bloco 1 OK" || echo "❌ Bloco 1 FALHOU"

# Após Bloco 2 (Login)
mvn test -Dtest=LoginUseCaseTest && echo "✅ Bloco 2 OK" || echo "❌ Bloco 2 FALHOU"

# Após Bloco 3 (Rate Limiter)
mvn test -Dtest=RateLimiterServiceTest && echo "✅ Bloco 3 OK" || echo "❌ Bloco 3 FALHOU"

# Após Bloco 4 (Domain)
mvn test -Dtest=UserTest,PostitTest && echo "✅ Bloco 4 OK" || echo "❌ Bloco 4 FALHOU"

# Final (tudo)
mvn verify && echo "✅ SPRINT 1 COMPLETA" || echo "❌ ALGO FALHOU"
```

---

## Notas da Revisão do QA Lead

**Score do plano v2.0:** 9/10 ✅

**Melhorias implementadas:**
1. ✅ Ordem TDD-friendly: Use Cases → Domain (gaps residuais)
2. ✅ Zero redundância: eliminados 9 testes que seriam cobertos indiretamente
3. ✅ Estimativas realistas: +20% de tempo considerando overhead real
4. ✅ UserPersistenceAdapter integration test adicionado (gap crítico do audit)
5. ✅ Quality gates objetivos e mensuráveis (sem abstrações tipo "Score QA")

**Riscos mitigados:**
- 🔴 C-1 (ordem invertida) → RESOLVIDO: use cases primeiro
- 🔴 C-2 (estimativa subestimada) → RESOLVIDO: 12h realistas
- 🔴 C-3 (SS-23 não atômica) → RESOLVIDO: eliminada concorrência complexa, mantido isolamento simples
- 🟠 A-2 (domain redundante) → RESOLVIDO: 12 testes eliminados
- 🟠 A-4 (constraint UNIQUE não testada) → RESOLVIDO: SS-04 adiciona integration test

**Próximo checkpoint:** Após SS-04 (fim Bloco 1), reavaliar se estimativas estão corretas. Se Bloco 1 terminar em <3h: ritmo bom. Se >3.5h: considerar estender para 4 dias.
