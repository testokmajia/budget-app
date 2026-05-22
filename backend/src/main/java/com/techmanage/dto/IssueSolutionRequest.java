package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record IssueSolutionRequest(
    @NotBlank String temporarySolution,
    @NotNull LocalDate temporaryDeadline,
    String rootCause,
    @Size(max = 1000) String permanentSolution,
    LocalDate permanentDeadline
) {}
