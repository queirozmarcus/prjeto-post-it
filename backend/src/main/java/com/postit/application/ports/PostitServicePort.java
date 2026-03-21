package com.postit.application.ports;

import com.postit.domain.Postit;
import java.util.List;
import java.util.Optional;

public interface PostitServicePort {
    Postit create(Postit postit);
    List<Postit> findAll();
    Optional<Postit> findById(Long id);
    Postit update(Long id, Postit postit);
    void delete(Long id);
}
