package com.techmanage.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "team_summaries", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"team_name", "week_start_date"})
})
public class TeamSummary extends BaseEntity {

    @Column(name = "team_name", nullable = false, length = 100)
    private String teamName;

    @Column(name = "leader_id", nullable = false)
    private Long leaderId;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "week_end_date", nullable = false)
    private LocalDate weekEndDate;

    @Column(name = "merged_content", columnDefinition = "TEXT")
    private String mergedContent;

    @Column(name = "edited_content", columnDefinition = "TEXT")
    private String editedContent;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "source_report_ids", length = 500)
    private String sourceReportIds;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public Long getLeaderId() { return leaderId; }
    public void setLeaderId(Long leaderId) { this.leaderId = leaderId; }
    public LocalDate getWeekStartDate() { return weekStartDate; }
    public void setWeekStartDate(LocalDate weekStartDate) { this.weekStartDate = weekStartDate; }
    public LocalDate getWeekEndDate() { return weekEndDate; }
    public void setWeekEndDate(LocalDate weekEndDate) { this.weekEndDate = weekEndDate; }
    public String getMergedContent() { return mergedContent; }
    public void setMergedContent(String mergedContent) { this.mergedContent = mergedContent; }
    public String getEditedContent() { return editedContent; }
    public void setEditedContent(String editedContent) { this.editedContent = editedContent; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSourceReportIds() { return sourceReportIds; }
    public void setSourceReportIds(String sourceReportIds) { this.sourceReportIds = sourceReportIds; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
