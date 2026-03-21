package com.postit.domain;

import java.time.LocalDateTime;

public class PostitObjectMother {

    public static Postit validPostit() {
        return new Postit(1L, "Lembrar de comprar leite", "#FF0000", LocalDateTime.now(), LocalDateTime.now());
    }

    public static Postit postitToCreate() {
        return Postit.create("Comprar café", "#00FF00");
    }

    public static Postit postitWithInvalidColor() {
        // Ignora a validação do construtor forçando a criação via reflexão ou apenas documentando a tentativa
        // Como o record tem validação no construtor compacto, usaremos uma string inválida e capturaremos a exceção no teste
        return null; // Apenas para referência, o teste chamará o construtor diretamente
    }
}
