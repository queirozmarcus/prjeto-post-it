package com.postit.infrastructure.adapters.in;

import com.postit.domain.Postit;
import java.time.LocalDateTime;

public record PostitResponse(
    Long id,
    String content,
    String color,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static PostitResponse fromDomain(Postit domain) {
        return new PostitResponse(
            domain.id(),
            domain.content(),
            domain.color(),
            domain.createdAt(),
            domain.updatedAt()
        );
    }
}
