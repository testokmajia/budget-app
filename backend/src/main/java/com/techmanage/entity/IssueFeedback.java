package com.techmanage.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "issue_feedbacks")
public class IssueFeedback extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(length = 50)
    private String category;

    @Column(nullable = false, length = 10)
    private String urgency;

    @Column(name = "system_name", length = 100)
    private String system;

    @Column(nullable = false)
    private Long submitterId;

    private Long assigneeId;

    @Column(length = 2000)
    private String solution;

    private LocalDate deadline;

    @Column(length = 500)
    private String progress;

    private LocalDateTime actualCompletionTime;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 500)
    private String rejectReason;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
    public String getSystem() { return system; }
    public void setSystem(String system) { this.system = system; }
    public Long getSubmitterId() { return submitterId; }
    public void setSubmitterId(Long submitterId) { this.submitterId = submitterId; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public String getSolution() { return solution; }
    public void setSolution(String solution) { this.solution = solution; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public String getProgress() { return progress; }
    public void setProgress(String progress) { this.progress = progress; }
    public LocalDateTime getActualCompletionTime() { return actualCompletionTime; }
    public void setActualCompletionTime(LocalDateTime actualCompletionTime) { this.actualCompletionTime = actualCompletionTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
}
