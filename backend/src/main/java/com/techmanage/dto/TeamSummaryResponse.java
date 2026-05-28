package com.techmanage.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TeamSummaryResponse(
    Long id,
    String teamName,
    Long leaderId,
    String leaderName,
    LocalDate weekStartDate,
    LocalDate weekEndDate,
    String mergedContent,
    String editedContent,
    String status,
    String sourceReportIds,
    LocalDateTime submittedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
