package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

public record IssueOccasionRequest(
    @NotBlank String name,
    @NotBlank String type,
    boolean enabled
) {}
