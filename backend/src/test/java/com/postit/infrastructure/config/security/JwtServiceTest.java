package com.postit.infrastructure.config.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários do JwtService — carrega apenas o bean JwtService,
 * sem subir o contexto completo do Spring Boot (evita dependências de JPA/Flyway).
 */
@SpringJUnitConfig(classes = JwtService.class)
@TestPropertySource(properties = {
        "app.security.jwt-secret=test-secret-key-minimum-32-chars-ok",
        "app.security.jwt-expiration-ms=3600000"
})
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    private static final String EMAIL = "user@example.com";
    private static final String NAME  = "Test User";

    @Test
    @DisplayName("Deve gerar um token JWT não nulo e não vazio")
    void shouldGenerateValidToken() {
        String token = jwtService.generateToken(EMAIL, NAME);

        assertThat(token).isNotNull().isNotEmpty();
        // Formato JWT: três segmentos separados por ponto
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Deve extrair o email (subject) corretamente do token")
    void shouldExtractEmailFromToken() {
        String token = jwtService.generateToken(EMAIL, NAME);

        String extractedEmail = jwtService.extractEmail(token);

        assertThat(extractedEmail).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("Deve extrair o nome corretamente do token")
    void shouldExtractNameFromToken() {
        String token = jwtService.generateToken(EMAIL, NAME);

        String extractedName = jwtService.extractName(token);

        assertThat(extractedName).isEqualTo(NAME);
    }

    @Test
    @DisplayName("Deve retornar true para um token válido")
    void shouldReturnTrueForValidToken() {
        String token = jwtService.generateToken(EMAIL, NAME);

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("Deve retornar false para um token expirado")
    void shouldReturnFalseForExpiredToken() {
        // Instancia JwtService diretamente com expiração negativa para forçar token já expirado
        JwtService expiredService = new JwtService(
                "test-secret-key-minimum-32-chars-ok",
                -1L
        );
        String expiredToken = expiredService.generateToken(EMAIL, NAME);

        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false para um token adulterado")
    void shouldReturnFalseForTamperedToken() {
        String token = jwtService.generateToken(EMAIL, NAME);
        // Troca os últimos 4 caracteres da assinatura para invalidar o token
        String tamperedToken = token.substring(0, token.length() - 4) + "XXXX";

        assertThat(jwtService.isTokenValid(tamperedToken)).isFalse();
    }
}
