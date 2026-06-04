package com.techmanage.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 测试报告审核记录
 */
@Entity
@Table(name = "test_report_comments")
public class TestReportComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联测试报告ID */
    @Column(name = "report_id", nullable = false)
    private Long reportId;

    /** 操作人姓名 */
    @Column(nullable = false, length = 50)
    private String author;

    /** 操作人角色 */
    @Column(nullable = false, length = 30)
    private String role;

    /** 操作类型：提交/确认/驳回/部门审核通过/部门审核驳回 */
    @Column(nullable = false, length = 20)
    private String action;

    /** 审核意见 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 操作时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ==================== getters & setters ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author != null ? author.trim() : null; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
