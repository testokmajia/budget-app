package com.techmanage.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record IssueUpdateRequest(
    String title,
    String description,
    String submitterDepartment,
    @NotNull Long occasionId,
    String meetingDepartment,
    LocalDate meetingDate,
    @NotNull String issueType,
    String responsibleTeam,
    Long responsiblePersonId,
    String temporarySolution,
    LocalDate temporaryDeadline,
    String rootCause,
    String permanentSolution,
    LocalDate permanentDeadline,
    String status,
    String system,
    @NotNull Long submitterId
) {}
