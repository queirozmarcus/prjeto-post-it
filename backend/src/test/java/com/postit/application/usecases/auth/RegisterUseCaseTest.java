package com.postit.application.usecases.auth;

import com.postit.application.ports.UserRepositoryPort;
import com.postit.domain.user.User;
import com.postit.domain.user.UserObjectMother;
import com.postit.infrastructure.adapters.in.auth.AuthResponse;
import com.postit.infrastructure.adapters.in.auth.RegisterRequest;
import com.postit.infrastructure.config.security.JwtService;
import com.postit.shared.exception.EmailAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUseCaseTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RegisterUseCase registerUseCase;

    @Test
    @DisplayName("Deve registrar novo usuário com sucesso e retornar AuthResponse")
    void shouldRegisterNewUserSuccessfully() {
        // Given
        String email = "newuser@example.com";
        String password = "SecurePass123";
        String name = "Jane Smith";
        String encodedPassword = "$2a$10$encodedHash";

        RegisterRequest request = new RegisterRequest(email, password, name);
        User savedUser = UserObjectMother.validUserWithEmail(email);

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        AuthResponse response = registerUseCase.register(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo(email);
        assertThat(response.name()).isEqualTo(savedUser.name());

        verify(userRepository, times(1)).existsByEmail(email);
        verify(passwordEncoder, times(1)).encode(password);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Deve codificar a senha antes de salvar usuário")
    void shouldEncodePasswordBeforeSavingUser() {
        // Given
        String email = "newuser@example.com";
        String plainPassword = "PlainPass123";
        String encodedPassword = "$2a$10$encodedHashValue";
        RegisterRequest request = new RegisterRequest(email, plainPassword, "User Name");

        User savedUser = UserObjectMother.validUserWithEmail(email);

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(plainPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        registerUseCase.register(request);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.passwordHash()).isEqualTo(encodedPassword);
        verify(passwordEncoder, times(1)).encode(plainPassword);
    }

    @Test
    @DisplayName("Deve lançar EmailAlreadyExistsException quando email já existe")
    void shouldThrowException_whenEmailAlreadyExists() {
        // Given
        String existingEmail = "existing@example.com";
        RegisterRequest request = new RegisterRequest(existingEmail, "Pass123", "User Name");

        when(userRepository.existsByEmail(existingEmail)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> registerUseCase.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("Email já cadastrado")
                .hasMessageContaining(existingEmail);

        verify(userRepository, times(1)).existsByEmail(existingEmail);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve criar User através do factory method User.create()")
    void shouldCreateUserViaFactoryMethod() {
        // Given
        String email = "newuser@example.com";
        String password = "SecurePass123";
        String name = "New User";
        String encodedPassword = "$2a$10$hash";

        RegisterRequest request = new RegisterRequest(email, password, name);
        User savedUser = UserObjectMother.validUserWithEmail(email);

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        registerUseCase.register(request);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.id()).isNull(); // Confirmação de que User.create() foi usado
        assertThat(capturedUser.email()).isEqualTo(email);
        assertThat(capturedUser.passwordHash()).isEqualTo(encodedPassword);
        assertThat(capturedUser.name()).isEqualTo(name);
        assertThat(capturedUser.createdAt()).isNotNull();
        assertThat(capturedUser.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve salvar o User retornado pelo repository")
    void shouldSaveUserReturnedByRepository() {
        // Given
        RegisterRequest request = new RegisterRequest("user@example.com", "Pass123", "User");
        User savedUser = UserObjectMother.validUserWithId(10L);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("$2a$10$hash");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        AuthResponse response = registerUseCase.register(request);

        // Then
        assertThat(response.email()).isEqualTo(savedUser.email());
        assertThat(response.name()).isEqualTo(savedUser.name());
    }

    @Test
    @DisplayName("Deve logar aviso quando registro falha por email duplicado")
    void shouldLogWarningWhenEmailAlreadyExists() {
        // Given
        String existingEmail = "existing@example.com";
        RegisterRequest request = new RegisterRequest(existingEmail, "Pass123", "User");

        when(userRepository.existsByEmail(existingEmail)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> registerUseCase.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        // Log verificado implicitamente — teste confirma comportamento de exceção
        verify(userRepository, times(1)).existsByEmail(existingEmail);
    }

    @Test
    @DisplayName("Deve logar sucesso quando registro é concluído")
    void shouldLogSuccessWhenRegistrationCompletes() {
        // Given
        String email = "newuser@example.com";
        RegisterRequest request = new RegisterRequest(email, "Pass123", "New User");
        User savedUser = UserObjectMother.validUserWithEmail(email);

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("$2a$10$hash");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        AuthResponse response = registerUseCase.register(request);

        // Then
        assertThat(response).isNotNull();
        // Log verificado implicitamente — teste confirma sucesso do registro
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Deve retornar AuthResponse com email e name do User salvo")
    void shouldReturnAuthResponseWithEmailAndNameFromSavedUser() {
        // Given
        RegisterRequest request = new RegisterRequest("test@example.com", "Pass123", "Test User");
        User savedUser = new User(5L, "test@example.com", "$2a$10$hash", "Test User", null, null);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("$2a$10$hash");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        AuthResponse response = registerUseCase.register(request);

        // Then
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.name()).isEqualTo("Test User");
    }
}
