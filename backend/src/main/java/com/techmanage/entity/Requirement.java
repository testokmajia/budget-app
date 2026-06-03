package com.techmanage.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * 需求实体
 */
@Entity
@Table(name = "requirements")
public class Requirement extends BaseEntity {

    /** 需求编号，如 REQ-2026-001 */
    @Column(name = "requirement_code", unique = true, nullable = false, length = 20)
    private String requirementCode;

    /** 需求标题 */
    @Column(nullable = false, length = 200)
    private String title;

    /** 需求内容 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 提出人ID */
    @Column(name = "submitter_id", nullable = false)
    private Long submitterId;

    /** 提出人姓名 */
    @Column(name = "submitter_name", nullable = false, length = 50)
    private String submitterName;

    /** 提出部门 */
    @Column(nullable = false, length = 100)
    private String dept;

    /** 优先级：普通/高/紧急/低 */
    @Column(nullable = false, length = 10)
    private String priority;

    /** 状态：草稿/审批中/实施中/已完成/已驳回 */
    @Column(nullable = false, length = 10)
    private String status;

    /** 当前审批节点名称 */
    @Column(name = "current_node", nullable = false, length = 20)
    private String currentNode;

    /** 当前节点序号 1-9 */
    @Column(name = "node_index", nullable = false)
    private Integer nodeIndex;

    /** 期望完成时间 */
    @Column(name = "expected_date")
    private LocalDate expectedDate;

    /**
     * 涉及系统列表，JSON格式：
     * [{"name":"系统名","team":"团队","owner":"负责人","modification":"改造内容"}]
     */
    @Column(name = "system_items", columnDefinition = "TEXT")
    private String systemItems;

    /** 指派的开发人员 */
    @Column(name = "assigned_dev", length = 50)
    private String assignedDev;

    /** 项目经理（团队组长指派） */
    @Column(name = "assigned_pm", length = 50)
    private String assignedPm;

    /** 产品经理（团队组长指派） */
    @Column(name = "assigned_pd", length = 50)
    private String assignedPd;

    /** 主责系统名称（架构管理岗指定） */
    @Column(name = "primary_system_name", length = 100)
    private String primarySystemName;

    /** 需求说明书文件路径（产品经理上传） */
    @Column(name = "spec_document_path", length = 500)
    private String specDocumentPath;

    /**
     * 确认人员列表，JSON格式：
     * [{"name":"姓名","role":"系统负责人|产品经理|业务人员","system":"系统名"}]
     */
    @Column(name = "spec_reviewers", columnDefinition = "TEXT")
    private String specReviewers;

    /**
     * 投产计划，JSON格式：
     * [{"date":"投产日期","desc":"投产说明"}]
     */
    @Column(columnDefinition = "TEXT")
    private String deployments;

    /** 附件路径 */
    @Column(name = "attachment_path", length = 500)
    private String attachmentPath;

    /** 计划测试时间（PM启动实施时填写） */
    @Column(name = "planned_test_date")
    private LocalDate plannedTestDate;

    /** 计划投产时间（PM启动实施时填写） */
    @Column(name = "planned_production_date")
    private LocalDate plannedProductionDate;

    /** 正式投产日期（PM/PD在测试通过后填写） */
    @Column(name = "production_date")
    private LocalDate productionDate;

    // ==================== getters & setters ====================

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
    public String getSystemItems() { return systemItems; }
    public void setSystemItems(String systemItems) { this.systemItems = systemItems; }
    public String getAssignedDev() { return assignedDev; }
    public void setAssignedDev(String assignedDev) { this.assignedDev = assignedDev; }
    public String getAssignedPm() { return assignedPm; }
    public void setAssignedPm(String assignedPm) { this.assignedPm = assignedPm; }
    public String getAssignedPd() { return assignedPd; }
    public void setAssignedPd(String assignedPd) { this.assignedPd = assignedPd; }
    public String getDeployments() { return deployments; }
    public void setDeployments(String deployments) { this.deployments = deployments; }
    public String getAttachmentPath() { return attachmentPath; }
    public void setAttachmentPath(String attachmentPath) { this.attachmentPath = attachmentPath; }
    public String getPrimarySystemName() { return primarySystemName; }
    public void setPrimarySystemName(String primarySystemName) { this.primarySystemName = primarySystemName; }
    public String getSpecDocumentPath() { return specDocumentPath; }
    public void setSpecDocumentPath(String specDocumentPath) { this.specDocumentPath = specDocumentPath; }
    public String getSpecReviewers() { return specReviewers; }
    public void setSpecReviewers(String specReviewers) { this.specReviewers = specReviewers; }
    public LocalDate getPlannedTestDate() { return plannedTestDate; }
    public void setPlannedTestDate(LocalDate plannedTestDate) { this.plannedTestDate = plannedTestDate; }
    public LocalDate getPlannedProductionDate() { return plannedProductionDate; }
    public void setPlannedProductionDate(LocalDate plannedProductionDate) { this.plannedProductionDate = plannedProductionDate; }
    public LocalDate getProductionDate() { return productionDate; }
    public void setProductionDate(LocalDate productionDate) { this.productionDate = productionDate; }
}
