package com.postit.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    @DisplayName("Deve criar User válido com todos os campos preenchidos")
    void shouldCreateValidUser() {
        // Given/When
        User user = UserObjectMother.validUser();

        // Then
        assertThat(user).isNotNull();
        assertThat(user.email()).isEqualTo("user@example.com");
        assertThat(user.passwordHash()).isNotNull();
        assertThat(user.name()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Deve criar User através do factory method create()")
    void shouldCreateUserViaFactoryMethod() {
        // Given/When
        User user = User.create("test@example.com", "$2a$10$hash", "Test User");

        // Then
        assertThat(user).isNotNull();
        assertThat(user.id()).isNull();
        assertThat(user.email()).isEqualTo("test@example.com");
        assertThat(user.passwordHash()).isEqualTo("$2a$10$hash");
        assertThat(user.name()).isEqualTo("Test User");
        assertThat(user.createdAt()).isNotNull();
        assertThat(user.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve criar nova instância com id através de withId()")
    void shouldCreateNewInstanceWithId() {
        // Given
        User userWithoutId = UserObjectMother.userToCreate();

        // When
        User userWithId = userWithoutId.withId(99L);

        // Then
        assertThat(userWithId).isNotNull();
        assertThat(userWithId.id()).isEqualTo(99L);
        assertThat(userWithId.email()).isEqualTo(userWithoutId.email());
        assertThat(userWithId.passwordHash()).isEqualTo(userWithoutId.passwordHash());
        assertThat(userWithId.name()).isEqualTo(userWithoutId.name());
    }

    @Test
    @DisplayName("Deve lançar exceção quando email é null")
    void shouldThrowException_whenEmailIsNull() {
        // Given/When/Then
        assertThatThrownBy(() -> new User(1L, null, "$2a$10$hash", "John Doe", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email do usuário não pode ser vazio");
    }

    @Test
    @DisplayName("Deve lançar exceção quando email é vazio")
    void shouldThrowException_whenEmailIsBlank() {
        // Given/When/Then
        assertThatThrownBy(() -> new User(1L, "   ", "$2a$10$hash", "John Doe", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email do usuário não pode ser vazio");
    }

    @Test
    @DisplayName("Deve lançar exceção quando passwordHash é null")
    void shouldThrowException_whenPasswordHashIsNull() {
        // Given/When/Then
        assertThatThrownBy(() -> new User(1L, "user@example.com", null, "John Doe", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hash de senha não pode ser vazio");
    }

    @Test
    @DisplayName("Deve lançar exceção quando passwordHash é vazio")
    void shouldThrowException_whenPasswordHashIsBlank() {
        // Given/When/Then
        assertThatThrownBy(() -> new User(1L, "user@example.com", "   ", "John Doe", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hash de senha não pode ser vazio");
    }

    @Test
    @DisplayName("Deve lançar exceção quando name é null")
    void shouldThrowException_whenNameIsNull() {
        // Given/When/Then
        assertThatThrownBy(() -> new User(1L, "user@example.com", "$2a$10$hash", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nome do usuário não pode ser vazio");
    }

    @Test
    @DisplayName("Deve lançar exceção quando name é vazio")
    void shouldThrowException_whenNameIsBlank() {
        // Given/When/Then
        assertThatThrownBy(() -> new User(1L, "user@example.com", "$2a$10$hash", "   ", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nome do usuário não pode ser vazio");
    }

    @Test
    @DisplayName("Deve aceitar email com formato válido")
    void shouldAcceptValidEmailFormat() {
        // Given/When
        User user = User.create("valid.email+tag@example.com", "$2a$10$hash", "User Name");

        // Then
        assertThat(user.email()).isEqualTo("valid.email+tag@example.com");
    }

    @Test
    @DisplayName("Deve aceitar name com espaços e caracteres especiais")
    void shouldAcceptNameWithSpacesAndSpecialChars() {
        // Given/When
        User user = User.create("user@example.com", "$2a$10$hash", "José da Silva Júnior");

        // Then
        assertThat(user.name()).isEqualTo("José da Silva Júnior");
    }
}
