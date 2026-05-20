package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RewardPunishmentRequest(
    @NotBlank String type,
    @NotBlank String title,
    @NotBlank String description,
    @NotBlank String involvedPerson,
    @NotBlank String department,
    @NotNull LocalDate decisionDate,
    String documentNo,
    String attachmentUrl
) {}
