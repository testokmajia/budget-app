package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

public record SystemInfoRequest(
    @NotBlank String name,
    String leader,
    String team,
    boolean enabled
) {}
