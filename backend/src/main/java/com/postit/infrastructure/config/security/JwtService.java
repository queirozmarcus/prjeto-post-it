package com.postit.infrastructure.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${app.security.jwt-secret}") String secret,
            @Value("${app.security.jwt-expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Gera um token JWT para o usuário autenticado.
     *
     * @param email email do usuário (subject)
     * @param name  nome do usuário (claim adicional)
     * @return token JWT assinado
     */
    public String generateToken(String email, String name) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .claim("name", name)
                .issuer("prjeto-post-it")          // SEC-013: identifica o emissor do token
                .audience().add("postit-app").and() // SEC-013: identifica o receptor esperado
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * Extrai o email (subject) do token.
     *
     * @param token token JWT
     * @return email do usuário
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extrai o nome do token.
     *
     * @param token token JWT
     * @return nome do usuário
     */
    public String extractName(String token) {
        return extractAllClaims(token).get("name", String.class);
    }

    /**
     * Valida se o token é válido — assinatura correta e não expirado.
     *
     * @param token token JWT
     * @return {@code true} se válido, {@code false} caso contrário
     */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Faz o parse do token e retorna todos os claims; lança JwtException em caso de falha.
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
