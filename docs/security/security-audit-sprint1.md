# Security Audit Report — prjeto-post-it
**Data:** 2026-03-28
**Auditor:** security-test-engineer
**Escopo:** Sprint 1 — Auditoria completa do backend Java 21 + Spring Boot 3.4.3

---

## Sumário Executivo

| Severidade | Quantidade |
|------------|-----------|
| CRITICO    | 2         |
| ALTO       | 4         |
| MEDIO      | 5         |
| BAIXO / INFORMATIVO | 4 |
| **Total**  | **15**    |

---

## Findings

---

### CRITICO

---

#### SEC-001

- **Categoria:** OWASP A01 — Broken Access Control / IDOR
- **Descrição:** O domínio `Postit` não possui campo `userId` (owner). A tabela `postits` não tem coluna de propriedade. Todo usuário autenticado pode ler, modificar e apagar qualquer post-it de qualquer outro usuário, bastando conhecer (ou iterar sobre) o `id` numérico sequencial (`BIGSERIAL`). A ausência de um ownership check é total — inexiste em nenhuma camada: domínio, use case, controller ou banco de dados.
- **Evidência:**

  `backend/src/main/resources/db/migration/V1__create_postit_table.sql`:
  ```sql
  CREATE TABLE IF NOT EXISTS postits (
      id BIGSERIAL PRIMARY KEY,
      content TEXT NOT NULL,
      color VARCHAR(7) DEFAULT '#FFFFFF',
      created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
  );
  -- Sem coluna user_id / owner_id
  ```

  `backend/src/main/java/com/postit/domain/Postit.java`:
  ```java
  public record Postit(
      Long id,
      String content,
      String color,
      LocalDateTime createdAt,
      LocalDateTime updatedAt
  // Sem campo ownerId ou userId
  ) {}
  ```

  `backend/src/main/java/com/postit/infrastructure/adapters/in/PostitController.java`:
  ```java
  @GetMapping("/{id}")
  public ResponseEntity<PostitResponse> findById(@PathVariable Long id) {
      return service.findById(id)   // nenhum contexto de autenticação passado
              .map(PostitResponse::fromDomain)
              .map(ResponseEntity::ok)
              .orElse(ResponseEntity.notFound().build());
  }
  ```

- **Remediação:**
  1. Criar migration `V3__add_owner_to_postits.sql` adicionando `user_id BIGINT NOT NULL REFERENCES users(id)` na tabela `postits`.
  2. Adicionar campo `Long ownerId` ao domain record `Postit`.
  3. No `PostitController`, injetar o `Authentication` do Spring Security e extrair o email do usuário logado em todos os endpoints (`GET /{id}`, `PUT /{id}`, `DELETE /{id}`, `GET /`).
  4. No use case e no repositório, filtrar sempre por `ownerId = currentUser.id`. Lançar `PostitNotFoundException` (não 403) quando o id existe mas pertence a outro usuário, para não revelar existência do recurso.
  5. Teste obrigatório:
     ```java
     @Test
     void shouldReturn404_whenAccessingOtherUsersPostit() { ... }
     ```

---

#### SEC-002

- **Categoria:** OWASP A02 — Cryptographic Failures / JWT Secret Fallback Inseguro
- **Descrição:** O `application.yml` define um fallback hardcoded para o secret JWT. Quando a variável de ambiente `JWT_SECRET` não está definida (ausente no `docker-compose.yml` ou em execução local), a aplicação sobe com o secret `changeme-replace-in-production-min-32-chars`, que é público por estar no repositório. Qualquer pessoa com acesso ao código pode forjar tokens JWT válidos para qualquer usuário. O `docker-compose.yml` não define `JWT_SECRET`, confirmando que o fallback é ativado em todos os ambientes atuais.
- **Evidência:**

  `backend/src/main/resources/application.yml` (linha 29):
  ```yaml
  app:
    security:
      jwt-secret: ${JWT_SECRET:changeme-replace-in-production-min-32-chars}
  ```

  `docker-compose.yml` — ausência total de `JWT_SECRET` no bloco `environment` do serviço `api`.

