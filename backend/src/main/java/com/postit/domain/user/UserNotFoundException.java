package com.postit.domain.user;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String email) {
        super("Usuário não encontrado com o email: " + email);
    }
}
