package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

public record DepartmentRequest(
    @NotBlank(message = "部门名称不能为空") String name,
    String leader,
    boolean enabled
) {}
