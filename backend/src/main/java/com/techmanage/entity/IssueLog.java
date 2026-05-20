package com.techmanage.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "issue_logs")
public class IssueLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long issueId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 500)
    private String remark;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public IssueLog() {}

    public IssueLog(Long issueId, Long userId, String action, String remark) {
        this.issueId = issueId;
        this.userId = userId;
        this.action = action;
        this.remark = remark;
    }

    public Long getId() { return id; }
    public Long getIssueId() { return issueId; }
    public Long getUserId() { return userId; }
    public String getAction() { return action; }
    public String getRemark() { return remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
