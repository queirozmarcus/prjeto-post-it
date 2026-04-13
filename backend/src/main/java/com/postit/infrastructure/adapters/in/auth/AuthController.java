package com.postit.infrastructure.adapters.in.auth;

import com.postit.application.ports.UserRepositoryPort;
import com.postit.application.usecases.auth.LoginUseCase;
import com.postit.application.usecases.auth.RegisterUseCase;
import com.postit.infrastructure.config.security.RateLimiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Registro e autenticação de usuários")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final RateLimiterService rateLimiterService;
    private final UserRepositoryPort userRepository;

    // SEC-006: controlado por variável de ambiente — false em dev, true em produção HTTPS
    @Value("${app.security.cookie-secure}")
    private boolean cookieSecure;

    public AuthController(
            RegisterUseCase registerUseCase,
            LoginUseCase loginUseCase,
            RateLimiterService rateLimiterService,
            UserRepositoryPort userRepository) {
        this.registerUseCase = registerUseCase;
        this.loginUseCase = loginUseCase;
        this.rateLimiterService = rateLimiterService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar novo usuário", description = "Cria conta e autentica via cookie JWT")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        String ip = httpRequest.getRemoteAddr();
        if (!rateLimiterService.tryConsume(ip)) {
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
            pd.setType(URI.create("https://api.postits.local/errors/rate-limit"));
            pd.setTitle("Limite de requisições excedido");
            pd.setDetail("Muitas tentativas de registro. Aguarde antes de tentar novamente.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(pd);
        }

        // Registrar e depois autenticar para obter o token
        registerUseCase.register(request);
        String token = loginUseCase.login(new LoginRequest(request.email(), request.password()));
        setJwtCookie(response, token);

        AuthResponse authResponse = loginUseCase.findAuthResponse(token);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário", description = "Valida credenciais e emite cookie JWT")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        String ip = httpRequest.getRemoteAddr();
        if (!rateLimiterService.tryConsume(ip)) {
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
            pd.setType(URI.create("https://api.postits.local/errors/rate-limit"));
            pd.setTitle("Limite de requisições excedido");
            pd.setDetail("Muitas tentativas de login. Aguarde antes de tentar novamente.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(pd);
        }

        String token = loginUseCase.login(request);
        setJwtCookie(response, token);

        AuthResponse authResponse = loginUseCase.findAuthResponse(token);
        return ResponseEntity.ok(authResponse);
    }

    @GetMapping("/me")
    @Operation(summary = "Obter dados do usuário autenticado", description = "Retorna email e nome do usuário logado via cookie JWT")
    public ResponseEntity<AuthResponse> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // authentication.getName() retorna o email — populado pelo JwtAuthFilter via UserDetails.getUsername()
        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .map(user -> ResponseEntity.ok(new AuthResponse(user.email(), user.name(), null)))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/logout")
    @Operation(summary = "Encerrar sessão", description = "Expira o cookie JWT imediatamente (MaxAge=0)")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        // Expirar o cookie JWT com MaxAge=0 — mesmos atributos do setJwtCookie, exceto maxAge
        ResponseCookie expiredCookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(cookieSecure)  // SEC-006: configurado via app.security.cookie-secure
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());
        return ResponseEntity.noContent().build();
    }

    /**
     * Define o cookie JWT httpOnly na resposta.
     * O atributo secure é controlado pela propriedade app.security.cookie-secure:
     * false em desenvolvimento local (sem HTTPS), true em produção.
     */
    private void setJwtCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(cookieSecure)  // SEC-006: configurado via app.security.cookie-secure
                .path("/")
                .maxAge(Duration.ofHours(1))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
