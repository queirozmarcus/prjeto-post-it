package com.postit.infrastructure.adapters.in;

import com.postit.application.ports.PageResult;
import com.postit.domain.Postit;
import java.util.List;

public record PagedPostitResponse(
    List<PostitResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static PagedPostitResponse fromPageResult(PageResult<Postit> result) {
        List<PostitResponse> items = result.content().stream()
                .map(PostitResponse::fromDomain)
                .toList();
        return new PagedPostitResponse(
            items,
            result.page(),
            result.size(),
            result.totalElements(),
            result.totalPages()
        );
    }
}
