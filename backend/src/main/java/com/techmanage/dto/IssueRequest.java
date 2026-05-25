package com.techmanage.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record IssueRequest(
    @NotBlank @Size(max = 20) String title,
    @NotBlank @Size(max = 1000) String description,
    @NotBlank String submitterDepartment,
    @NotNull Long submitterId,
    Long occasionId,
    String meetingDepartment,
    LocalDate meetingDate,
    String issueType,
    String rootCause,
    @Size(max = 1000) String permanentSolution,
    LocalDate permanentDeadline
) {}
