# Sprint 2: Análise de Cobertura de Testes

**Data:** 2026-04-03  
**QA Lead:** Claude Code (qa-lead)  
**Status:** ✅ Concluído

---

## 1. Problema/Oportunidade Identificada

### Contexto
O projeto Post-it possui 11 arquivos de teste distribuídos entre unit, integration e fixtures. Identificamos que:

- **Falta ferramenta de cobertura automatizada** — sem JaCoCo configurado, não há métricas objetivas de cobertura por linha/branch
- **Gaps de cobertura no domain User** — record com 3 validações não possui testes unitários diretos
- **Use cases de autenticação sem testes diretos** — `LoginUseCase` e `RegisterUseCase` testados apenas via integration tests
- **Ausência de quality gates** — build não bloqueia quando cobertura é baixa

### Análise de Risco

| Componente | Risco | Cobertura Atual | Justificativa |
|------------|-------|-----------------|---------------|
| `Postit` (domain) | Alto | ~95% | Bem coberto (PostitUseCaseTest + validações inline) |
| `User` (domain) | Alto | ~60% | Validações não testadas isoladamente |
| `PostitUseCase` | Alto | ~90% | 18 testes cobrindo ownership + paginação |
| `LoginUseCase` | Alto | ~70% | Apenas AuthIntegrationTest (mock fraco) |
| `RegisterUseCase` | Alto | ~70% | Apenas AuthIntegrationTest (mock fraco) |
| `PageQuery` / `PageResult` | Médio | 100% | Testes unitários dedicados |
| Controllers | Médio | ~85% | PostitControllerUnitTest + AuthControllerTest |
| Adapters (persistence) | Médio | ~80% | Testcontainers integration test |

### Métricas Estimadas (sem JaCoCo)

Baseado em inspeção manual dos testes:

| Camada | Classes | Testes | Cobertura Estimada | Status |
|--------|---------|--------|-------------------|--------|
| **Domain** | 2 (Postit, User) | 1 file (PostitObjectMother) + inline validations | ~80% | ⚠️ User sem testes dedicados |
| **Application (use cases)** | 3 (PostitUseCase, LoginUseCase, RegisterUseCase) | 2 files (PostitUseCaseTest, AuthIntegrationTest) | ~80% | ⚠️ Auth use cases sem unit tests |
| **Infrastructure (adapters)** | Controllers + persistence | 5 files | ~80% | ✅ Boa cobertura |
| **Geral** | ~37 classes Java | 11 test files | ~78% | 🟡 Abaixo do target de 80% |

---

## 2. Ações Tomadas

### 2.1. Adição do Plugin JaCoCo ao `pom.xml`

Configurei JaCoCo 0.8.12 com:

- **Instrumentation automática** via `prepare-agent`
- **Report gerado em `verify` phase** (HTML + XML em `target/site/jacoco/`)
- **Quality gates com bloqueio de build:**
  - 80% de cobertura de instruções (INSTRUCTION)
  - 70% de cobertura de branches (BRANCH)
- **Exclusões sensatas:**
  - `**/PostitApplication.class` (Spring Boot bootstrap)
  - `**/config/**` (beans de configuração)
  - `**/*Request.class`, `**/*Response.class` (DTOs sem lógica)
  - `**/*Entity.class` (JPA entities — mapeamento simples)
  - `**/*JpaRepository.class` (interfaces geradas pelo Spring Data)

**Arquivo modificado:** `/home/mq/iGitHub/prjeto-post-it/backend/pom.xml` (linhas 171-215)

**Comando para gerar relatório:**
```bash
cd backend
mvn clean verify  # Gera target/site/jacoco/index.html
```

**Exemplo de quality gate:**
```xml
<limit>
    <counter>INSTRUCTION</counter>
    <value>COVEREDRATIO</value>
    <minimum>0.80</minimum>
</limit>
```

### 2.2. Identificação de Classes Sem Cobertura

| Classe | Tipo | Motivo da Ausência | Risco | Prioridade |
|--------|------|-------------------|-------|-----------|
| `User` (domain record) | Domain | Sem testes unitários dedicados para validações | Alto | 🔴 Sprint 3 |
| `LoginUseCase` | Application | Apenas integration test (mock insuficiente) | Alto | 🔴 Sprint 3 |
| `RegisterUseCase` | Application | Apenas integration test (mock insuficiente) | Alto | 🔴 Sprint 3 |
| `UserNotFoundException` | Exception | Exception simples — baixo risco | Baixo | ⚪ Opcional |

### 2.3. Análise de Fixtures (Test Data Builder)

**Arquivo:** `PostitObjectMother.java`

Padrão **Object Mother** implementado corretamente:

