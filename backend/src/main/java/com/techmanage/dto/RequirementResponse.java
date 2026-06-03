package com.techmanage.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 需求列表/详情响应
 */
public class RequirementResponse {

    private Long id;
    private String requirementCode;
    private String title;
    private String content;
    private Long submitterId;
    private String submitterName;
    private String dept;
    private String priority;
    private String status;
    private String currentNode;
    private Integer nodeIndex;
    private LocalDate expectedDate;
    private List<SystemItem> systemItems;
    private String assignedDev;
    private String assignedPm;
    private String assignedPd;
    private List<DeploymentItem> deployments;
    private String primarySystemName;
    private String specDocumentPath;
    private String specReviewers;
    private String attachmentPath;
    private LocalDate plannedTestDate;
    private LocalDate plannedProductionDate;
    private LocalDate productionDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 审批评论列表 */
    private List<CommentItem> comments;

    // 内嵌类型
    public record SystemItem(String name, String team, String owner, String modification, String pm, String pd) {}
    public record DeploymentItem(String date, String desc) {}
    public record CommentItem(String author, String role, String action, String content, LocalDateTime time) {}

    // ==================== getters & setters ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRequirementCode() { return requirementCode; }
    public void setRequirementCode(String requirementCode) { this.requirementCode = requirementCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getSubmitterId() { return submitterId; }
    public void setSubmitterId(Long submitterId) { this.submitterId = submitterId; }
    public String getSubmitterName() { return submitterName; }
    public void setSubmitterName(String submitterName) { this.submitterName = submitterName; }
    public String getDept() { return dept; }
    public void setDept(String dept) { this.dept = dept; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCurrentNode() { return currentNode; }
    public void setCurrentNode(String currentNode) { this.currentNode = currentNode; }
    public Integer getNodeIndex() { return nodeIndex; }
    public void setNodeIndex(Integer nodeIndex) { this.nodeIndex = nodeIndex; }
    public LocalDate getExpectedDate() { return expectedDate; }
    public void setExpectedDate(LocalDate expectedDate) { this.expectedDate = expectedDate; }
    public List<SystemItem> getSystemItems() { return systemItems; }
    public void setSystemItems(List<SystemItem> systemItems) { this.systemItems = systemItems; }
    public String getAssignedDev() { return assignedDev; }
    public void setAssignedDev(String assignedDev) { this.assignedDev = assignedDev; }
    public String getAssignedPm() { return assignedPm; }
    public void setAssignedPm(String assignedPm) { this.assignedPm = assignedPm; }
    public String getAssignedPd() { return assignedPd; }
    public void setAssignedPd(String assignedPd) { this.assignedPd = assignedPd; }
    public List<DeploymentItem> getDeployments() { return deployments; }
    public void setDeployments(List<DeploymentItem> deployments) { this.deployments = deployments; }
    public String getPrimarySystemName() { return primarySystemName; }
    public void setPrimarySystemName(String primarySystemName) { this.primarySystemName = primarySystemName; }
    public String getSpecDocumentPath() { return specDocumentPath; }
    public void setSpecDocumentPath(String specDocumentPath) { this.specDocumentPath = specDocumentPath; }
    public String getSpecReviewers() { return specReviewers; }
    public void setSpecReviewers(String specReviewers) { this.specReviewers = specReviewers; }
    public String getAttachmentPath() { return attachmentPath; }
    public void setAttachmentPath(String attachmentPath) { this.attachmentPath = attachmentPath; }
    public LocalDate getPlannedTestDate() { return plannedTestDate; }
    public void setPlannedTestDate(LocalDate plannedTestDate) { this.plannedTestDate = plannedTestDate; }
    public LocalDate getPlannedProductionDate() { return plannedProductionDate; }
    public void setPlannedProductionDate(LocalDate plannedProductionDate) { this.plannedProductionDate = plannedProductionDate; }
    public LocalDate getProductionDate() { return productionDate; }
    public void setProductionDate(LocalDate productionDate) { this.productionDate = productionDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<CommentItem> getComments() { return comments; }
    public void setComments(List<CommentItem> comments) { this.comments = comments; }
}
