package com.techmanage.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RewardPunishmentResponse(
    Long id,
    String type,
    String title,
    String description,
    String involvedPerson,
    String department,
    LocalDate decisionDate,
    String documentNo,
    String attachmentUrl,
    String creatorName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Integer score
) {}
