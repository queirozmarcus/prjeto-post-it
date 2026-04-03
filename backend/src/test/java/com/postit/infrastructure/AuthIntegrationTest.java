package com.postit.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postit.infrastructure.adapters.in.PostitRequest;
import com.postit.infrastructure.adapters.in.auth.LoginRequest;
import com.postit.infrastructure.adapters.in.auth.RegisterRequest;
import com.postit.infrastructure.containers.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String EMAIL = "integracao@example.com";
    private static final String PASSWORD = "Senha@123";  // SEC-015: política de senha — maiúscula + minúscula + dígito
    private static final String NAME = "Usuario Integracao";

    private static final String EMAIL_B = "usuario.b@example.com";
    private static final String PASSWORD_B = "Senha@456";
    private static final String NAME_B = "Usuario B";

    // Estado compartilhado entre os testes da sessão
    private static String jwtCookie;
    private static String jwtCookieB;
    private static Long postitIdOfA;

    @Test
    @Order(1)
    @DisplayName("1. POST /register deve retornar 201 e Set-Cookie com JWT")
    void step1_register() throws Exception {
        RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD, NAME);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie", containsString("jwt=")))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.name").value(NAME))
                .andReturn();

        jwtCookie = extractJwtCookie(result.getResponse());
        assertThat(jwtCookie).isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("2. POST /login com credenciais válidas deve retornar 200 e Set-Cookie com JWT")
    void step2_login() throws Exception {
        LoginRequest request = new LoginRequest(EMAIL, PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie", containsString("jwt=")))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.name").value(NAME))
                .andReturn();

        // Atualiza o cookie com o da resposta do login
        jwtCookie = extractJwtCookie(result.getResponse());
        assertThat(jwtCookie).isNotBlank();
    }

    @Test
    @Order(3)
    @DisplayName("3. GET /me com cookie válido deve retornar 200 com email e nome corretos")
    void step3_meWithValidCookie() throws Exception {
        assertThat(jwtCookie).as("Cookie JWT deve estar presente do passo anterior").isNotBlank();

        mockMvc.perform(get("/api/v1/auth/me")
                        .cookie(new jakarta.servlet.http.Cookie("jwt", jwtCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.name").value(NAME));
    }

    @Test
    @Order(4)
    @DisplayName("4. GET /api/v1/postits sem cookie deve retornar 401")
    void step4_postitsWithoutCookie() throws Exception {
        mockMvc.perform(get("/api/v1/postits"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    @DisplayName("5. POST /api/v1/postits deve criar postit para o usuário autenticado")
    void step5_createPostitAndVerifyOwnership() throws Exception {
        assertThat(jwtCookie).as("Cookie JWT deve estar presente do passo anterior").isNotBlank();

        PostitRequest request = new PostitRequest("Meu primeiro post-it", "#FF5733");

        MvcResult result = mockMvc.perform(post("/api/v1/postits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new jakarta.servlet.http.Cookie("jwt", jwtCookie)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Meu primeiro post-it"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        postitIdOfA = body.get("id").asLong();
        assertThat(postitIdOfA).isPositive();
    }

    @Test
    @Order(6)
    @DisplayName("6. GET /api/v1/postits com cookie válido deve retornar lista com o postit criado")
    void step6_postitsWithValidCookieReturnsOwnPostits() throws Exception {
        assertThat(jwtCookie).as("Cookie JWT deve estar presente do passo anterior").isNotBlank();

        mockMvc.perform(get("/api/v1/postits")
                        .cookie(new jakarta.servlet.http.Cookie("jwt", jwtCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value("Meu primeiro post-it"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @Order(7)
    @DisplayName("7. Registra userB — isolamento de ownership entre usuários")
    void step7_registerUserB() throws Exception {
        RegisterRequest request = new RegisterRequest(EMAIL_B, PASSWORD_B, NAME_B);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        jwtCookieB = extractJwtCookie(result.getResponse());
        assertThat(jwtCookieB).isNotBlank();
    }

    @Test
    @Order(8)
    @DisplayName("8. GET /postits com cookie de userB deve retornar lista vazia (não vê postit de A)")
    void step8_userBDoesNotSeeUserAPostits() throws Exception {
        assertThat(jwtCookieB).as("Cookie de userB deve estar presente").isNotBlank();

        mockMvc.perform(get("/api/v1/postits")
                        .cookie(new jakarta.servlet.http.Cookie("jwt", jwtCookieB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @Order(9)
    @DisplayName("9. GET /postits/{id} com cookie de userB deve retornar 404 (não revela existência)")
    void step9_userBCannotReadUserAPostitById() throws Exception {
        assertThat(jwtCookieB).as("Cookie de userB deve estar presente").isNotBlank();
        assertThat(postitIdOfA).as("ID do postit de A deve estar disponível").isNotNull();

        mockMvc.perform(get("/api/v1/postits/{id}", postitIdOfA)
                        .cookie(new jakarta.servlet.http.Cookie("jwt", jwtCookieB)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(10)
    @DisplayName("10. DELETE /postits/{id} com cookie de userB deve retornar 403")
    void step10_userBCannotDeleteUserAPostit() throws Exception {
        assertThat(jwtCookieB).as("Cookie de userB deve estar presente").isNotBlank();
        assertThat(postitIdOfA).as("ID do postit de A deve estar disponível").isNotNull();

        mockMvc.perform(delete("/api/v1/postits/{id}", postitIdOfA)
                        .cookie(new jakarta.servlet.http.Cookie("jwt", jwtCookieB)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(11)
    @DisplayName("11. POST /logout deve retornar 204 e Set-Cookie com MaxAge=0")
    void step11_logout() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    @Test
    @Order(12)
    @DisplayName("12. GET /me após logout (sem cookie) deve retornar 401")
    void step12_meAfterLogout() throws Exception {
        // Sem cookie na requisição — simula acesso após logout
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(13)
    @DisplayName("13. POST /login com senha errada deve retornar 401 com mensagem 'Credenciais inválidas'")
    void step13_loginWithWrongPassword() throws Exception {
        LoginRequest request = new LoginRequest(EMAIL, "senha-errada");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Credenciais inválidas"));
    }

    @Test
    @Order(14)
    @DisplayName("14. POST /register com email duplicado deve retornar 409")
    void step14_registerDuplicateEmail() throws Exception {
        // EMAIL já foi registrado no step 1
        RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD, NAME);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(15)
    @DisplayName("15. POST /api/v1/postits sem cookie deve retornar 401 (pós-V4: coluna NOT NULL)")
    void step15_shouldRejectUnauthenticatedPostCreation() throws Exception {
        // Pós-V4: user_id é NOT NULL na tabela. Qualquer postit criado sem autenticação
        // seria rejeitado pelo Spring Security antes de chegar no banco — garantia dupla.
        PostitRequest request = new PostitRequest("Tentativa sem auth", "#FFFFFF");

        mockMvc.perform(post("/api/v1/postits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // Extrai o valor do cookie "jwt" do header Set-Cookie da resposta
    private String extractJwtCookie(MockHttpServletResponse response) {
        String setCookieHeader = response.getHeader("Set-Cookie");
        if (setCookieHeader == null) {
            return null;
        }
        // Header format: "jwt=<token>; Path=/; Max-Age=3600; HttpOnly; SameSite=Lax"
        for (String part : setCookieHeader.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("jwt=")) {
                return trimmed.substring("jwt=".length());
            }
        }
        return null;
    }
}
