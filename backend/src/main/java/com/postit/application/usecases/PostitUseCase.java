package com.postit.application.usecases;

import com.postit.application.ports.PostitRepositoryPort;
import com.postit.application.ports.PostitServicePort;
import com.postit.application.ports.UserRepositoryPort;
import com.postit.domain.Postit;
import com.postit.shared.exception.PostitAccessDeniedException;
import com.postit.shared.exception.PostitNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class PostitUseCase implements PostitServicePort {

    private final PostitRepositoryPort repository;
    private final UserRepositoryPort userRepository;

    public PostitUseCase(PostitRepositoryPort repository, UserRepositoryPort userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    // --- Operações com ownership (Sprint 5) ---

    @Override
    public Postit create(Postit postit, Long userId) {
        return repository.save(
            Postit.create(postit.content(), postit.color(), userId)
        );
    }

    @Override
    public List<Postit> findAllByUser(Long userId) {
        return repository.findAllByUserId(userId);
    }

    @Override
    public Optional<Postit> findById(Long id, Long userId) {
        return repository.findById(id)
            .filter(p -> userId.equals(p.userId()));
    }

    @Override
    public Postit update(Long id, Postit postit, Long userId) {
        Postit existing = repository.findById(id)
            .orElseThrow(() -> new PostitNotFoundException(id));
        if (!userId.equals(existing.userId())) {
            throw new PostitAccessDeniedException(id);
        }
        // Preserva userId e createdAt do registro original
        Postit toSave = new Postit(
            id,
            postit.content(),
            postit.color(),
            existing.userId(),
            existing.createdAt(),
            LocalDateTime.now()
        );
        return repository.save(toSave);
    }

    @Override
    public void delete(Long id, Long userId) {
        Postit existing = repository.findById(id)
            .orElseThrow(() -> new PostitNotFoundException(id));
        if (!userId.equals(existing.userId())) {
            throw new PostitAccessDeniedException(id);
        }
        repository.deleteById(id);
    }

    // --- Operações legadas sem ownership (mantidas para compatibilidade) ---

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
