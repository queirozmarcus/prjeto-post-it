package com.postit.application.ports;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageQueryTest {

    @Test
    @DisplayName("Deve rejeitar page negativo")
    void shouldRejectNegativePage() {
        assertThatThrownBy(() -> new PageQuery(-1, 10, "createdAt", "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page deve ser >= 0");
    }

    @Test
    @DisplayName("Deve rejeitar size igual a zero")
    void shouldRejectZeroSize() {
        assertThatThrownBy(() -> new PageQuery(0, 0, "createdAt", "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size deve estar entre 1 e 100");
    }

    @Test
    @DisplayName("Deve rejeitar size acima de 100")
    void shouldRejectSizeAbove100() {
        assertThatThrownBy(() -> new PageQuery(0, 101, "createdAt", "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size deve estar entre 1 e 100");
    }

    @Test
    @DisplayName("Deve usar 'createdAt' como sortField padrão quando sortField for nulo")
    void shouldDefaultSortFieldWhenNull() {
        // Given / When
        PageQuery query = new PageQuery(0, 10, null, "asc");

        // Then
        assertThat(query.sortField()).isEqualTo("createdAt");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    @DisplayName("Deve usar 'createdAt' como sortField padrão quando sortField for em branco")
    void shouldDefaultSortFieldWhenBlank(String blank) {
        // Given / When
        PageQuery query = new PageQuery(0, 10, blank, "asc");

        // Then
        assertThat(query.sortField()).isEqualTo("createdAt");
    }

    @Test
    @DisplayName("Deve usar 'desc' como sortDirection padrão quando sortDirection for nulo")
    void shouldDefaultSortDirectionWhenNull() {
        // Given / When
        PageQuery query = new PageQuery(0, 10, "createdAt", null);

        // Then
        assertThat(query.sortDirection()).isEqualTo("desc");
    }

    @Test
    @DisplayName("Deve criar PageQuery com parâmetros válidos sem lançar exceção")
    void shouldCreateWithValidParams() {
        // Given / When
        PageQuery query = new PageQuery(3, 50, "content", "asc");

        // Then
        assertThat(query.page()).isEqualTo(3);
        assertThat(query.size()).isEqualTo(50);
        assertThat(query.sortField()).isEqualTo("content");
        assertThat(query.sortDirection()).isEqualTo("asc");
    }

    @Test
    @DisplayName("ofDefaults deve retornar page=0, size=20, sortField=createdAt, sortDirection=desc")
    void shouldCreateDefaults() {
        // When
        PageQuery defaults = PageQuery.ofDefaults();

        // Then
        assertThat(defaults.page()).isZero();
        assertThat(defaults.size()).isEqualTo(20);
        assertThat(defaults.sortField()).isEqualTo("createdAt");
        assertThat(defaults.sortDirection()).isEqualTo("desc");
    }
}
