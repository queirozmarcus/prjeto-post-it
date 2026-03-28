package com.postit.domain.user;

import java.time.LocalDateTime;

public record User(
    Long id,
    String email,
    String passwordHash,
    String name,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public User {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("O email do usuário não pode ser vazio.");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("O hash de senha não pode ser vazio.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome do usuário não pode ser vazio.");
        }
    }

    public static User create(String email, String passwordHash, String name) {
        return new User(null, email, passwordHash, name, LocalDateTime.now(), LocalDateTime.now());
    }

    public User withId(Long id) {
        return new User(id, this.email, this.passwordHash, this.name, this.createdAt, this.updatedAt);
    }
}
