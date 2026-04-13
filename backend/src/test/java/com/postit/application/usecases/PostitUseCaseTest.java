package com.postit.application.usecases;

import com.postit.application.ports.PostitRepositoryPort;
import com.postit.application.ports.UserRepositoryPort;
import com.postit.domain.Postit;
import com.postit.domain.PostitObjectMother;
import com.postit.shared.exception.PostitAccessDeniedException;
import com.postit.shared.exception.PostitNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.postit.application.ports.PageQuery;
import com.postit.application.ports.PageResult;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostitUseCaseTest {

    @Mock
    private PostitRepositoryPort repository;

    @Mock
    private UserRepositoryPort userRepository;

    @InjectMocks
    private PostitUseCase useCase;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;

    // --- Testes legados (assinaturas sem userId) ---

    @Test
    @DisplayName("Deve criar um post-it com sucesso (legado sem userId)")
    void shouldCreatePostitSuccessfully() {
        Postit postit = PostitObjectMother.postitToCreate();
        when(repository.save(any(Postit.class))).thenReturn(PostitObjectMother.validPostit());

        Postit result = useCase.create(postit);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        verify(repository, times(1)).save(any(Postit.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar post-it inexistente (legado sem userId)")
    void shouldThrowExceptionWhenUpdatingNonExistentPostit() {
        Long invalidId = 999L;
        Postit postit = PostitObjectMother.validPostit();
        when(repository.findById(invalidId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.update(invalidId, postit))
                .isInstanceOf(PostitNotFoundException.class)
                .hasMessageContaining("ID: 999");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao excluir post-it inexistente (legado sem userId)")
    void shouldThrowExceptionWhenDeletingNonExistentPostit() {
        Long invalidId = 999L;
        when(repository.findById(invalidId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.delete(invalidId))
                .isInstanceOf(PostitNotFoundException.class);

        verify(repository, never()).deleteById(any());
    }

    // --- Testes com ownership (Sprint 5) ---

    @Test
    @DisplayName("Deve criar um post-it associado ao userId")
    void shouldCreatePostitWithUserId() {
        Postit input = PostitObjectMother.postitToCreateWithUserId(USER_ID);
        Postit saved = PostitObjectMother.validPostit(); // userId = 1L
        when(repository.save(any(Postit.class))).thenReturn(saved);

        Postit result = useCase.create(input, USER_ID);

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(USER_ID);
        verify(repository, times(1)).save(any(Postit.class));
    }

    @Test
    @DisplayName("Deve listar apenas postits do usuário autenticado")
    void shouldListOnlyPostitsOfAuthenticatedUser() {
        List<Postit> userPostits = List.of(PostitObjectMother.validPostit());
        when(repository.findAllByUserId(USER_ID)).thenReturn(userPostits);

        List<Postit> result = useCase.findAllByUser(USER_ID);

        assertThat(result).hasSize(1);
        verify(repository, times(1)).findAllByUserId(USER_ID);
    }

    @Test
    @DisplayName("findById deve retornar vazio quando postit pertence a outro usuário")
    void shouldReturnEmptyWhenPostitBelongsToAnotherUser() {
        Postit existing = PostitObjectMother.validPostit(); // userId = 1L
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        Optional<Postit> result = useCase.findById(1L, OTHER_USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findById deve retornar o postit quando ownership é válido")
    void shouldReturnPostitWhenOwnershipIsValid() {
        Postit existing = PostitObjectMother.validPostit(); // userId = 1L
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        Optional<Postit> result = useCase.findById(1L, USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().userId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("Deve negar acesso ao atualizar postit de outro usuário")
    void shouldDenyAccessWhenUserDoesNotOwnPostit_update() {
        Postit existing = PostitObjectMother.validPostit(); // userId = 1L
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> useCase.update(1L, existing, OTHER_USER_ID))
                .isInstanceOf(PostitAccessDeniedException.class)
                .hasMessageContaining("ID: 1");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve negar acesso ao excluir postit de outro usuário")
    void shouldDenyAccessWhenUserDoesNotOwnPostit_delete() {
        Postit existing = PostitObjectMother.validPostit(); // userId = 1L
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> useCase.delete(1L, OTHER_USER_ID))
                .isInstanceOf(PostitAccessDeniedException.class)
                .hasMessageContaining("ID: 1");

        verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("update deve lançar PostitNotFoundException quando postit não existe")
    void shouldThrowNotFoundWhenUpdatingWithOwnership_nonExistent() {
        Long invalidId = 999L;
        Postit postit = PostitObjectMother.validPostit();
        when(repository.findById(invalidId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.update(invalidId, postit, USER_ID))
                .isInstanceOf(PostitNotFoundException.class)
                .hasMessageContaining("ID: 999");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("delete deve lançar PostitNotFoundException quando postit não existe")
    void shouldThrowNotFoundWhenDeletingWithOwnership_nonExistent() {
        Long invalidId = 999L;
        when(repository.findById(invalidId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.delete(invalidId, USER_ID))
                .isInstanceOf(PostitNotFoundException.class);

        verify(repository, never()).deleteById(any());
    }

    // --- Testes de paginação (Sprint 6) ---

    @Test
    @DisplayName("Deve retornar página de postits do usuário autenticado")
    void shouldReturnPaginatedPostitsForUser() {
        // Given
        PageQuery pageQuery = PageQuery.ofDefaults();
        List<Postit> items = List.of(PostitObjectMother.validPostit());
        PageResult<Postit> expectedPage = new PageResult<>(items, 0, 20, 1L, 1);
        when(repository.findAllByUserId(USER_ID, pageQuery)).thenReturn(expectedPage);

        // When
        PageResult<Postit> result = useCase.findAllByUser(USER_ID, pageQuery);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(1L);
        assertThat(result.totalPages()).isEqualTo(1);
        verify(repository, times(1)).findAllByUserId(USER_ID, pageQuery);
    }

    @Test
    @DisplayName("Deve repassar o PageQuery exato ao repositório sem modificação")
    void shouldPassPageQueryToRepository() {
        // Given
        PageQuery pageQuery = new PageQuery(2, 5, "content", "asc");
        PageResult<Postit> emptyPage = new PageResult<>(List.of(), 2, 5, 0L, 0);
        when(repository.findAllByUserId(eq(USER_ID), eq(pageQuery))).thenReturn(emptyPage);

        // When
        useCase.findAllByUser(USER_ID, pageQuery);

        // Then
        verify(repository).findAllByUserId(eq(USER_ID), eq(pageQuery));
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("Deve rejeitar post-it com conteúdo maior que 120 caracteres")
    void shouldRejectPostitWithContentExceeding120Chars() {
        // Given: content com 121 caracteres
        String longContent = "a".repeat(121);

        // When/Then: validação falha no construtor do domain record
        assertThatThrownBy(() -> Postit.create(longContent, "#FFFFFF"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("120 caracteres");
    }

    @Test
    @DisplayName("Deve aceitar post-it com exatamente 120 caracteres")
    void shouldAcceptPostitWithExactly120Chars() {
        // Given: content com 120 caracteres
        String maxContent = "a".repeat(120);
        Postit postit = Postit.create(maxContent, "#FFFFFF");
        when(repository.save(any(Postit.class))).thenReturn(postit.withId(1L));

        // When
        Postit result = useCase.create(postit);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(120);
        verify(repository, times(1)).save(any(Postit.class));
    }
}
