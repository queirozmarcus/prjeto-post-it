package com.postit.infrastructure.adapters.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostitJpaRepository extends JpaRepository<PostitEntity, Long> {
    List<PostitEntity> findByUserId(Long userId);
}
