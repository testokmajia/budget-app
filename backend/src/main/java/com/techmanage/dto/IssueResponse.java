package com.techmanage.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record IssueResponse(
    Long id,
    String title,
    String description,
    String department,
    String category,
    String urgency,
    String system,
    Long submitterId,
    String submitterName,
    Long assigneeId,
    String assigneeName,
    String solution,
    LocalDate deadline,
    String progress,
    LocalDateTime actualCompletionTime,
    String status,
    String rejectReason,
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
