package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

public record IssueCategoryRequest(
    @NotBlank(message = "分类名称不能为空") String name,
    int sortOrder,
    boolean enabled
) {}
