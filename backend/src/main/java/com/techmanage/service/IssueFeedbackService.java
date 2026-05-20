package com.techmanage.service;

import com.techmanage.dto.*;

import java.util.List;

public interface IssueFeedbackService {
    List<IssueResponse> list(Long userId, String department, boolean isAdmin, boolean isIssueAdmin, String status);
    IssueResponse getById(Long id);
    IssueResponse create(Long userId, IssueRequest request);
    IssueResponse assign(Long id, Long userId, IssueAssignRequest request);
    IssueResponse submitSolution(Long id, Long userId, IssueSolutionRequest request);
    IssueResponse complete(Long id, Long userId);
    IssueResponse confirm(Long id, Long userId, IssueConfirmRequest request);
    IssueResponse reject(Long id, Long userId, IssueRejectRequest request);
}
