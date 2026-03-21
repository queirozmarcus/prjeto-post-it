package com.postit.shared.exception;

public class PostitNotFoundException extends RuntimeException {
    public PostitNotFoundException(Long id) {
        super("Não foi possível encontrar uma nota com o ID: " + id);
    }
}
