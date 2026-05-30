package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record IssueSolutionRequest(
    String temporarySolution,
    LocalDate temporaryDeadline,
    @NotBlank(message = "产生原因不能为空") String rootCause,
    @NotBlank(message = "永久解决方案不能为空") @Size(max = 1000, message = "永久解决方案不能超过1000字") String permanentSolution,
    @NotNull(message = "永久解决时限不能为空") LocalDate permanentDeadline
) {}
