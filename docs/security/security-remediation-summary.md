# Security Remediation Summary — prjeto-post-it

**Auditoria original:** Sprint 1 (2026-03-28)
**Última atualização:** Sprint 6 (2026-03-28)
**Auditor original:** security-test-engineer

---

## Sumário de Status

| Severidade | Total | Resolvido | Aceito/Deferido |
|------------|-------|-----------|-----------------|
| CRITICO    | 2     | 2         | 0               |
| ALTO       | 4     | 4         | 0               |
| MEDIO      | 5     | 3         | 2               |
| BAIXO/INFO | 4     | 2         | 2               |
| **Total**  | **15**| **11**    | **4**           |

---

## Findings — Status Detalhado

### CRITICOS

#### SEC-001 — IDOR: Ausência de ownership em postits
- **Status:** RESOLVIDO (Sprint 5)
- **Resolução:**
  - `V3__add_user_id_to_postits.sql`: coluna `user_id BIGINT` adicionada com FK para `users`
  - `V4__set_user_id_not_null.sql`: constraint `NOT NULL` aplicada (Sprint 6)
  - Domain record `Postit` recebeu campo `userId`
  - `PostitUseCase`: todos os métodos filtram por `userId` do usuário autenticado
  - `PostitController`: extrai `userId` do `Authentication` do Spring Security em todos os endpoints
  - GET `/{id}` retorna 404 (não 403) quando postit pertence a outro usuário — não revela existência
  - DELETE `/{id}` retorna 403 quando postit pertence a outro usuário (por design: a posse é verificável pelo dono)
  - Testes: 9 cenários de integração em `AuthIntegrationTest` + testes unitários de use case

#### SEC-002 — JWT secret com fallback hardcoded publicamente
- **Status:** RESOLVIDO (Sprint 3)
- **Resolução:**
  - `application.yml`: fallback removido — `jwt-secret: ${JWT_SECRET}` sem valor default
  - Aplicação falha na inicialização (fail-fast) se `JWT_SECRET` não estiver definida
  - `docker-compose.yml`: `JWT_SECRET` injetada via variável de ambiente do host
  - `.env.example`: documenta geração via `openssl rand -hex 32`

---

### ALTOS

#### SEC-003 — Security headers ausentes (HSTS, CSP, Referrer-Policy, Permissions-Policy)
- **Status:** RESOLVIDO (Sprint 2)
- **Resolução:**
  - `SecurityConfig.java`: bloco `.headers()` adicionado com lambda block conforme Spring Security 6
  - HSTS: `includeSubDomains(true)`, `maxAgeInSeconds(31536000)`
  - CSP: `default-src 'self'; frame-ancestors 'none'`
  - Referrer-Policy: `STRICT_ORIGIN_WHEN_CROSS_ORIGIN`
  - Permissions-Policy: via `StaticHeadersWriter` (sintaxe do header não suportada nativamente pelo Spring Security 6)
  - Nota técnica: `permissionsPolicy()` retorna tipo diferente em Spring Security 6 — requer lambda block statements, não method chaining

#### SEC-004 — Rate limit ausente no endpoint `/register`
- **Status:** RESOLVIDO (Sprint 2)
- **Resolução:**
  - `AuthController.register()`: `HttpServletRequest` injetado e `rateLimiterService.tryConsume(ip)` aplicado
  - Mesmo bucket que o login (5 req/min por IP) — coerente com proteção existente

#### SEC-005 — Actuator com `show-details: always` sem autenticação
- **Status:** RESOLVIDO (Sprint 2)
- **Resolução:**
  - `application.yml`: `show-details: when-authorized`
  - Docker health check mantido via endpoint `/actuator/health` que retorna status sem detalhes internos para não-autenticados

#### SEC-006 — Cookie JWT com `secure: false` hardcoded
- **Status:** RESOLVIDO (Sprint 2)
- **Resolução:**
  - `AuthController.java`: `@Value("${app.security.cookie-secure:false}")` injetado
  - `application.yml` dev: `cookie-secure: false`
  - Em produção, variável `APP_SECURITY_COOKIE_SECURE=true` via env var
  - O valor `false` nunca mais está hardcoded no código Java

---

### MEDIOS

#### SEC-007 — Ausência de logs de autenticação
- **Status:** RESOLVIDO (Sprint 2)
- **Resolução:**
  - `LoginUseCase`: `log.info("auth.login.success")` e `log.warn("auth.login.failure")`
  - `RegisterUseCase`: `log.info("auth.register")`
  - `JwtAuthFilter`: `log.warn("auth.token.invalid")` ao descartar token inválido
  - `AuthController`: `log.warn("auth.ratelimit")` ao atingir rate limit
  - Nenhum dado sensível (senha, token completo) é logado