- **Remediação:**
  1. Remover o valor de fallback do `application.yml`:
     ```yaml
     jwt-secret: ${JWT_SECRET}
     ```
     A aplicação falhará na inicialização se `JWT_SECRET` não estiver definida — comportamento correto (fail-fast).
  2. Gerar um secret forte: `openssl rand -hex 32` (64 caracteres = 256 bits).
  3. Adicionar `JWT_SECRET` ao `docker-compose.yml` via variável de ambiente do host ou arquivo `.env` (gitignored).
  4. Documentar a variável no `.env.example` com instrução de geração.
  5. A chave HMAC com 32 bytes (256 bits) já satisfaz o requisito mínimo do JJWT para HS256. O valor atual de fallback tem apenas 43 bytes mas o problema principal é ser público.

---

### ALTO

---

#### SEC-003

- **Categoria:** OWASP A05 — Security Misconfiguration / Security Headers Ausentes
- **Descrição:** O `SecurityConfig` não configura nenhum security header explícito além do mínimo provido pelo Spring Security por padrão. Os seguintes headers de segurança críticos estão ausentes ou com valores padrão insuficientes:

  | Header | Status | Impacto |
  |--------|--------|---------|
  | `Strict-Transport-Security` (HSTS) | Ausente (desabilitado em sessão stateless) | Permite downgrade para HTTP; cookie JWT trafega em claro |
  | `Content-Security-Policy` | Ausente | Sem proteção contra XSS e data injection |
  | `Referrer-Policy` | Ausente | URL com dados sensíveis pode vazar via Referer |
  | `Permissions-Policy` | Ausente | Browser features irrestrias |
  | `X-Frame-Options` | Habilitado por padrão (DENY) pelo Spring Security | OK |
  | `X-Content-Type-Options` | Habilitado por padrão (nosniff) pelo Spring Security | OK |

- **Evidência:**

  `SecurityConfig.java` — ausência de `.headers(headers -> headers.xxx())` no `HttpSecurity`.

- **Remediação:**
  Adicionar bloco `headers` no `SecurityFilterChain`:
  ```java
  .headers(headers -> headers
      .httpStrictTransportSecurity(hsts -> hsts
          .includeSubDomains(true)
          .maxAgeInSeconds(31536000))
      .contentSecurityPolicy(csp -> csp
          .policyDirectives("default-src 'self'; frame-ancestors 'none'"))
      .referrerPolicy(ref -> ref
          .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
  )
  ```
  Para `Permissions-Policy`, adicionar um filtro de resposta ou usar `StaticHeadersWriter`.

---

#### SEC-004

- **Categoria:** OWASP A04 — Insecure Design / Rate Limit Ausente no Register
- **Descrição:** O rate limiting (5 req/min por IP via Bucket4j) é aplicado exclusivamente no endpoint `POST /api/v1/auth/login`. O endpoint `POST /api/v1/auth/register` não tem nenhuma limitação. Um atacante pode:
  1. Criar ilimitadas contas para enumeração de emails válidos (a resposta 409 confirma existência do email — SEC-010).
  2. Realizar ataques de enumeração via registro em massa.
  3. Gerar carga sobre o banco de dados com BCrypt(12) — cada chamada tem custo computacional elevado, podendo ser usado como vetor de DoS.
- **Evidência:**

  `AuthController.java` linha 48-60: método `register` não chama `rateLimiterService.tryConsume(ip)`.

  ```java
  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(
          @Valid @RequestBody RegisterRequest request,
          HttpServletResponse response) {
      // Sem rate limit
      AuthResponse authResponse = registerUseCase.register(request);
      ...
  }
  ```

