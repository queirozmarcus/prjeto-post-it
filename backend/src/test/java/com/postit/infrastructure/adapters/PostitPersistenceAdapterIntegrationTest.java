package com.postit.infrastructure.adapters;

import com.postit.application.ports.PostitRepositoryPort;
import com.postit.application.ports.UserRepositoryPort;
import com.postit.domain.Postit;
import com.postit.domain.PostitObjectMother;
import com.postit.domain.user.User;
import com.postit.infrastructure.containers.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PostitPersistenceAdapterIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PostitRepositoryPort repository;

    @Autowired
    private UserRepositoryPort userRepository;

    @Test
    @DisplayName("Deve persistir e recuperar um post-it do banco real (Testcontainers)")
    void shouldPersistAndRetrievePostit() {
        // Given: criar usuário válido primeiro (user_id é NOT NULL desde V4)
        User user = User.create("test@example.com", "hashedPassword123", "Test User");
        User savedUser = userRepository.save(user);

        // When: criar postit associado ao usuário
        Postit postit = PostitObjectMother.postitToCreateWithUserId(savedUser.id());

        Postit saved = repository.save(postit);

        // Then: postit deve ser persistido com ID e recuperado corretamente
        assertThat(saved.id()).isNotNull();

        List<Postit> all = repository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).content()).isEqualTo("Comprar café");
        assertThat(all.get(0).userId()).isEqualTo(savedUser.id());
    }
}
