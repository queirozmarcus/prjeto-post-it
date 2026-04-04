package com.postit.domain.user;

import java.time.LocalDateTime;

public class UserObjectMother {

    public static User validUser() {
        return new User(1L, "user@example.com", "$2a$10$hashedPassword123", "John Doe", LocalDateTime.now(), LocalDateTime.now());
    }

    public static User validUserWithId(Long id) {
        return new User(id, "user@example.com", "$2a$10$hashedPassword123", "John Doe", LocalDateTime.now(), LocalDateTime.now());
    }

    public static User validUserWithEmail(String email) {
        return new User(1L, email, "$2a$10$hashedPassword123", "John Doe", LocalDateTime.now(), LocalDateTime.now());
    }

    public static User userToCreate(String email, String passwordHash, String name) {
        return User.create(email, passwordHash, name);
    }

    public static User userToCreate() {
        return User.create("newuser@example.com", "$2a$10$hashedPassword456", "Jane Smith");
    }
}
