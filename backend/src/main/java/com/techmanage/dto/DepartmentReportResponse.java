package com.techmanage.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DepartmentReportResponse(
    Long id,
    LocalDate weekStartDate,
    LocalDate weekEndDate,
    String department,
    String mergedContent,
    String editedContent,
    String status,
    String sourceReportIds,
    LocalDateTime submittedAt,
    LocalDateTime finalizedAt,
    String finalizedByName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
