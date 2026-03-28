package com.postit.shared.exception;

public class PostitAccessDeniedException extends RuntimeException {

    public PostitAccessDeniedException(Long id) {
        super("Acesso negado ao post-it com ID: " + id);
    }
}
