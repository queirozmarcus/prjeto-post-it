package com.postit.infrastructure.adapters;

import com.postit.application.ports.PostitRepositoryPort;
import com.postit.domain.Postit;
import com.postit.domain.PostitObjectMother;
import com.postit.infrastructure.containers.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PostitPersistenceAdapterIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PostitRepositoryPort repository;

    @Test
    @DisplayName("Deve persistir e recuperar um post-it do banco real (Testcontainers)")
    void shouldPersistAndRetrievePostit() {
        Postit postit = PostitObjectMother.postitToCreate();
        
        Postit saved = repository.save(postit);
        assertThat(saved.id()).isNotNull();
        
        List<Postit> all = repository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).content()).isEqualTo("Comprar café");
    }
}
