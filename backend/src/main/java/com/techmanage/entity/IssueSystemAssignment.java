package com.techmanage.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "issue_system_assignments")
public class IssueSystemAssignment extends BaseEntity {

    @Column(name = "issue_id", nullable = false)
    private Long issueId;

    @Column(name = "system_name", nullable = false, length = 100)
    private String systemName;

    @Column(name = "system_owner_id", nullable = false)
    private Long systemOwnerId;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completion_note", length = 300)
    private String completionNote;

    public IssueSystemAssignment() {}

    public IssueSystemAssignment(Long issueId, String systemName, Long systemOwnerId) {
        this.issueId = issueId;
        this.systemName = systemName;
        this.systemOwnerId = systemOwnerId;
    }

    public Long getIssueId() { return issueId; }
    public void setIssueId(Long issueId) { this.issueId = issueId; }
    public String getSystemName() { return systemName; }
    public void setSystemName(String systemName) { this.systemName = systemName; }
    public Long getSystemOwnerId() { return systemOwnerId; }
    public void setSystemOwnerId(Long systemOwnerId) { this.systemOwnerId = systemOwnerId; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getCompletionNote() { return completionNote; }
    public void setCompletionNote(String completionNote) { this.completionNote = completionNote; }
}
