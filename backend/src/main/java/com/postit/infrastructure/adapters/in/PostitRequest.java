package com.postit.infrastructure.adapters.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PostitRequest(
    @NotBlank(message = "O conteúdo é obrigatório")
    @Size(max = 120, message = "O conteúdo deve ter no máximo 120 caracteres")
    String content,

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "A cor deve ser um hexadecimal válido")
    String color
) {}
