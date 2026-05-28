package com.techmanage.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record WeeklyReportRequest(
    @NotNull LocalDate weekStartDate,
    @NotNull LocalDate weekEndDate,
    String doneWork,
    String planWork,
    String problems,
    String supportNeeded
) {}
