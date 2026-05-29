package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record RewardPunishmentRequest(
    @NotBlank String type,
    @NotBlank String title,
    @NotBlank String description,
    @NotEmpty List<String> involvedPersonNames,
    @NotBlank String department,
    @NotNull LocalDate decisionDate,
    String documentNo,
    String attachmentUrl,
    String attachmentFileName,
    Integer score
) {}
