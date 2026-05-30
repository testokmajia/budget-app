package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

public record IssueOccasionRequest(
    @NotBlank(message = "场合名称不能为空") String name,
    @NotBlank(message = "类型不能为空") String type,
    boolean enabled
) {}
