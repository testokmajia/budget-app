package com.techmanage.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record IssueRequest(
    @NotBlank(message = "标题不能为空") @Size(max = 20, message = "标题不能超过20字") String title,
    @NotBlank(message = "描述不能为空") @Size(max = 1000, message = "描述不能超过1000字") String description,
    @NotBlank(message = "提出部门不能为空") String submitterDepartment,
    @NotNull(message = "提出人不能为空") Long submitterId,
    Long occasionId,
    String meetingDepartment,
    LocalDate meetingDate,
    String issueType,
    String rootCause,
    @Size(max = 1000, message = "永久解决方案不能超过1000字") String permanentSolution,
    LocalDate permanentDeadline
) {}
