package com.techmanage.service;

import com.techmanage.dto.ChangeProposalRequest;
import com.techmanage.dto.ChangeProposalResponse;
import com.techmanage.dto.ChangeReviewRequest;

import java.util.List;

public interface IssueChangeProposalService {
    ChangeProposalResponse submit(Long issueId, Long userId, ChangeProposalRequest request,
                                  boolean isTeamLeader, List<Long> ledTeamIds);
    List<ChangeProposalResponse> listPending(Long userId, boolean isAdmin, boolean isIssueAdmin,
                                             boolean isTeamLeader, List<Long> ledTeamIds);
    ChangeProposalResponse review(Long id, Long reviewerId, ChangeReviewRequest request,
                                  boolean isAdmin, boolean isIssueAdmin);
    List<ChangeProposalResponse> getByIssueId(Long issueId);
}
