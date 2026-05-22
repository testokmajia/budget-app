package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

public record DepartmentRequest(
    @NotBlank String name,
    String leader,
    boolean enabled
) {}
