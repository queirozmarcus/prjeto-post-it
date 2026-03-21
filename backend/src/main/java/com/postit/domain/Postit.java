package com.postit.domain;

import java.time.LocalDateTime;

public record Postit(
    Long id,
    String content,
    String color,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public Postit {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("O conteúdo do Post-it não pode ser vazio.");
        }
        if (color != null && !color.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("A cor deve ser um código hexadecimal válido (ex: #FFFFFF).");
        }
    }

    public static Postit create(String content, String color) {
        return new Postit(null, content, color != null ? color : "#FFFFFF", LocalDateTime.now(), LocalDateTime.now());
    }

    public Postit withId(Long id) {
        return new Postit(id, this.content, this.color, this.createdAt, this.updatedAt);
    }
}
