package com.postit.infrastructure.adapters.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PostitRequest(
    @NotBlank(message = "O conteúdo é obrigatório")
    String content,

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "A cor deve ser um hexadecimal válido")
    String color
) {}
