package com.techmanage.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record DepartmentReportRequest(
    LocalDate weekStartDate,
    LocalDate weekEndDate
) {}
