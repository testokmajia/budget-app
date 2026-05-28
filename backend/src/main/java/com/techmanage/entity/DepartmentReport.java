package com.techmanage.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "department_reports", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"department", "weekStartDate"})
})
public class DepartmentReport extends BaseEntity {

    @Column(nullable = false)
    private LocalDate weekStartDate;

    @Column(nullable = false)
    private LocalDate weekEndDate;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(columnDefinition = "LONGTEXT")
    private String mergedContent;

    @Column(columnDefinition = "LONGTEXT")
    private String editedContent;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(columnDefinition = "TEXT")
    private String sourceReportIds;

    private LocalDateTime submittedAt;

    private LocalDateTime finalizedAt;

    private Long finalizedBy;

    public LocalDate getWeekStartDate() { return weekStartDate; }
    public void setWeekStartDate(LocalDate weekStartDate) { this.weekStartDate = weekStartDate; }
    public LocalDate getWeekEndDate() { return weekEndDate; }
    public void setWeekEndDate(LocalDate weekEndDate) { this.weekEndDate = weekEndDate; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
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
    public LocalDateTime getFinalizedAt() { return finalizedAt; }
    public void setFinalizedAt(LocalDateTime finalizedAt) { this.finalizedAt = finalizedAt; }
    public Long getFinalizedBy() { return finalizedBy; }
    public void setFinalizedBy(Long finalizedBy) { this.finalizedBy = finalizedBy; }
}
