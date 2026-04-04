package com.postit.infrastructure.adapters.in.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.postit.application.ports.UserRepositoryPort;
import com.postit.application.usecases.auth.LoginUseCase;
import com.postit.application.usecases.auth.RegisterUseCase;
import com.postit.domain.user.User;
import com.postit.infrastructure.config.security.JwtService;
import com.postit.infrastructure.config.security.RateLimiterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.postit.infrastructure.config.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.security.cookie-secure=false",
        "app.security.jwt-secret=test-secret-key-minimum-32-chars-ok",
        "app.security.jwt-expiration-ms=3600000"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterUseCase registerUseCase;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @MockitoBean
    private UserRepositoryPort userRepository;

    // Necessário para JwtAuthFilter e SecurityConfig
    @MockitoBean
    private JwtService jwtService;

    // Necessário para UserDetailsServiceImpl referenciado pelo SecurityConfig
    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("Deve registrar usuário e retornar cookie JWT no header Set-Cookie")
    void shouldRegisterAndReturnCookie() throws Exception {
        // Given — senha atende à política SEC-015: maiúscula + minúscula + dígito
        RegisterRequest request = new RegisterRequest("joao@example.com", "Senha123", "Joao Silva");
        AuthResponse authResponse = new AuthResponse("joao@example.com", "Joao Silva");

        // SEC-004: register agora aplica rate limit
        when(rateLimiterService.tryConsume(anyString())).thenReturn(true);
        when(registerUseCase.register(any(RegisterRequest.class))).thenReturn(authResponse);
        when(loginUseCase.login(any(LoginRequest.class))).thenReturn("mocked-jwt-token");

        // When / Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie", containsString("jwt=")))
                .andExpect(jsonPath("$.email").value("joao@example.com"))
                .andExpect(jsonPath("$.name").value("Joao Silva"));
    }

    @Test
    @DisplayName("Deve autenticar usuário e retornar cookie JWT no header Set-Cookie")
    void shouldLoginAndReturnCookie() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("joao@example.com", "Senha123");
        AuthResponse authResponse = new AuthResponse("joao@example.com", "Joao Silva");

        when(rateLimiterService.tryConsume(anyString())).thenReturn(true);
        when(loginUseCase.login(any(LoginRequest.class))).thenReturn("mocked-jwt-token");
        when(loginUseCase.findAuthResponse("mocked-jwt-token")).thenReturn(authResponse);

        // When / Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie", containsString("jwt=")))
                .andExpect(jsonPath("$.email").value("joao@example.com"))
                .andExpect(jsonPath("$.name").value("Joao Silva"));
    }

    @Test
    @DisplayName("Deve retornar 200 com email e nome para usuário autenticado no endpoint /me")
    void shouldReturnMeForAuthenticatedUser() throws Exception {
        // Given
        User user = new User(1L, "joao@example.com", "hashed-pass", "Joao Silva",
                LocalDateTime.now(), LocalDateTime.now());
        when(userRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(user));

        // When / Then — usa authentication() post processor para popular Authentication sem filtros de segurança
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "joao@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/api/v1/auth/me")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("joao@example.com"))
                .andExpect(jsonPath("$.name").value("Joao Silva"));
    }

    @Test
    @DisplayName("Deve retornar 204 e cookie JWT expirado (MaxAge=0) no logout")
    void shouldLogoutAndExpireCookie() throws Exception {
        // When / Then
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie", containsString("jwt=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    @Test
    @DisplayName("Deve retornar 429 quando o rate limit de login for excedido")
    void shouldReturn429WhenRateLimitExceeded() throws Exception {
        // Given — as primeiras 5 chamadas são permitidas, a 6ª é bloqueada
        LoginRequest request = new LoginRequest("joao@example.com", "senha123");
        AuthResponse authResponse = new AuthResponse("joao@example.com", "Joao Silva");

        when(rateLimiterService.tryConsume(anyString()))
                .thenReturn(true)   // 1ª
                .thenReturn(true)   // 2ª
                .thenReturn(true)   // 3ª
                .thenReturn(true)   // 4ª
                .thenReturn(true)   // 5ª
                .thenReturn(false); // 6ª — bloqueada

        when(loginUseCase.login(any(LoginRequest.class))).thenReturn("mocked-jwt-token");
        when(loginUseCase.findAuthResponse(anyString())).thenReturn(authResponse);

        String body = objectMapper.writeValueAsString(request);

        // 5 requisições devem passar
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        // A 6ª deve retornar 429
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests());
    }
}
