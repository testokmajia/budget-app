package com.techmanage.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ChangeProposalResponse(
    Long id,
    Long issueId,
    String issueCode,
    String issueTitle,
    Long proposerId,
    String proposerName,
    String oldTemporarySolution,
    String newTemporarySolution,
    LocalDate oldTemporaryDeadline,
    LocalDate newTemporaryDeadline,
    String oldRootCause,
    String newRootCause,
    String oldPermanentSolution,
    String newPermanentSolution,
    LocalDate oldPermanentDeadline,
    LocalDate newPermanentDeadline,
    String status,
    Long reviewedByTeamLeaderId,
    String teamLeaderReviewerName,
    String teamLeaderReviewComment,
    LocalDateTime teamLeaderReviewedAt,
    Long reviewedByAdminId,
    String adminReviewerName,
    String adminReviewComment,
    LocalDateTime adminReviewedAt,
    LocalDateTime createdAt
) {}
