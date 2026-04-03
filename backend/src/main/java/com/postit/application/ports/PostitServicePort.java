package com.postit.application.ports;

import com.postit.domain.Postit;
import java.util.List;
import java.util.Optional;

public interface PostitServicePort {

    // --- Operações com ownership (Sprint 5) ---

    Postit create(Postit postit, Long userId);

    List<Postit> findAllByUser(Long userId);
    PageResult<Postit> findAllByUser(Long userId, PageQuery pageQuery);

    Optional<Postit> findById(Long id, Long userId);

    Postit update(Long id, Postit postit, Long userId);

    void delete(Long id, Long userId);

    // --- Operações legadas sem ownership (mantidas para compatibilidade) ---

    Postit create(Postit postit);

    List<Postit> findAll();

    Optional<Postit> findById(Long id);

    Postit update(Long id, Postit postit);

    void delete(Long id);
}
