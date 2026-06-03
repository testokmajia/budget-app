package com.techmanage.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 需求审批评论
 */
@Entity
@Table(name = "requirement_comments")
public class RequirementComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联需求ID */
    @Column(name = "requirement_id", nullable = false)
    private Long requirementId;

    /** 评论人姓名 */
    @Column(nullable = false, length = 50)
    private String author;

    /** 评论人角色 */
    @Column(nullable = false, length = 20)
    private String role;

    /** 操作类型：提交/通过/驳回/转交/指派/完成/评估 */
    @Column(nullable = false, length = 10)
    private String action;

    /** 评论内容 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 评论时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRequirementId() { return requirementId; }
    public void setRequirementId(Long requirementId) { this.requirementId = requirementId; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
