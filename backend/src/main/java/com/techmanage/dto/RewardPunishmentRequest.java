package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record RewardPunishmentRequest(
    @NotBlank(message = "奖惩类型不能为空") String type,
    @NotBlank(message = "标题不能为空") String title,
    @NotBlank(message = "描述不能为空") String description,
    @NotEmpty(message = "涉及人员不能为空") List<String> involvedPersonNames,
    @NotBlank(message = "部门不能为空") String department,
    @NotNull(message = "决定日期不能为空") LocalDate decisionDate,
    String documentNo,
    String attachmentUrl,
    String attachmentFileName,
    Integer score
) {}
