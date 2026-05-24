package com.techmanage.service.impl;

import com.techmanage.dto.ChangeProposalRequest;
import com.techmanage.dto.ChangeProposalResponse;
import com.techmanage.dto.ChangeReviewRequest;
import com.techmanage.entity.IssueChangeProposal;
import com.techmanage.entity.IssueFeedback;
import com.techmanage.repository.IssueChangeProposalRepository;
import com.techmanage.repository.IssueFeedbackRepository;
import com.techmanage.repository.UserRepository;
import com.techmanage.service.IssueChangeProposalService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class IssueChangeProposalServiceImpl implements IssueChangeProposalService {

    private final IssueChangeProposalRepository proposalRepository;
    private final IssueFeedbackRepository issueRepository;
    private final UserRepository userRepository;

    public IssueChangeProposalServiceImpl(IssueChangeProposalRepository proposalRepository,
                                           IssueFeedbackRepository issueRepository,
                                           UserRepository userRepository) {
        this.proposalRepository = proposalRepository;
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ChangeProposalResponse submit(Long issueId, Long userId, ChangeProposalRequest request,
                                          boolean isTeamLeader, List<Long> ledTeamIds) {
        IssueFeedback issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("问题不存在"));

        boolean isResponsible = userId.equals(issue.getResponsiblePersonId());
        boolean isLeaderOfTeam = isTeamLeader && ledTeamIds.contains(issue.getResponsiblePersonId());

        if (!isResponsible && !isLeaderOfTeam) {
            throw new RuntimeException("无权修改该问题的方案");
        }

        IssueChangeProposal proposal = new IssueChangeProposal();
        proposal.setIssueId(issueId);
        proposal.setProposerId(userId);
        proposal.setOldTemporarySolution(issue.getTemporarySolution());
        proposal.setNewTemporarySolution(request.temporarySolution());
        proposal.setOldTemporaryDeadline(issue.getTemporaryDeadline());
        proposal.setNewTemporaryDeadline(request.temporaryDeadline());
        proposal.setOldRootCause(issue.getRootCause());
        proposal.setNewRootCause(request.rootCause());
        proposal.setOldPermanentSolution(issue.getPermanentSolution());
        proposal.setNewPermanentSolution(request.permanentSolution());
        proposal.setOldPermanentDeadline(issue.getPermanentDeadline());
        proposal.setNewPermanentDeadline(request.permanentDeadline());

        proposal.setStatus(isTeamLeader ? "PENDING_ADMIN" : "PENDING_TEAM_LEADER");
        proposalRepository.save(proposal);
        return toResponse(proposal);
    }

    @Override
    public List<ChangeProposalResponse> listPending(Long userId, boolean isAdmin, boolean isIssueAdmin,
                                                     boolean isTeamLeader, List<Long> ledTeamIds) {
        List<IssueChangeProposal> all = proposalRepository.findByStatusOrderByCreatedAtDesc("PENDING_TEAM_LEADER");
        all.addAll(proposalRepository.findByStatusOrderByCreatedAtDesc("PENDING_ADMIN"));

        return all.stream()
                .filter(p -> {
                    if (isAdmin || isIssueAdmin) {
                        return "PENDING_ADMIN".equals(p.getStatus());
                    }
                    if (isTeamLeader) {
                        if ("PENDING_TEAM_LEADER".equals(p.getStatus())) {
                            IssueFeedback issue = issueRepository.findById(p.getIssueId()).orElse(null);
                            return issue != null && ledTeamIds.contains(issue.getResponsiblePersonId());
                        }
                    }
                    return false;
                })
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ChangeProposalResponse review(Long id, Long reviewerId, ChangeReviewRequest request,
                                          boolean isAdmin, boolean isIssueAdmin) {
        IssueChangeProposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("变更申请不存在"));

        if ("PENDING_TEAM_LEADER".equals(proposal.getStatus())) {
            if (request.approved()) {
                proposal.setReviewedByTeamLeaderId(reviewerId);
                proposal.setTeamLeaderReviewComment(request.comment());
                proposal.setTeamLeaderReviewedAt(LocalDateTime.now());
                proposal.setStatus("PENDING_ADMIN");
            } else {
                proposal.setReviewedByTeamLeaderId(reviewerId);
                proposal.setTeamLeaderReviewComment(request.comment());
                proposal.setTeamLeaderReviewedAt(LocalDateTime.now());
                proposal.setStatus("REJECTED");
            }
        } else if ("PENDING_ADMIN".equals(proposal.getStatus())) {
            if (!isAdmin && !isIssueAdmin) {
                throw new RuntimeException("无权审批");
            }
            if (request.approved()) {
                proposal.setReviewedByAdminId(reviewerId);
                proposal.setAdminReviewComment(request.comment());
                proposal.setAdminReviewedAt(LocalDateTime.now());
                proposal.setStatus("APPROVED");
                applyChanges(proposal);
            } else {
                proposal.setReviewedByAdminId(reviewerId);
                proposal.setAdminReviewComment(request.comment());
                proposal.setAdminReviewedAt(LocalDateTime.now());
                proposal.setStatus("REJECTED");
            }
        } else {
            throw new RuntimeException("当前状态不允许审批");
        }

        proposalRepository.save(proposal);
        return toResponse(proposal);
    }

    @Override
    public List<ChangeProposalResponse> getByIssueId(Long issueId) {
        return proposalRepository.findByIssueIdOrderByCreatedAtDesc(issueId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void applyChanges(IssueChangeProposal proposal) {
        IssueFeedback issue = issueRepository.findById(proposal.getIssueId())
                .orElseThrow(() -> new RuntimeException("问题不存在"));
        issue.setTemporarySolution(proposal.getNewTemporarySolution());
        issue.setTemporaryDeadline(proposal.getNewTemporaryDeadline());
        issue.setRootCause(proposal.getNewRootCause());
        issue.setPermanentSolution(proposal.getNewPermanentSolution());
        issue.setPermanentDeadline(proposal.getNewPermanentDeadline());
        issueRepository.save(issue);
    }

    private ChangeProposalResponse toResponse(IssueChangeProposal p) {
        IssueFeedback issue = issueRepository.findById(p.getIssueId()).orElse(null);
        String proposerName = userRepository.findById(p.getProposerId()).map(u -> u.getName()).orElse("");
        String tlName = p.getReviewedByTeamLeaderId() != null
                ? userRepository.findById(p.getReviewedByTeamLeaderId()).map(u -> u.getName()).orElse("")
                : null;
        String adminName = p.getReviewedByAdminId() != null
                ? userRepository.findById(p.getReviewedByAdminId()).map(u -> u.getName()).orElse("")
                : null;

        return new ChangeProposalResponse(
                p.getId(), p.getIssueId(),
                issue != null ? issue.getIssueCode() : "",
                issue != null ? issue.getTitle() : "",
                p.getProposerId(), proposerName,
                p.getOldTemporarySolution(), p.getNewTemporarySolution(),
                p.getOldTemporaryDeadline(), p.getNewTemporaryDeadline(),
                p.getOldRootCause(), p.getNewRootCause(),
                p.getOldPermanentSolution(), p.getNewPermanentSolution(),
                p.getOldPermanentDeadline(), p.getNewPermanentDeadline(),
                p.getStatus(),
                p.getReviewedByTeamLeaderId(), tlName, p.getTeamLeaderReviewComment(), p.getTeamLeaderReviewedAt(),
                p.getReviewedByAdminId(), adminName, p.getAdminReviewComment(), p.getAdminReviewedAt(),
                p.getCreatedAt()
        );
    }
}
