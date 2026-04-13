package com.postit.infrastructure.adapters.out;

import com.postit.application.ports.PageQuery;
import com.postit.application.ports.PageResult;
import com.postit.domain.Postit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostitPersistenceAdapter — whitelist de campos de ordenação (mapSortField)")
class PostitPersistenceAdapterUnitTest {

    @Mock
    private PostitJpaRepository jpaRepository;

    private PostitPersistenceAdapter adapter;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        adapter = new PostitPersistenceAdapter(jpaRepository);
    }

    // --- helper para montar entidade mínima válida ---

    private PostitEntity entityWithId(Long id) {
        return new PostitEntity(id, "Conteúdo teste", "#FFFFFF", USER_ID,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private Page<PostitEntity> singleItemPage(PostitEntity entity) {
        return new PageImpl<>(List.of(entity));
    }

    // --- Campos válidos da whitelist ---

    @Test
    @DisplayName("Campo 'createdAt' deve ser repassado ao Pageable sem alteração")
    void shouldPassCreatedAtSortFieldToPageable() {
        // Given
        PageQuery pageQuery = new PageQuery(0, 10, "createdAt", "desc");
        when(jpaRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(singleItemPage(entityWithId(1L)));

        // When
        adapter.findAllByUserId(USER_ID, pageQuery);

        // Then
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpaRepository).findByUserId(eq(USER_ID), captor.capture());
        Sort.Order order = captor.getValue().getSort().getOrderFor("createdAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("Campo 'updatedAt' deve ser repassado ao Pageable sem alteração")
    void shouldPassUpdatedAtSortFieldToPageable() {
        // Given
        PageQuery pageQuery = new PageQuery(0, 10, "updatedAt", "asc");
        when(jpaRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(singleItemPage(entityWithId(1L)));

        // When
        adapter.findAllByUserId(USER_ID, pageQuery);

        // Then
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpaRepository).findByUserId(eq(USER_ID), captor.capture());
        Sort.Order order = captor.getValue().getSort().getOrderFor("updatedAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("Campo 'color' deve ser repassado ao Pageable sem alteração")
    void shouldPassColorSortFieldToPageable() {
        // Given
        PageQuery pageQuery = new PageQuery(0, 10, "color", "asc");
        when(jpaRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(singleItemPage(entityWithId(1L)));

        // When
        adapter.findAllByUserId(USER_ID, pageQuery);

        // Then
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpaRepository).findByUserId(eq(USER_ID), captor.capture());
        Sort.Order order = captor.getValue().getSort().getOrderFor("color");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    // --- Campos inválidos caem no fallback ---

    @ParameterizedTest(name = "Campo inválido ''{0}'' deve fazer fallback para 'createdAt'")
    @ValueSource(strings = { "malicious", "'; DROP TABLE postits; --", "userId", "id", "content" })
    @DisplayName("Campos fora da whitelist devem fazer fallback para 'createdAt' sem lançar exceção")
    void shouldFallbackToCreatedAt_whenSortFieldIsNotWhitelisted(String invalidField) {
        // Given — PageQuery aceita qualquer string em sortField; o adapter é quem filtra
        PageQuery pageQuery = new PageQuery(0, 10, invalidField, "desc");
        when(jpaRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // When — não deve lançar exceção
        PageResult<Postit> result = adapter.findAllByUserId(USER_ID, pageQuery);

        // Then — Pageable recebido ordena por createdAt (fallback), não pelo campo inválido
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpaRepository).findByUserId(eq(USER_ID), captor.capture());
        Sort sort = captor.getValue().getSort();
        assertThat(sort.getOrderFor("createdAt")).isNotNull();
        assertThat(sort.getOrderFor(invalidField)).isNull();
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Campo vazio deve fazer fallback para 'createdAt' (PageQuery normaliza para 'createdAt')")
    void shouldFallbackToCreatedAt_whenSortFieldIsBlank() {
        // Given — PageQuery normaliza campo em branco para "createdAt" no construtor compacto
        PageQuery pageQuery = new PageQuery(0, 10, "", "desc");
        when(jpaRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // When
        adapter.findAllByUserId(USER_ID, pageQuery);

        // Then
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpaRepository).findByUserId(eq(USER_ID), captor.capture());
        Sort.Order order = captor.getValue().getSort().getOrderFor("createdAt");
        assertThat(order).isNotNull();
    }

    // --- Mapeamento de PageResult ---

    @Test
    @DisplayName("Deve mapear corretamente os metadados de paginação do Page JPA para PageResult")
    void shouldMapPageMetadataFromJpaPageToPageResult() {
        // Given
        PostitEntity entity = entityWithId(42L);
        Page<PostitEntity> jpaPage = new PageImpl<>(
                List.of(entity),
                org.springframework.data.domain.PageRequest.of(1, 5),
                12L
        );
        PageQuery pageQuery = new PageQuery(1, 5, "createdAt", "desc");
        when(jpaRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(jpaPage);

        // When
        PageResult<Postit> result = adapter.findAllByUserId(USER_ID, pageQuery);

        // Then
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.totalElements()).isEqualTo(12L);
        assertThat(result.totalPages()).isEqualTo(3); // ceil(12/5)
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).id()).isEqualTo(42L);
    }

    @Test
    @DisplayName("Deve retornar content vazio quando o repositório não encontra resultados")
    void shouldReturnEmptyContent_whenRepositoryReturnsEmptyPage() {
        // Given
        PageQuery pageQuery = new PageQuery(0, 20, "createdAt", "desc");
        when(jpaRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // When
        PageResult<Postit> result = adapter.findAllByUserId(USER_ID, pageQuery);

        // Then
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }
}
