package com.techmanage.dto;

import java.time.LocalDate;

public record ChangeProposalRequest(
    String temporarySolution,
    LocalDate temporaryDeadline,
    String rootCause,
    String permanentSolution,
    LocalDate permanentDeadline
) {}
