package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record IssueSolutionRequest(
    @NotBlank String solution,
    @NotNull LocalDate deadline,
    String progress
) {}
