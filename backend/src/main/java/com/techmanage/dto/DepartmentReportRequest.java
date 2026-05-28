package com.techmanage.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record DepartmentReportRequest(
    @NotNull LocalDate weekStartDate,
    @NotNull LocalDate weekEndDate
) {}
