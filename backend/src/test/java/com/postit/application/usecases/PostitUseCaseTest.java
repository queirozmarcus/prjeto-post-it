package com.postit.application.usecases;

import com.postit.application.ports.PostitRepositoryPort;
import com.postit.domain.Postit;
import com.postit.domain.PostitObjectMother;
import com.postit.shared.exception.PostitNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostitUseCaseTest {

    @Mock
    private PostitRepositoryPort repository;

    @InjectMocks
    private PostitUseCase useCase;

    @Test
    @DisplayName("Deve criar um post-it com sucesso")
    void shouldCreatePostitSuccessfully() {
        Postit postit = PostitObjectMother.postitToCreate();
        when(repository.save(any(Postit.class))).thenReturn(PostitObjectMother.validPostit());

        Postit result = useCase.create(postit);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        verify(repository, times(1)).save(any(Postit.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar post-it inexistente")
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
    @DisplayName("Deve lançar exceção ao excluir post-it inexistente")
    void shouldThrowExceptionWhenDeletingNonExistentPostit() {
        Long invalidId = 999L;
        when(repository.findById(invalidId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.delete(invalidId))
                .isInstanceOf(PostitNotFoundException.class);
        
        verify(repository, never()).deleteById(any());
    }
}