- `validPostit()` — postit válido com ID (cenários de update/delete)
- `postitToCreate()` — sem ID (cenários de criação)
- `postitToCreateWithUserId()` — com ownership (auth contexts)
- Usa `Postit.create()` e `withId()` do domain

**Sugestão de melhoria:** Criar `UserObjectMother` análogo para evitar repetição de `User.create()` nos testes.

---

## 3. Impacto/Melhorias

### Ganhos Imediatos

1. **Visibilidade objetiva de cobertura** — JaCoCo gera relatório HTML navegável por classe/método
2. **Quality gate automatizado** — build falha se cobertura < 80% instruções ou < 70% branches
3. **Mapeamento de gaps** — 3 classes prioritárias identificadas (User, LoginUseCase, RegisterUseCase)
4. **Baseline estabelecido** — ~78% cobertura estimada (será confirmado após primeiro `mvn verify`)

### Próximos Passos (Sprint 3)

1. Rodar `mvn clean verify` para obter métricas reais de JaCoCo
2. Criar `UserTest.java` para cobrir validações do domain record User
3. Criar `LoginUseCaseTest.java` com mocks de `UserRepositoryPort` + `PasswordEncoder` + `JwtService`
4. Criar `RegisterUseCaseTest.java` com os mesmos mocks
5. Criar `UserObjectMother.java` para padronizar fixtures de User

### Riscos Mitigados

- **Regressão silenciosa** — JaCoCo bloqueia PRs que reduzem cobertura abaixo de 80%
- **Falso positivo de "tudo testado"** — métricas objetivas substituem inspeção manual

---

## 4. Status dos Testes Após Mudanças

### Configuração Adicionada

- ✅ JaCoCo plugin configurado em `pom.xml`
- ✅ Quality gates definidos (80% instruções, 70% branches)
- ✅ Exclusões configuradas (DTOs, configs, bootstrap)

### Execução Pendente

- ⏳ Aguardando primeiro `mvn clean verify` para gerar relatório inicial
- ⏳ Baseline de cobertura real será estabelecido após execução bem-sucedida

### Comando de Validação

```bash
cd /home/mq/iGitHub/prjeto-post-it/backend
mvn clean verify

# Se build passar:
# → target/site/jacoco/index.html (abrir no browser)
# → métricas detalhadas por pacote/classe

# Se build falhar:
# → JaCoCo reportará quais classes estão abaixo do threshold
# → ajustar exclusões ou adicionar testes antes de mergear
```

---

## 5. Aprendizados e Recomendações

### O que funciona bem no projeto

1. **Testcontainers adotado desde Sprint 1** — testes de integração com PostgreSQL real
2. **Separação clara de responsabilidades** — unit tests mockam ports, integration tests usam infra real
3. **Object Mother pattern** — fixtures padronizadas em `PostitObjectMother`
4. **Naming consistente** — `*Test.java` para unit, `*IntegrationTest.java` para integration

### Oportunidades de melhoria

1. **Criar testes unitários para domain records** — validações de `User` devem ter testes dedicados
2. **Testar use cases isoladamente** — `LoginUseCase` e `RegisterUseCase` precisam de unit tests com mocks
3. **Adicionar mutation testing** — JaCoCo mede linhas cobertas, mas não testa qualidade dos asserts (PITest recomendado)
4. **Documentar estratégia de testes** — criar `docs/qa/test-strategy.md` com pirâmide e critérios

### Quality Gates Recomendados (além de JaCoCo)

| Gate | Ferramenta | Threshold | Fase de Bloqueio |
|------|-----------|-----------|------------------|
| Cobertura de linhas | JaCoCo | 80% | `mvn verify` |
| Cobertura de branches | JaCoCo | 70% | `mvn verify` |
| Mutation score | PITest | 80% | CI/CD (opcional) |
| Vulnerabilidades | OWASP Dependency-Check | 0 critical/high | CI/CD |
| Bugs SonarQube | Sonar | 0 blocker/critical | CI/CD (se disponível) |

---

## 6. Checklist de Conclusão da Sprint 2

- ✅ JaCoCo plugin adicionado ao `pom.xml`
- ✅ Quality gates configurados (80% instruções, 70% branches)
- ✅ Exclusões de cobertura documentadas
- ✅ Gaps de cobertura identificados e priorizados
- ✅ Baseline de cobertura estimado (~78%)
- ⏳ Relatório JaCoCo real pendente (dependente de ambiente de build funcional)
- 📋 Plano de ação para Sprint 3 definido (3 classes + 1 fixture)

---

**Próxima Sprint:** Sprint 3 — Identificação de Gaps de Teste
