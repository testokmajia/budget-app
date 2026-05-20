package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

public record IssueRequest(
    @NotBlank String title,
    @NotBlank String description,
    @NotBlank String department,
    @NotBlank String urgency,
    String category,
    String system
) {}