- **Remediação:**
  1. Injetar `HttpServletRequest` no método `register` e adicionar o mesmo bloco de rate limit presente no `login`.
  2. Considerar um limite mais restritivo para register (ex: 3/hora por IP) em relação ao login (5/min).
  3. Mover a lógica de rate limit para um `Filter` ou `HandlerInterceptor` aplicado a todo `/api/v1/auth/**`, evitando duplicação.

---

#### SEC-005

- **Categoria:** OWASP A05 — Security Misconfiguration / Actuator com `show-details: always`
- **Descrição:** O Actuator está configurado com `show-details: always` para o endpoint `health`. Isso expõe detalhes de conectividade da base de dados (URL, status da conexão, pool) para qualquer cliente sem autenticação, já que `/actuator/health` está em `permitAll()`. Essa informação é útil para reconhecimento de infraestrutura.
- **Evidência:**

  `application.yml` linhas 32-38:
  ```yaml
  management:
    endpoints:
      web:
        exposure:
          include: health,info
    endpoint:
      health:
        show-details: always
  ```

  `SecurityConfig.java` linha 65:
  ```java
  .requestMatchers("/actuator/health", ...).permitAll()
  ```

- **Remediação:**
  1. Alterar para `show-details: when-authorized` ou `never` em produção.
  2. Opção recomendada para produção:
     ```yaml
     endpoint:
       health:
         show-details: when-authorized
     ```
  3. Se health check do Docker precisar de detalhes, usar `show-components: never` com endpoint separado interno.

---

#### SEC-006

- **Categoria:** OWASP A02 — Cryptographic Failures / Cookie `Secure=false`
- **Descrição:** O cookie JWT é criado com `secure(false)` tanto no `setJwtCookie()` quanto no `logout()`. Isso permite que o cookie trafegue sobre conexões HTTP não criptografadas, expondo o token JWT a ataques de interceptação (man-in-the-middle). Embora o contexto atual seja de desenvolvimento local, o código contém o valor hardcoded `false` — em vez de ser controlado por variável de ambiente ou profile — aumentando o risco de esse valor permanecer em produção por esquecimento.
- **Evidência:**

  `AuthController.java` linha 125:
  ```java
  ResponseCookie cookie = ResponseCookie.from("jwt", token)
          .httpOnly(true)
          .secure(false)  // Em produção: alterar para true (requer HTTPS)
          ...
  ```

  `AuthController.java` linha 108 (logout):
  ```java
  ResponseCookie expiredCookie = ResponseCookie.from("jwt", "")
          .httpOnly(true)
          .secure(false)  // Em produção: alterar para true (requer HTTPS)
  ```

- **Remediação:**
  1. Injetar um `@Value("${app.security.cookie-secure:true}")` booleano no `AuthController`.
  2. No `application.yml` de desenvolvimento, setar `cookie-secure: false`. No perfil de produção ou via variável de ambiente, manter `true` (default).
  3. Resultado: o valor `secure(false)` nunca estará hardcoded no código Java.

---

### MEDIO

---

#### SEC-007

- **Categoria:** OWASP A09 — Security Logging Failures / Ausência de Logs de Autenticação
- **Descrição:** Nenhum evento de autenticação é registrado em log: logins com sucesso, falhas de autenticação, tentativas de acesso com token inválido e registros de novos usuários são silenciosos. Isso impede auditoria, detecção de ataques de força bruta que contornem o rate limit e resposta a incidentes.
- **Evidência:**

  `LoginUseCase.java`, `AuthController.java`, `JwtAuthFilter.java` — ausência total de chamadas `log.info(...)` ou `log.warn(...)` para eventos de auth.

  `JwtAuthFilter.java` linhas 42-44: token inválido descartado silenciosamente:
  ```java
  if (!jwtService.isTokenValid(token)) {
      filterChain.doFilter(request, response);
      return;
  }
  ```

