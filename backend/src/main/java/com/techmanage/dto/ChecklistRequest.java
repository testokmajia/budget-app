package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record ChecklistRequest(
    @NotBlank String source,
    String sourceDetail,
    @NotBlank String description,
    @NotBlank String status,
    String responsiblePerson,
    LocalDate plannedDate,
    LocalDate actualDate,
    String remark
) {}
