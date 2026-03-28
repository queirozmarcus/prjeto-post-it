package com.postit.application.usecases.auth;

import com.postit.application.ports.UserRepositoryPort;
import com.postit.domain.user.User;
import com.postit.infrastructure.adapters.in.auth.AuthResponse;
import com.postit.infrastructure.adapters.in.auth.LoginRequest;
import com.postit.infrastructure.config.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginUseCase {

    private static final Logger log = LoggerFactory.getLogger(LoginUseCase.class);

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginUseCase(
            UserRepositoryPort userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Autentica o usuário e retorna um token JWT assinado.
     * Nunca revela se o email existe ou não — sempre lança BadCredentialsException
     * com mensagem genérica para não vazar informação de enumeração de usuários.
     */
    public String login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("AUTH_LOGIN_FAILURE email={} reason=user_not_found", request.email());
                    return new BadCredentialsException("Credenciais inválidas");
                });

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            log.warn("AUTH_LOGIN_FAILURE email={} reason=bad_credentials", request.email());
            throw new BadCredentialsException("Credenciais inválidas");
        }

        log.info("AUTH_LOGIN_SUCCESS email={}", request.email());
        return jwtService.generateToken(user.email(), user.name());
    }

    public AuthResponse findAuthResponse(String token) {
        String email = jwtService.extractEmail(token);
        String name = jwtService.extractName(token);
        return new AuthResponse(email, name);
    }
}