- **Remediação:**
  Adicionar logging estruturado nos pontos-chave:
  - Login com sucesso: `log.info("auth.login.success email={}", email)` (nunca logar senha ou token completo)
  - Login com falha: `log.warn("auth.login.failure email={} ip={}", email, ip)`
  - Token inválido: `log.warn("auth.token.invalid ip={} path={}", ip, path)`
  - Novo registro: `log.info("auth.register email={}", email)`
  - Rate limit atingido: `log.warn("auth.ratelimit ip={}", ip)`

---

#### SEC-008

- **Categoria:** OWASP A07 — Auth Failures / Ausência de Refresh Token
- **Descrição:** O sistema emite apenas um access token com validade de 1 hora (3.600.000 ms). Não há mecanismo de refresh token. As implicações são: (a) o usuário é forçado a relogar a cada hora, degradando a experiência; (b) não há como invalidar um token comprometido antes do vencimento, pois não existe lista de revogação — um token roubado permanece válido por até 1 hora; (c) o frontend não tem tratamento para re-autenticação silenciosa, redirecionando abruptamente para `/login` ao receber 401.
- **Evidência:**

  `application.yml` linha 30: `jwt-expiration-ms: 3600000`

  `postitApi.ts` linhas 40-43:
  ```typescript
  if (error.response?.status === 401) {
      window.location.href = '/login'; // Redirect abrupto, sem tentativa de refresh
  }
  ```

- **Remediação:**
  1. Implementar refresh token de longa duração (ex: 7 dias) armazenado em cookie httpOnly separado.
  2. Access token com validade mais curta (ex: 15 minutos).
  3. Endpoint `POST /api/v1/auth/refresh` que valida o refresh token e emite novo access token.
  4. No frontend, interceptor de 401 deve tentar refresh antes de redirecionar para login.
  5. Considerar armazenar refresh tokens no banco para permitir revogação individual.

---

#### SEC-009

- **Categoria:** OWASP A05 — Security Misconfiguration / Swagger Exposto sem Restrição
- **Descrição:** O Swagger UI (`/swagger-ui/**`) e a especificação OpenAPI (`/v3/api-docs/**`) estão expostos publicamente sem qualquer autenticação. Isso entrega ao atacante um mapa completo da API: endpoints, parâmetros, schemas, exemplos de requisição e resposta, e modelos de erro — reduzindo significativamente o esforço de reconhecimento.
- **Evidência:**

  `SecurityConfig.java` linhas 62-67:
  ```java
  .requestMatchers(
          "/api/v1/auth/**",
          "/actuator/health",
          "/swagger-ui/**",
          "/v3/api-docs/**"
  ).permitAll()
  ```

- **Remediação:**
  1. Em produção, desabilitar Swagger completamente via profile:
     ```yaml
     springdoc:
       api-docs:
         enabled: ${SWAGGER_ENABLED:false}
       swagger-ui:
         enabled: ${SWAGGER_ENABLED:false}
     ```
  2. Em ambientes de desenvolvimento/staging, proteger com autenticação Basic ou restringir por IP.
  3. Remover `/swagger-ui/**` e `/v3/api-docs/**` do `permitAll()` quando desabilitados.

---

#### SEC-010

- **Categoria:** OWASP A07 — Auth Failures / Enumeração de Emails via Register
- **Descrição:** O endpoint `POST /api/v1/auth/register` retorna HTTP 409 quando o email já está cadastrado, com mensagem explícita "Email já cadastrado: {email}". Isso permite que um atacante enumere emails registrados no sistema com certeza absoluta, enviando requests de registro em massa — especialmente crítico dado que não há rate limit no register (SEC-004).
- **Evidência:**

  `EmailAlreadyExistsException.java`:
  ```java
  public EmailAlreadyExistsException(String email) {
      super("Email já cadastrado: " + email);
  }
  ```

  `GlobalExceptionHandler.java` linhas 26-31: retorna o detalhe da exceção como `detail` no ProblemDetail com status 409.

