package com.postit.infrastructure.adapters.in;

import com.postit.application.ports.PageResult;
import com.postit.domain.Postit;
import com.postit.domain.PostitObjectMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PagedPostitResponse — conversão de PageResult")
class PagedPostitResponseTest {

    @Test
    @DisplayName("Deve mapear todos os campos de paginação corretamente")
    void shouldMapAllPaginationFields() {
        // Given
        List<Postit> items = List.of(PostitObjectMother.validPostit());
        PageResult<Postit> pageResult = new PageResult<>(items, 2, 10, 45L, 5);

        // When
        PagedPostitResponse response = PagedPostitResponse.fromPageResult(pageResult);

        // Then
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(45L);
        assertThat(response.totalPages()).isEqualTo(5);
    }

    @Test
    @DisplayName("Deve converter lista vazia sem erro e retornar content vazio")
    void shouldHandleEmptyContentList() {
        // Given
        PageResult<Postit> emptyPage = new PageResult<>(List.of(), 0, 20, 0L, 0);

        // When
        PagedPostitResponse response = PagedPostitResponse.fromPageResult(emptyPage);

        // Then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
    }

    @Test
    @DisplayName("Deve converter cada Postit para PostitResponse com os campos corretos")
    void shouldConvertEachPostitToPostItResponse() {
        // Given
        LocalDateTime createdAt = LocalDateTime.of(2024, 6, 15, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 6, 15, 11, 30);
        Postit postit = new Postit(7L, "Estudar para a prova", "#00FF00", 1L, createdAt, updatedAt);
        PageResult<Postit> pageResult = new PageResult<>(List.of(postit), 0, 20, 1L, 1);

        // When
        PagedPostitResponse response = PagedPostitResponse.fromPageResult(pageResult);

        // Then
        assertThat(response.content()).hasSize(1);
        PostitResponse item = response.content().get(0);
        assertThat(item.id()).isEqualTo(7L);
        assertThat(item.content()).isEqualTo("Estudar para a prova");
        assertThat(item.color()).isEqualTo("#00FF00");
        assertThat(item.createdAt()).isEqualTo(createdAt);
        assertThat(item.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("Deve converter múltiplos itens preservando ordem e valores individuais")
    void shouldConvertMultipleItemsPreservingOrderAndValues() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Postit first  = new Postit(1L, "Primeiro",  "#FF0000", 1L, now, now);
        Postit second = new Postit(2L, "Segundo",   "#00FF00", 1L, now, now);
        Postit third  = new Postit(3L, "Terceiro",  "#0000FF", 1L, now, now);
        PageResult<Postit> pageResult = new PageResult<>(List.of(first, second, third), 0, 20, 3L, 1);

        // When
        PagedPostitResponse response = PagedPostitResponse.fromPageResult(pageResult);

        // Then
        assertThat(response.content()).hasSize(3);
        assertThat(response.content()).extracting(PostitResponse::content)
                .containsExactly("Primeiro", "Segundo", "Terceiro");
        assertThat(response.content()).extracting(PostitResponse::id)
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("Deve manter page=0 e size correto quando há apenas uma página")
    void shouldKeepPageZeroAndCorrectSizeForSinglePage() {
        // Given
        List<Postit> items = List.of(PostitObjectMother.validPostit());
        PageResult<Postit> singlePage = new PageResult<>(items, 0, 20, 1L, 1);

        // When
        PagedPostitResponse response = PagedPostitResponse.fromPageResult(singlePage);

        // Then
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.content()).hasSize(1);
    }
}
