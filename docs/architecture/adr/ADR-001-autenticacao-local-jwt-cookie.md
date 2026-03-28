# ADR-001: Autenticacao Local com JWT em httpOnly Cookie

**Status:** Aceito
**Data:** 2026-03-28

## Contexto

O sistema prjeto-post-it precisa de autenticacao de usuarios para proteger operacoes de CRUD de post-its. O sistema e uma aplicacao local (single-instance) composta por uma SPA Vue 3 (porta 3000) e uma API Spring Boot (porta 8080) comunicando-se via REST cross-origin.

Requisitos levantados:
- Usuarios devem poder se registrar e fazer login com email/senha
- Endpoints de CRUD de postits devem ser protegidos
- A solucao deve ser stateless no servidor (sem HttpSession)
- Deve ser resistente a ataques de brute force no login
- Deve ser resistente a XSS no armazenamento do token
- Deve seguir os padroes ja estabelecidos no projeto (hexagonal architecture, RFC 9457, Flyway)

## Decisao

Implementar autenticacao local com:

1. **Spring Security** com `UserDetailsService` customizado e `BCryptPasswordEncoder` (strength 12)
2. **JWT** transportado em **cookie httpOnly** (nao em header Authorization, nao em localStorage)
3. **Token de 1 hora** de expiracao, algoritmo HS256, claims minimas (sub, name, email, iat, exp)
4. **CSRF desabilitado** — justificado por: SPA stateless, SameSite=Lax, cookie httpOnly
5. **Rate limiting** com bucket4j: 5 tentativas/minuto por IP no endpoint `/api/v1/auth/login`
6. **Erro 401 generico** no login — nao revela se o email existe ou se a senha esta incorreta
7. **CORS** restrito a `http://localhost:3000` com `allowCredentials=true`

## Alternativas Consideradas

### Opcao A: Session-based (HttpSession + JSESSIONID)
- **Pros:** Simples de implementar com Spring Security default; token revogavel (invalidar sessao); suporte nativo do Spring.
- **Contras:** Estado no servidor (memoria ou Redis); incompativel com deploy stateless sem sticky sessions; escala horizontal requer sessao distribuida (Redis); acoplamento do frontend ao ciclo de vida da sessao.

### Opcao B: JWT em localStorage + Authorization header (Bearer)
- **Pros:** Stateless; familiar para equipes que trabalham com SPAs; sem problematica de CORS/cookies.
- **Contras:** Vulneravel a XSS — qualquer script injetado pode ler o localStorage e exfiltrar o token. E o vetor de ataque mais comum em SPAs.

### Opcao C: JWT em cookie httpOnly (escolhida)
- **Pros:** Stateless; imune a XSS (JavaScript nao acessa cookie httpOnly); SameSite=Lax mitiga CSRF para maioria dos cenarios; alinhado com recomendacoes OWASP para SPAs.
- **Contras:** Configuracao de CORS mais cuidadosa (precisa de allowCredentials); token nao revogavel server-side (mitigado por expiracao curta); CSRF residual em GET com side-effects (nao aplicavel — nossos GETs sao safe).

### Opcao D: OAuth2/OIDC com provider externo (Keycloak, Auth0)
- **Pros:** Padroes maduros; SSO; delegacao de responsabilidade de seguranca.
- **Contras:** Complexidade operacional desproporcional (container extra, configuracao OIDC); overkill para aplicacao local sem requisito de SSO; latencia adicional para validacao de token com provider.

### Opcao E: Spring Security OAuth2 Resource Server
- **Pros:** Integrado ao ecossistema Spring; suporte a validacao de JWT embutido.
- **Contras:** Projetado para tokens emitidos por authorization server externo (issuer validation, JWK Set); para auth local com cookie, requer hacks que vao contra o design da lib. Filtro JWT customizado e mais transparente e simples.

## Consequencias

### O que muda
- Todos os endpoints de CRUD de postits passam a exigir autenticacao
- Nova tabela `users` no PostgreSQL (migracao Flyway V2)
- 6 novos packages com ~15 classes seguindo hexagonal architecture
- 4 novos endpoints REST sob `/api/v1/auth/**`
- 3 novas dependencias Maven (spring-security, jjwt, bucket4j)
- Nova env var obrigatoria: `JWT_SECRET`
- Frontend deve configurar Axios com `withCredentials: true`

### O que precisamos fazer depois
- Sprint DBA: executar migracao V2__create_users_table.sql
- Sprint Backend: implementar classes conforme blueprint
- Sprint Frontend: implementar telas de login/register e configurar interceptor Axios
- Futuro: migracao V3 para associar postits a usuarios (FK `user_id` na tabela `postits`)
- Futuro: se necessario logout de todos os dispositivos, adicionar campo `token_version` na tabela users

### Riscos aceitos
- **Token nao revogavel:** Um token emitido e valido por ate 1 hora mesmo apos logout. Mitigacao: cookie e removido do browser; expiracao curta limita janela de exposicao.
- **Rate limiting in-memory:** Perde estado no restart e nao funciona em multi-instance. Aceitavel para MVP single-instance. Migracao para bucket4j-redis e direta quando necessario.
- **CSRF desabilitado:** SameSite=Lax + cookie httpOnly + POST-only para mutacoes = risco residual minimo. Reavaliar se surgirem endpoints GET com side-effects.
- **Sem verificacao de email:** O registro nao confirma email. Aceitavel para uso local. Adicionar fluxo de verificacao se o sistema for para producao com usuarios externos.
