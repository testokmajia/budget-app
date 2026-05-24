package com.techmanage.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record IssueResponse(
    Long id,
    String issueCode,
    String title,
    String description,
    String submitterDepartment,
    Long submitterId,
    String submitterName,
    Long occasionId,
    String occasionName,
    String occasionType,
    String meetingDepartment,
    LocalDate meetingDate,
    String issueType,
    String responsibleTeam,
    Long responsiblePersonId,
    String responsiblePersonName,
    String temporarySolution,
    LocalDate temporaryDeadline,
    String rootCause,
    String permanentSolution,
    LocalDate permanentDeadline,
    String status,
    Long lastOperatorId,
    String system,
    List<IssueLogInfo> logs,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public record IssueLogInfo(
        Long id,
        String userName,
        String action,
        String remark,
        LocalDateTime createdAt
    ) {}
}
