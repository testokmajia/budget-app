package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record ChecklistRequest(
    @NotBlank(message = "来源不能为空") String source,
    String sourceDetail,
    @NotBlank(message = "描述不能为空") String description,
    @NotBlank(message = "状态不能为空") String status,
    String responsiblePerson,
    LocalDate plannedDate,
    LocalDate actualDate,
    String remark
) {}
