package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

public record TeamRequest(
    @NotBlank String name,
    String department,
    String leader,
    String members,
    String systems,
    String systemOwners,
    boolean enabled
) {}
