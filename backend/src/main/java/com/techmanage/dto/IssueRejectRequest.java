package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

public record IssueRejectRequest(
    @NotBlank String reason
) {}
