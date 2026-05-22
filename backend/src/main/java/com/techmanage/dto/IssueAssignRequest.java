package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IssueAssignRequest(
    @NotBlank String responsibleTeam,
    @NotNull Long responsiblePersonId
) {}