#### SEC-008 — Ausência de refresh token
- **Status:** ACEITO — Deferido para backlog
- **Justificativa:** Escopo do produto atual não requer sessões longas. Token de 1 hora é aceitável para MVP. Implementação de refresh token requer mudanças significativas no frontend e backend. O risco residual é mitigado pelo httpOnly cookie (não acessível via JavaScript) e pela implementação de logout que invalida a sessão no cliente.
- **Ação futura:** Implementar em sprint dedicada quando houver demanda de UX por sessões mais longas.

#### SEC-009 — Swagger exposto publicamente sem autenticação
- **Status:** RESOLVIDO (Sprint 2)
- **Resolução:**
  - `application.yml`: `springdoc.swagger-ui.enabled: ${SWAGGER_ENABLED:false}` e `springdoc.api-docs.enabled: ${SWAGGER_ENABLED:false}`
  - Por padrão (produção): Swagger desabilitado
  - Em desenvolvimento: `SWAGGER_ENABLED=true` via env var
  - `SecurityConfig`: rotas de Swagger mantidas no `permitAll()` mas inacessíveis quando desabilitadas

#### SEC-010 — Enumeração de emails via 409 em `/register`
- **Status:** ACEITO — Mitigado parcialmente
- **Justificativa:** Retornar 409 é uma escolha de UX deliberada — o formulário de registro mostra ao usuário que o email já existe, evitando frustração. Essa informação seria descobrível via tentativa de login de qualquer forma. O risco real de enumeração em massa foi mitigado pelo rate limit no `/register` (SEC-004 resolvido). A mensagem foi ajustada para não repetir o email no detalhe do erro.
- **Mitigação aplicada:** `EmailAlreadyExistsException` — mensagem genérica "Email já cadastrado" sem incluir o endereço.

#### SEC-011 — Rate limiter in-process (não distribuído)
- **Status:** ACEITO — Adequado para escala atual
- **Justificativa:** O sistema roda em instância única (Docker Compose, não Kubernetes com HPA). Rate limiter em memória é suficiente. Migrar para Redis adicionaria dependência de infraestrutura sem benefício concreto agora.
- **Ação futura:** Migrar para `bucket4j-redis` quando o serviço for escalado horizontalmente.

---

### BAIXOS / INFORMATIVOS

#### SEC-012 — `show-sql: true` em produção
- **Status:** RESOLVIDO (Sprint 2)
- **Resolução:**
  - `application.yml`: `show-sql: ${JPA_SHOW_SQL:false}`
  - Em desenvolvimento local, setar `JPA_SHOW_SQL=true` se necessário para debug

#### SEC-013 — Ausência de claims `iss` e `aud` no JWT
- **Status:** ACEITO — Risco baixo no contexto atual
- **Justificativa:** Sistema de instância única com secret exclusivo. Não há múltiplos serviços compartilhando o mesmo secret que possam aceitar tokens de forma cruzada. O risco descrito (token de outro ambiente aceito) é mitigado pela prática de secrets distintos por ambiente.
- **Ação futura:** Adicionar claims quando o sistema evoluir para múltiplos serviços.

#### SEC-014 — `allowedHeaders: ["*"]` no CORS
- **Status:** RESOLVIDO (Sprint 2)
- **Resolução:**
  - `SecurityConfig.java`: `setAllowedHeaders(List.of("Content-Type", "Cookie"))`
  - Apenas headers necessários para a aplicação Vue 3 + axios com cookies

#### SEC-015 — Política de senha sem complexidade obrigatória
- **Status:** RESOLVIDO (Sprint 2)
- **Resolução:**
  - `RegisterRequest.java`: `@Pattern` aplicado exigindo maiúscula + minúscula + dígito (mínimo)
  - Padrão: `^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$`
  - Testes de integração em `AuthIntegrationTest` usam senha `Senha@123` conforme política
  - Nota: caractere especial não foi incluído no regex para não quebrar UX — trade-off deliberado

---

## Risco Residual Aceito

| ID | Risco | Nível | Mitigação em vigor |
|----|-------|-------|-------------------|
| SEC-008 | Sem refresh token — sessão expira em 1h | BAIXO | httpOnly cookie, logout funcional |
| SEC-010 | 409 confirma existência de email | BAIXO | Rate limit em /register |
| SEC-011 | Rate limit in-process (não distribuído) | BAIXO | Instância única; adequado |
| SEC-013 | JWT sem claims iss/aud | INFORMATIVO | Secret exclusivo por ambiente |

---

## Linha do Tempo de Remediação

| Sprint | Findings resolvidos |
|--------|---------------------|
| Sprint 2 | SEC-003, SEC-004, SEC-005, SEC-006, SEC-007, SEC-009, SEC-012, SEC-014, SEC-015 |
| Sprint 3 | SEC-002 |
| Sprint 5 | SEC-001 (IDOR — ownership implementado) |
| Sprint 6 | SEC-001 (complemento: V4 NOT NULL aplicada, risco V3 nullable fechado) |
