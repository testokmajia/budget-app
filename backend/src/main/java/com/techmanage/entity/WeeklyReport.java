package com.techmanage.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weekly_reports", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"userId", "weekStartDate"})
})
public class WeeklyReport extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate weekStartDate;

    @Column(nullable = false)
    private LocalDate weekEndDate;

    @Column(columnDefinition = "TEXT")
    private String doneWork;

    @Column(columnDefinition = "TEXT")
    private String planWork;

    @Column(columnDefinition = "TEXT")
    private String problems;

    @Column(columnDefinition = "TEXT")
    private String supportNeeded;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(columnDefinition = "TEXT")
    private String reviewComment;

    private Long reviewerId;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    @Column(nullable = false)
    private boolean merged = false;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getWeekStartDate() { return weekStartDate; }
    public void setWeekStartDate(LocalDate weekStartDate) { this.weekStartDate = weekStartDate; }
    public LocalDate getWeekEndDate() { return weekEndDate; }
    public void setWeekEndDate(LocalDate weekEndDate) { this.weekEndDate = weekEndDate; }
    public String getDoneWork() { return doneWork; }
    public void setDoneWork(String doneWork) { this.doneWork = doneWork; }
    public String getPlanWork() { return planWork; }
    public void setPlanWork(String planWork) { this.planWork = planWork; }
    public String getProblems() { return problems; }
    public void setProblems(String problems) { this.problems = problems; }
    public String getSupportNeeded() { return supportNeeded; }
    public void setSupportNeeded(String supportNeeded) { this.supportNeeded = supportNeeded; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public boolean isMerged() { return merged; }
    public void setMerged(boolean merged) { this.merged = merged; }
}
