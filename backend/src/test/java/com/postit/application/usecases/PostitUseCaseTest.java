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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
}
