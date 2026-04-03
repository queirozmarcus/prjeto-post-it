package com.postit.infrastructure.adapters.out;

import com.postit.application.ports.PageQuery;
import com.postit.application.ports.PageResult;
import com.postit.application.ports.PostitRepositoryPort;
import com.postit.domain.Postit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PostitPersistenceAdapter implements PostitRepositoryPort {

    private final PostitJpaRepository repository;

    public PostitPersistenceAdapter(PostitJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Postit save(Postit postit) {
        PostitEntity entity = toEntity(postit);
        PostitEntity savedEntity = repository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public List<Postit> findAll() {
        return repository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Postit> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public List<Postit> findAllByUserId(Long userId) {
        return repository.findByUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<Postit> findAllByUserId(Long userId, PageQuery pageQuery) {
        Sort sort = Sort.by(
            Sort.Direction.fromString(pageQuery.sortDirection()),
            mapSortField(pageQuery.sortField())
        );
        PageRequest pageable = PageRequest.of(pageQuery.page(), pageQuery.size(), sort);
        Page<PostitEntity> page = repository.findByUserId(userId, pageable);

        List<Postit> content = page.getContent().stream()
                .map(this::toDomain)
                .toList();

        return new PageResult<>(
            content,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    private String mapSortField(String field) {
        return switch (field) {
            case "createdAt" -> "createdAt";
            case "updatedAt" -> "updatedAt";
            case "color" -> "color";
            default -> "createdAt";
        };
    }

    private PostitEntity toEntity(Postit domain) {
        return new PostitEntity(
                domain.id(),
                domain.content(),
                domain.color(),
                domain.userId(),
                domain.createdAt(),
                domain.updatedAt()
        );
    }

    private Postit toDomain(PostitEntity entity) {
        return new Postit(
                entity.getId(),
                entity.getContent(),
                entity.getColor(),
                entity.getUserId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