- **Remediação:**
  1. Aplicação de rate limit em `/register` (SEC-004) mitiga o impacto.
  2. Avaliar se retornar 409 é aceitável para o produto. A alternativa mais segura é retornar sempre 201 (processamento assíncrono) ou 200 com mensagem genérica "Se o email não estiver cadastrado, a conta será criada", mas isso degrada UX de cadastro legítimo.
  3. No mínimo: remover o email do detalhe da mensagem de erro. A mensagem genérica "Email já cadastrado" sem repetir o endereço é suficiente para UX sem adicionar valor ao atacante.

---

#### SEC-011

- **Categoria:** OWASP A05 — Security Misconfiguration / Rate Limiter em Memória (In-Process)
- **Descrição:** O `RateLimiterService` usa um `ConcurrentHashMap` em memória para armazenar os buckets por IP. Em ambiente com múltiplas instâncias da aplicação (horizontal scaling) ou após restart do container, todos os contadores são perdidos. Atacantes podem contornar o limite reiniciando conexões, trocando de IP via proxy rotativo, ou explorando múltiplos nós.
- **Evidência:**

  `RateLimiterService.java` linha 20:
  ```java
  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
  ```

  O próprio comentário no código reconhece: "Em produção, considere usar armazenamento distribuído (Redis)".

- **Remediação:**
  1. Para produção com múltiplas instâncias: substituir pelo Bucket4j com backend Redis (`bucket4j-redis`).
  2. Para instância única: o comportamento atual é aceitável como proteção básica.
  3. Adicionar `X-RateLimit-Remaining` e `Retry-After` nos headers de resposta 429.

---

### BAIXO / INFORMATIVO

---

#### SEC-012

- **Categoria:** OWASP A02 — Cryptographic Failures / `show-sql: true` em Produção
- **Descrição:** `spring.jpa.show-sql: true` está habilitado no `application.yml`. Em produção, isso pode expor queries SQL (incluindo valores de parâmetros dependendo do driver) nos logs de aplicação, vazando estrutura de dados e potencialmente valores sensíveis. A configuração não está protegida por profile.
- **Evidência:**

  `application.yml` linha 14: `show-sql: true`

- **Remediação:**
  Desabilitar por padrão e habilitar apenas em perfil de desenvolvimento:
  ```yaml
  spring:
    jpa:
      show-sql: ${JPA_SHOW_SQL:false}
  ```
  Ou usar `application-dev.yml` com override.

---

#### SEC-013

- **Categoria:** JWT Security / Ausência de `issuer` e `audience` Claims
- **Descrição:** O token JWT gerado por `JwtService.generateToken()` não inclui os claims `iss` (issuer) e `aud` (audience). Sem esses claims, tokens gerados por outra aplicação com o mesmo secret (ou vazados de outro ambiente) podem ser aceitos. A validação no `JwtAuthFilter` também não verifica esses claims.
- **Evidência:**

  `JwtService.java` linhas 36-43:
  ```java
  return Jwts.builder()
          .subject(email)
          .claim("name", name)
          .issuedAt(new Date(now))
          .expiration(new Date(now + expirationMs))
          .signWith(key)
          .compact();
  // Sem .issuer() e .audience()
  ```

- **Remediação:**
  Adicionar claims e validação correspondente:
  ```java
  .issuer("postit-api")
  .audience().add("postit-frontend").and()
  ```
  E no parser:
  ```java
  Jwts.parser()
      .requireIssuer("postit-api")
      .requireAudience("postit-frontend")
      .verifyWith(key)
  ```

---

#### SEC-014

- **Categoria:** CORS / Informativo
- **Descrição:** A configuração CORS está correta para o contexto atual: `allowedOrigins` restritos a `localhost:3000` e `localhost:5173` (sem wildcard), `allowCredentials=true` apenas com origens explícitas (configuração válida pelo spec CORS), métodos e métodos necessários listados. Ponto de atenção: `allowedHeaders: ["*"]` permite qualquer header de request. Isso raramente é um risco por si só mas pode facilitar ataques se headers personalizados forem processados sem sanitização.
- **Evidência:**

  `SecurityConfig.java` linhas 83-84:
  ```java
  configuration.setAllowedHeaders(List.of("*"));
  configuration.setAllowCredentials(true);
  ```

