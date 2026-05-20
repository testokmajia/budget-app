package com.techmanage.dto;

import jakarta.validation.constraints.NotNull;

public record IssueAssignRequest(
    String category,
    @NotNull Long assigneeId
) {}
