package com.techmanage.dto;

import java.util.List;

public record PageResponse<T>(
    List<T> content,
    long totalElements,
    int totalPages,
    int number,
    int size
) {
    public static <T> PageResponse<T> of(List<T> content, long total, int page, int size) {
        return new PageResponse<>(content, total, (int) Math.ceil((double) total / size), page, size);
    }
}
