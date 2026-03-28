package com.postit.application.usecases.auth;

import com.postit.application.ports.UserRepositoryPort;
import com.postit.domain.user.User;
import com.postit.infrastructure.adapters.in.auth.AuthResponse;
import com.postit.infrastructure.adapters.in.auth.RegisterRequest;
import com.postit.infrastructure.config.security.JwtService;
import com.postit.shared.exception.EmailAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegisterUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterUseCase.class);

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public RegisterUseCase(
            UserRepositoryPort userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            log.warn("AUTH_REGISTER_FAILURE email={} reason=email_already_exists", request.email());
            throw new EmailAlreadyExistsException(request.email());
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = User.create(request.email(), passwordHash, request.name());
        User saved = userRepository.save(user);

        log.info("AUTH_REGISTER_SUCCESS email={}", saved.email());
        return new AuthResponse(saved.email(), saved.name());
    }
}
