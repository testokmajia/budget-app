package com.techmanage.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record TeamSummaryRequest(
    @NotNull LocalDate weekStartDate,
    @NotNull LocalDate weekEndDate,
    String teamName
) {}
