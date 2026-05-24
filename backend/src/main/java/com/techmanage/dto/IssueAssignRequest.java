package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

public record IssueAssignRequest(
    @NotBlank String responsibleTeam,
    Long responsiblePersonId,
    String system
) {}
