package com.techmanage.service;

import com.techmanage.dto.*;
import java.time.LocalDate;
import java.util.List;

public interface IssueFeedbackService {
    PageResponse<IssueResponse> list(int page, int size, String sortBy, String sortDir,
        List<String> statuses, List<Long> submitterIds, String submitterDepartment,
        Long occasionId, String issueType, String responsibleTeam,
        Long responsiblePersonId, LocalDate dateFrom, LocalDate dateTo,
        Long currentUserId, boolean isAdmin, boolean isIssueAdmin);

    IssueResponse getById(Long id);
    IssueResponse create(Long userId, IssueRequest request);
    IssueResponse assign(Long id, Long userId, IssueAssignRequest request);
    IssueResponse submitSolution(Long id, Long userId, IssueSolutionRequest request);
    IssueResponse complete(Long id, Long userId);
    IssueResponse confirm(Long id, Long userId, IssueConfirmRequest request);
    IssueResponse reject(Long id, Long userId, IssueRejectRequest request);
    IssueResponse close(Long id, Long userId, IssueCloseRequest request);
    IssueResponse update(Long id, Long userId, IssueUpdateRequest request);
}
