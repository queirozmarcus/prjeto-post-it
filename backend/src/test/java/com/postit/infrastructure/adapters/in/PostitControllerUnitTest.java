package com.postit.infrastructure.adapters.in;

import com.postit.application.ports.PageQuery;
import com.postit.application.ports.PageResult;
import com.postit.application.ports.PostitServicePort;
import com.postit.application.ports.UserRepositoryPort;
import com.postit.domain.Postit;
import com.postit.domain.PostitObjectMother;
import com.postit.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostitController — lógica de parsing do sort (unit)")
class PostitControllerUnitTest {

    @Mock
    private PostitServicePort service;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private Authentication authentication;

    private PostitController controller;

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "usuario@teste.com";

    private static final User DEFAULT_USER = new User(
            USER_ID, USER_EMAIL, "hash-seguro", "Usuário Teste",
            LocalDateTime.now(), LocalDateTime.now()
    );

    @BeforeEach
    void setUp() {
        controller = new PostitController(service, userRepository);
    }

    // --- helpers para evitar repetição ---

    private void mockAuthenticatedUser() {
        when(authentication.getName()).thenReturn(USER_EMAIL);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(DEFAULT_USER));
    }

    private PageResult<Postit> emptyPage(int page, int size) {
        return new PageResult<>(List.of(), page, size, 0L, 0);
    }

    // --- Testes de parsing do parâmetro sort ---

    @Test
    @DisplayName("Sort padrão 'createdAt,desc' deve gerar PageQuery com campo e direção corretos")
    void shouldUseDefaultSortParams() {
        // Given
        mockAuthenticatedUser();
        when(service.findAllByUser(eq(USER_ID), any(PageQuery.class)))
                .thenReturn(emptyPage(0, 20));

        // When
        controller.findAll(0, 20, "createdAt,desc", authentication);

        // Then
        ArgumentCaptor<PageQuery> captor = ArgumentCaptor.forClass(PageQuery.class);
        verify(service).findAllByUser(eq(USER_ID), captor.capture());
        PageQuery captured = captor.getValue();
        assertThat(captured.sortField()).isEqualTo("createdAt");
        assertThat(captured.sortDirection()).isEqualTo("desc");
    }

    @Test
    @DisplayName("Sort com apenas campo sem direção deve usar 'desc' como direção padrão")
    void shouldDefaultToDescWhenOnlySortFieldProvided() {
        // Given
        mockAuthenticatedUser();
        when(service.findAllByUser(eq(USER_ID), any(PageQuery.class)))
                .thenReturn(emptyPage(0, 20));

        // When — sort sem vírgula: apenas o campo
        controller.findAll(0, 20, "createdAt", authentication);

        // Then
        ArgumentCaptor<PageQuery> captor = ArgumentCaptor.forClass(PageQuery.class);
        verify(service).findAllByUser(eq(USER_ID), captor.capture());
        PageQuery captured = captor.getValue();
        assertThat(captured.sortField()).isEqualTo("createdAt");
        assertThat(captured.sortDirection()).isEqualTo("desc");
    }

    @Test
    @DisplayName("Sort 'updatedAt,asc' deve gerar PageQuery com campo updatedAt e direção asc")
    void shouldParseUpdatedAtAscSort() {
        // Given
        mockAuthenticatedUser();
        when(service.findAllByUser(eq(USER_ID), any(PageQuery.class)))
                .thenReturn(emptyPage(0, 20));

        // When
        controller.findAll(0, 20, "updatedAt,asc", authentication);

        // Then
        ArgumentCaptor<PageQuery> captor = ArgumentCaptor.forClass(PageQuery.class);
        verify(service).findAllByUser(eq(USER_ID), captor.capture());
        PageQuery captured = captor.getValue();
        assertThat(captured.sortField()).isEqualTo("updatedAt");
        assertThat(captured.sortDirection()).isEqualTo("asc");
    }

    @Test
    @DisplayName("Sort 'color,asc' deve gerar PageQuery com campo color e direção asc")
    void shouldParseColorAscSort() {
        // Given
        mockAuthenticatedUser();
        when(service.findAllByUser(eq(USER_ID), any(PageQuery.class)))
                .thenReturn(emptyPage(0, 20));

        // When
        controller.findAll(0, 20, "color,asc", authentication);

        // Then
        ArgumentCaptor<PageQuery> captor = ArgumentCaptor.forClass(PageQuery.class);
        verify(service).findAllByUser(eq(USER_ID), captor.capture());
        PageQuery captured = captor.getValue();
        assertThat(captured.sortField()).isEqualTo("color");
        assertThat(captured.sortDirection()).isEqualTo("asc");
    }

    @ParameterizedTest(name = "sort=''{0}'' deve extrair campo=''{1}'' e direção=''{2}''")
    @CsvSource(delimiter = '|', value = {
        "createdAt,desc | createdAt | desc",
        "updatedAt,asc  | updatedAt | asc",
        "color,desc     | color     | desc",
        "createdAt      | createdAt | desc"
    })
    @DisplayName("Deve parsear combinações de sort corretamente")
    void shouldParseVariousSortCombinations(String sortParam, String expectedField, String expectedDirection) {
        // Given
        mockAuthenticatedUser();
        when(service.findAllByUser(eq(USER_ID), any(PageQuery.class)))
                .thenReturn(emptyPage(0, 20));

        // When
        controller.findAll(0, 20, sortParam.trim(), authentication);

        // Then
        ArgumentCaptor<PageQuery> captor = ArgumentCaptor.forClass(PageQuery.class);
        verify(service).findAllByUser(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().sortField()).isEqualTo(expectedField.trim());
        assertThat(captor.getValue().sortDirection()).isEqualTo(expectedDirection.trim());
    }

    // --- Validação de page e size propagam exceção do PageQuery ---

    @Test
    @DisplayName("Page negativo deve lançar IllegalArgumentException antes de chamar o service")
    void shouldThrowIllegalArgumentException_whenPageIsNegative() {
        // Given
        mockAuthenticatedUser();

        // When / Then
        assertThatThrownBy(() -> controller.findAll(-1, 20, "createdAt,desc", authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page deve ser >= 0");

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Size zero deve lançar IllegalArgumentException antes de chamar o service")
    void shouldThrowIllegalArgumentException_whenSizeIsZero() {
        // Given
        mockAuthenticatedUser();

        // When / Then
        assertThatThrownBy(() -> controller.findAll(0, 0, "createdAt,desc", authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size deve estar entre 1 e 100");

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Size acima do limite (101) deve lançar IllegalArgumentException antes de chamar o service")
    void shouldThrowIllegalArgumentException_whenSizeExceedsLimit() {
        // Given
        mockAuthenticatedUser();

        // When / Then
        assertThatThrownBy(() -> controller.findAll(0, 101, "createdAt,desc", authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size deve estar entre 1 e 100");

        verifyNoInteractions(service);
    }

    // --- Resultado final é convertido corretamente ---

    @Test
    @DisplayName("Deve retornar PagedPostitResponse com os dados da página retornada pelo service")
    void shouldReturnPagedPostitResponseMappedFromService() {
        // Given
        mockAuthenticatedUser();
        Postit postit = PostitObjectMother.validPostit();
        PageResult<Postit> page = new PageResult<>(List.of(postit), 0, 20, 1L, 1);
        when(service.findAllByUser(eq(USER_ID), any(PageQuery.class))).thenReturn(page);

        // When
        PagedPostitResponse response = controller.findAll(0, 20, "createdAt,desc", authentication);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).hasSize(1);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(1L);
        assertThat(response.totalPages()).isEqualTo(1);
    }
}
