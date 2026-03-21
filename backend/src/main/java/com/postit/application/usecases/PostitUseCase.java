package com.postit.application.usecases;

import com.postit.application.ports.PostitRepositoryPort;
import com.postit.application.ports.PostitServicePort;
import com.postit.domain.Postit;
import com.postit.shared.exception.PostitNotFoundException;
import java.util.List;
import java.util.Optional;

public class PostitUseCase implements PostitServicePort {

    private final PostitRepositoryPort repository;

    public PostitUseCase(PostitRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public Postit create(Postit postit) {
        return repository.save(postit);
    }

    @Override
    public List<Postit> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Postit> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Postit update(Long id, Postit postit) {
        if (repository.findById(id).isEmpty()) {
            throw new PostitNotFoundException(id);
        }
        return repository.save(postit.withId(id));
    }

    @Override
    public void delete(Long id) {
        if (repository.findById(id).isEmpty()) {
            throw new PostitNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
