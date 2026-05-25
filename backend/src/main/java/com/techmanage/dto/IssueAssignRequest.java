package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record IssueAssignRequest(
    @NotBlank String responsibleTeam,
    Long responsiblePersonId,
    List<String> systems
) {}
