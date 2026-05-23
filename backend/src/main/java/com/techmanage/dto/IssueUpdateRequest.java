package com.techmanage.dto;

import java.time.LocalDate;

public record IssueUpdateRequest(
    String title,
    String description,
    String submitterDepartment,
    Long occasionId,
    String meetingDepartment,
    LocalDate meetingDate,
    String issueType,
    String responsibleTeam,
    Long responsiblePersonId,
    String temporarySolution,
    LocalDate temporaryDeadline,
    String rootCause,
    String permanentSolution,
    LocalDate permanentDeadline,
    String status
) {}
