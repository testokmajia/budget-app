package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

public record IssueCategoryRequest(
    @NotBlank String name,
    int sortOrder,
    boolean enabled
) {}
