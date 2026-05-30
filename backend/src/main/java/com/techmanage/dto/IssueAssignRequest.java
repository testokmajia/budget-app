package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record IssueAssignRequest(
    @NotBlank(message = "责任团队不能为空") String responsibleTeam,
    Long responsiblePersonId,
    List<String> systems
) {}
