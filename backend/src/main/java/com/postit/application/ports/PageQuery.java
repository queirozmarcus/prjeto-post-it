package com.postit.application.ports;

public record PageQuery(
    int page,
    int size,
    String sortField,
    String sortDirection
) {
    public PageQuery {
        if (page < 0) throw new IllegalArgumentException("page deve ser >= 0");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size deve estar entre 1 e 100");
        if (sortField == null || sortField.isBlank()) sortField = "createdAt";
        if (sortDirection == null || sortDirection.isBlank()) sortDirection = "desc";
    }

    public static PageQuery ofDefaults() {
        return new PageQuery(0, 20, "createdAt", "desc");
    }
}
