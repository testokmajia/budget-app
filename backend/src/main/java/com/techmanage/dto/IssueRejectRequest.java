package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

public record IssueRejectRequest(
    @NotBlank(message = "驳回原因不能为空") String reason
) {}
