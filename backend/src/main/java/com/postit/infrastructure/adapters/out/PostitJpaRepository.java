package com.postit.infrastructure.adapters.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostitJpaRepository extends JpaRepository<PostitEntity, Long> {
}
