package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record IssueSolutionRequest(
    String temporarySolution,
    LocalDate temporaryDeadline,
    @NotBlank String rootCause,
    @NotBlank @Size(max = 1000) String permanentSolution,
    @NotNull LocalDate permanentDeadline
) {}
