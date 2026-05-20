package com.techmanage.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ChecklistResponse(
    Long id,
    String source,
    String sourceDetail,
    String description,
    String status,
    String responsiblePerson,
    LocalDate plannedDate,
    LocalDate actualDate,
    String remark,
    String creatorName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
