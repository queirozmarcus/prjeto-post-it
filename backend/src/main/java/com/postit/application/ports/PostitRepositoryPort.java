package com.postit.application.ports;

import com.postit.domain.Postit;
import java.util.List;
import java.util.Optional;

public interface PostitRepositoryPort {
    Postit save(Postit postit);
    List<Postit> findAll();
    Optional<Postit> findById(Long id);
    void deleteById(Long id);
    List<Postit> findAllByUserId(Long userId);
}
