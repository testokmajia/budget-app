package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

public record TeamRequest(
    @NotBlank(message = "团队名称不能为空") String name,
    String department,
    String leader,
    String members,
    String systems,
    String systemOwners,
    boolean enabled
) {}
