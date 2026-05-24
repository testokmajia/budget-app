package com.techmanage.service;

import com.techmanage.dto.*;
import java.time.LocalDate;
import java.util.List;

public interface IssueFeedbackService {
    PageResponse<IssueResponse> list(int page, int size, String sortBy, String sortDir,
        List<String> statuses, List<Long> submitterIds, String submitterDepartment,
        Long occasionId, String issueType, String responsibleTeam,
        Long responsiblePersonId, LocalDate dateFrom, LocalDate dateTo,
        Long currentUserId, boolean isAdmin, boolean isIssueAdmin,
        boolean isItEmployee, boolean myScope, List<Long> teamMemberIds);

    IssueResponse getById(Long id);
    IssueResponse create(Long userId, IssueRequest request);

    // 管理员分派: 待分派 → 待员工处理
    IssueResponse assign(Long id, Long userId, IssueAssignRequest request);

    // 员工填写方案: 待员工处理 → 待组长审核
    IssueResponse submitSolution(Long id, Long userId, IssueSolutionRequest request);

    // 团队负责人审核: 待组长审核 → 待管理员审核 (通过) or 待员工处理 (退回)
    IssueResponse reviewByLeader(Long id, Long userId, IssueReviewRequest request);

    // 管理员审核: 待管理员审核 → 待确认 (通过) or 待员工处理 (退回)
    IssueResponse reviewByAdmin(Long id, Long userId, IssueReviewRequest request);

    // 提出人确认: 待确认 → 已完成 (确认) or 待员工处理 (退回)
    IssueResponse confirm(Long id, Long userId, IssueConfirmRequest request);

    IssueResponse reject(Long id, Long userId, IssueRejectRequest request);
    IssueResponse close(Long id, Long userId, IssueCloseRequest request);
    IssueResponse update(Long id, Long userId, IssueUpdateRequest request);
    IssueResponse undo(Long id, Long userId);
}
