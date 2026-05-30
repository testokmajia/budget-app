package com.techmanage.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WeeklyReportResponse(
    Long id,
    Long userId,
    String userName,
    String userDepartment,
    String teamName,
    LocalDate weekStartDate,
    LocalDate weekEndDate,
    String doneWork,
    String planWork,
    String problems,
    String supportNeeded,
    String status,
    String reviewComment,
    LocalDateTime submittedAt,
    LocalDateTime reviewedAt,
    String reviewerName,
    boolean merged,
    Integer version,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
