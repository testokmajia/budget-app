package com.techmanage.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "issue_change_proposals")
public class IssueChangeProposal extends BaseEntity {

    @Column(name = "issue_id", nullable = false)
    private Long issueId;

    @Column(name = "proposer_id", nullable = false)
    private Long proposerId;

    @Column(name = "old_temporary_solution", length = 2000)
    private String oldTemporarySolution;

    @Column(name = "new_temporary_solution", length = 2000)
    private String newTemporarySolution;

    @Column(name = "old_temporary_deadline")
    private LocalDate oldTemporaryDeadline;

    @Column(name = "new_temporary_deadline")
    private LocalDate newTemporaryDeadline;

    @Column(name = "old_root_cause", length = 2000)
    private String oldRootCause;

    @Column(name = "new_root_cause", length = 2000)
    private String newRootCause;

    @Column(name = "old_permanent_solution", length = 1000)
    private String oldPermanentSolution;

    @Column(name = "new_permanent_solution", length = 1000)
    private String newPermanentSolution;

    @Column(name = "old_permanent_deadline")
    private LocalDate oldPermanentDeadline;

    @Column(name = "new_permanent_deadline")
    private LocalDate newPermanentDeadline;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "reviewed_by_team_leader_id")
    private Long reviewedByTeamLeaderId;

    @Column(name = "team_leader_review_comment", length = 500)
    private String teamLeaderReviewComment;

    @Column(name = "team_leader_reviewed_at")
    private LocalDateTime teamLeaderReviewedAt;

    @Column(name = "reviewed_by_admin_id")
    private Long reviewedByAdminId;

    @Column(name = "admin_review_comment", length = 500)
    private String adminReviewComment;

    @Column(name = "admin_reviewed_at")
    private LocalDateTime adminReviewedAt;

    public Long getIssueId() { return issueId; }
    public void setIssueId(Long issueId) { this.issueId = issueId; }
    public Long getProposerId() { return proposerId; }
    public void setProposerId(Long proposerId) { this.proposerId = proposerId; }
    public String getOldTemporarySolution() { return oldTemporarySolution; }
    public void setOldTemporarySolution(String oldTemporarySolution) { this.oldTemporarySolution = oldTemporarySolution; }
    public String getNewTemporarySolution() { return newTemporarySolution; }
    public void setNewTemporarySolution(String newTemporarySolution) { this.newTemporarySolution = newTemporarySolution; }
    public LocalDate getOldTemporaryDeadline() { return oldTemporaryDeadline; }
    public void setOldTemporaryDeadline(LocalDate oldTemporaryDeadline) { this.oldTemporaryDeadline = oldTemporaryDeadline; }
    public LocalDate getNewTemporaryDeadline() { return newTemporaryDeadline; }
    public void setNewTemporaryDeadline(LocalDate newTemporaryDeadline) { this.newTemporaryDeadline = newTemporaryDeadline; }
    public String getOldRootCause() { return oldRootCause; }
    public void setOldRootCause(String oldRootCause) { this.oldRootCause = oldRootCause; }
    public String getNewRootCause() { return newRootCause; }
    public void setNewRootCause(String newRootCause) { this.newRootCause = newRootCause; }
    public String getOldPermanentSolution() { return oldPermanentSolution; }
    public void setOldPermanentSolution(String oldPermanentSolution) { this.oldPermanentSolution = oldPermanentSolution; }
    public String getNewPermanentSolution() { return newPermanentSolution; }
    public void setNewPermanentSolution(String newPermanentSolution) { this.newPermanentSolution = newPermanentSolution; }
    public LocalDate getOldPermanentDeadline() { return oldPermanentDeadline; }
    public void setOldPermanentDeadline(LocalDate oldPermanentDeadline) { this.oldPermanentDeadline = oldPermanentDeadline; }
    public LocalDate getNewPermanentDeadline() { return newPermanentDeadline; }
    public void setNewPermanentDeadline(LocalDate newPermanentDeadline) { this.newPermanentDeadline = newPermanentDeadline; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getReviewedByTeamLeaderId() { return reviewedByTeamLeaderId; }
    public void setReviewedByTeamLeaderId(Long reviewedByTeamLeaderId) { this.reviewedByTeamLeaderId = reviewedByTeamLeaderId; }
    public String getTeamLeaderReviewComment() { return teamLeaderReviewComment; }
    public void setTeamLeaderReviewComment(String teamLeaderReviewComment) { this.teamLeaderReviewComment = teamLeaderReviewComment; }
    public LocalDateTime getTeamLeaderReviewedAt() { return teamLeaderReviewedAt; }
    public void setTeamLeaderReviewedAt(LocalDateTime teamLeaderReviewedAt) { this.teamLeaderReviewedAt = teamLeaderReviewedAt; }
    public Long getReviewedByAdminId() { return reviewedByAdminId; }
    public void setReviewedByAdminId(Long reviewedByAdminId) { this.reviewedByAdminId = reviewedByAdminId; }
    public String getAdminReviewComment() { return adminReviewComment; }
    public void setAdminReviewComment(String adminReviewComment) { this.adminReviewComment = adminReviewComment; }
    public LocalDateTime getAdminReviewedAt() { return adminReviewedAt; }
    public void setAdminReviewedAt(LocalDateTime adminReviewedAt) { this.adminReviewedAt = adminReviewedAt; }
}