- **Remediação:**
  Enumerar apenas os headers necessários:
  ```java
  configuration.setAllowedHeaders(List.of("Content-Type", "Authorization", "Cookie"));
  ```
  Para produção, substituir `localhost` pelas origens reais do frontend.

---

#### SEC-015

- **Categoria:** Informativo / Password Validation
- **Descrição:** O `RegisterRequest` valida senha com `@Size(min = 8)`, mas não impõe complexidade (caracteres especiais, dígitos, maiúsculas). Com BCrypt(12), o custo computacional de ataque offline é alto, mas senhas como `12345678` ou `password` ainda são aceitas. O frontend não implementa medidor de força de senha.
- **Evidência:**

  `RegisterRequest.java` linha 13:
  ```java
  @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
  String password,
  ```

- **Remediação:**
  Adicionar validação de padrão:
  ```java
  @Pattern(
      regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$",
      message = "Senha deve conter letras maiúsculas, minúsculas, números e caractere especial"
  )
  ```
  Ou integrar biblioteca como `passay` para políticas configuráveis.

---

## Resumo de Ações para Sprint 2

### Prioridade 1 — Bloquear antes do próximo deploy

| ID | Ação |
|----|------|
| SEC-001 | Implementar ownership em postits (migration + domain + use case + controller) |
| SEC-002 | Remover fallback do JWT secret; exigir `JWT_SECRET` via env var |
| SEC-006 | Tornar `cookie.secure` configurável por variável de ambiente |

### Prioridade 2 — Implementar no Sprint 2

| ID | Ação |
|----|------|
| SEC-003 | Adicionar security headers (HSTS, CSP, Referrer-Policy, Permissions-Policy) |
| SEC-004 | Aplicar rate limit no endpoint `/register` |
| SEC-005 | Alterar `show-details` para `when-authorized` |
| SEC-007 | Adicionar logging estruturado de eventos de autenticação |
| SEC-009 | Desabilitar Swagger em produção via profile/env var |

### Prioridade 3 — Backlog de segurança

| ID | Ação |
|----|------|
| SEC-008 | Implementar refresh token com revogação |
| SEC-010 | Remover email do detalhe da exceção `EmailAlreadyExistsException` |
| SEC-011 | Migrar rate limiter para Redis em ambiente multi-instância |
| SEC-012 | Desabilitar `show-sql` fora do perfil de desenvolvimento |
| SEC-013 | Adicionar claims `iss` e `aud` ao JWT e validação correspondente |
| SEC-014 | Enumerar headers CORS explicitamente |
| SEC-015 | Adicionar validação de complexidade de senha |

---

## Testes de Segurança Recomendados (Sprint 2)

Os findings acima geram os seguintes testes obrigatórios a serem implementados pelo QA:

```java
// SEC-001: IDOR
@Test void shouldReturn404_whenUserAccessesOtherUsersPostit()
@Test void shouldReturn404_whenUserUpdatesOtherUsersPostit()
@Test void shouldReturn404_whenUserDeletesOtherUsersPostit()
@Test void findAll_shouldReturnOnlyCurrentUserPostits()

// SEC-002: JWT forjado com secret público
@Test void shouldReturn401_whenTokenSignedWithDefaultFallbackSecret()

// SEC-003: Headers
@Test void shouldReturnHSTSHeader()
@Test void shouldReturnCSPHeader()
@Test void errorResponseShouldNotLeakStackTrace()

// SEC-004: Rate limit register
@Test void shouldReturn429_afterExceedingRegisterRateLimit()

// SEC-005: Actuator
@Test void actuatorHealth_shouldNotExposeInternalDetails_whenUnauthenticated()

// SEC-007: Logging (verificação via captura de appender em teste)
@Test void shouldLogFailedLoginAttempt()
```
