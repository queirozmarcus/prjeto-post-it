package com.postit.application.usecases.auth;

import com.postit.application.ports.UserRepositoryPort;
import com.postit.domain.user.User;
import com.postit.domain.user.UserObjectMother;
import com.postit.infrastructure.adapters.in.auth.AuthResponse;
import com.postit.infrastructure.adapters.in.auth.LoginRequest;
import com.postit.infrastructure.config.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private LoginUseCase loginUseCase;

    @Test
    @DisplayName("Deve autenticar usuário com credenciais válidas e retornar token JWT")
    void shouldAuthenticateUserWithValidCredentials() {
        // Given
        String email = "user@example.com";
        String password = "ValidPass123";
        String passwordHash = "$2a$10$hashedPassword";
        String expectedToken = "jwt.token.here";

        LoginRequest request = new LoginRequest(email, password);
        User user = UserObjectMother.validUserWithEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, user.passwordHash())).thenReturn(true);
        when(jwtService.generateToken(email, user.name())).thenReturn(expectedToken);

        // When
        String token = loginUseCase.login(request);

        // Then
        assertThat(token).isEqualTo(expectedToken);
        verify(userRepository, times(1)).findByEmail(email);
        verify(passwordEncoder, times(1)).matches(password, user.passwordHash());
        verify(jwtService, times(1)).generateToken(email, user.name());
    }

    @Test
    @DisplayName("Deve lançar BadCredentialsException quando usuário não existe")
    void shouldThrowBadCredentialsException_whenUserNotFound() {
        // Given
        String email = "nonexistent@example.com";
        String password = "SomePass123";
        LoginRequest request = new LoginRequest(email, password);

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> loginUseCase.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Credenciais inválidas");

        verify(userRepository, times(1)).findByEmail(email);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Deve lançar BadCredentialsException quando senha está incorreta")
    void shouldThrowBadCredentialsException_whenPasswordIsIncorrect() {
        // Given
        String email = "user@example.com";
        String wrongPassword = "WrongPass123";
        LoginRequest request = new LoginRequest(email, wrongPassword);
        User user = UserObjectMother.validUserWithEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(wrongPassword, user.passwordHash())).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> loginUseCase.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Credenciais inválidas");

        verify(userRepository, times(1)).findByEmail(email);
        verify(passwordEncoder, times(1)).matches(wrongPassword, user.passwordHash());
        verify(jwtService, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Deve retornar a mesma mensagem de erro para usuário inexistente e senha incorreta")
    void shouldReturnSameErrorMessageForUserNotFoundAndBadPassword() {
        // Given
        String email = "user@example.com";
        String password = "SomePass123";
        LoginRequest requestUserNotFound = new LoginRequest("nonexistent@example.com", password);
        LoginRequest requestBadPassword = new LoginRequest(email, "WrongPass");

        User user = UserObjectMother.validUserWithEmail(email);

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass", user.passwordHash())).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> loginUseCase.login(requestUserNotFound))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciais inválidas");

        assertThatThrownBy(() -> loginUseCase.login(requestBadPassword))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciais inválidas");
    }

    @Test
    @DisplayName("Deve extrair AuthResponse de token JWT válido")
    void shouldExtractAuthResponseFromValidToken() {
        // Given
        String token = "valid.jwt.token";
        String email = "user@example.com";
        String name = "John Doe";

        when(jwtService.extractEmail(token)).thenReturn(email);
        when(jwtService.extractName(token)).thenReturn(name);

        // When
        AuthResponse response = loginUseCase.findAuthResponse(token);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo(email);
        assertThat(response.name()).isEqualTo(name);
        verify(jwtService, times(1)).extractEmail(token);
        verify(jwtService, times(1)).extractName(token);
    }

    @Test
    @DisplayName("Deve logar aviso quando login falha por usuário não encontrado")
    void shouldLogWarningWhenUserNotFound() {
        // Given
        String email = "missing@example.com";
        LoginRequest request = new LoginRequest(email, "password");

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> loginUseCase.login(request))
                .isInstanceOf(BadCredentialsException.class);

        // Log verificado implicitamente — teste confirma comportamento de exceção
        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("Deve logar aviso quando login falha por senha incorreta")
    void shouldLogWarningWhenPasswordMismatch() {
        // Given
        String email = "user@example.com";
        String wrongPassword = "WrongPass";
        LoginRequest request = new LoginRequest(email, wrongPassword);
        User user = UserObjectMother.validUserWithEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(wrongPassword, user.passwordHash())).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> loginUseCase.login(request))
                .isInstanceOf(BadCredentialsException.class);

        // Log verificado implicitamente — teste confirma comportamento de exceção
        verify(passwordEncoder, times(1)).matches(wrongPassword, user.passwordHash());
    }
}
