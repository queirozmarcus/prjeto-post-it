package com.postit.domain;

import java.time.LocalDateTime;

public record Postit(
    Long id,
    String content,
    String color,
    Long userId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public Postit {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("O conteúdo do Post-it não pode ser vazio.");
        }
        if (content.length() > 120) {
            throw new IllegalArgumentException("O conteúdo deve ter entre 1 e 120 caracteres.");
        }
        if (color != null && !color.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("A cor deve ser um código hexadecimal válido (ex: #FFFFFF).");
        }
        // userId é nullable neste sprint — postits existentes não têm owner
    }

    // Factory method com userId explícito
    public static Postit create(String content, String color, Long userId) {
        return new Postit(null, content, color != null ? color : "#FFFFFF", userId, LocalDateTime.now(), LocalDateTime.now());
    }

    // Overload backward-compatible: userId = null para não quebrar testes e código existentes
    public static Postit create(String content, String color) {
        return create(content, color, null);
    }

    public Postit withId(Long id) {
        return new Postit(id, this.content, this.color, this.userId, this.createdAt, this.updatedAt);
    }
}
