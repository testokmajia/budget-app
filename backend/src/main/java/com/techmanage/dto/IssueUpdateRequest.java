package com.techmanage.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record IssueUpdateRequest(
    String title,
    String description,
    String submitterDepartment,
    @NotNull(message = "提出场合不能为空") Long occasionId,
    String meetingDepartment,
    LocalDate meetingDate,
    @NotNull(message = "问题类型不能为空") String issueType,
    String responsibleTeam,
    Long responsiblePersonId,
    String temporarySolution,
    LocalDate temporaryDeadline,
    String rootCause,
    String permanentSolution,
    LocalDate permanentDeadline,
    String status,
    String system,
    @NotNull(message = "提出人不能为空") Long submitterId
) {}
