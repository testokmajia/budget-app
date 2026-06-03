package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建/更新需求请求
 */
public class RequirementRequest {

    @NotBlank(message = "需求标题不能为空")
    private String title;

    @NotBlank(message = "需求内容不能为空")
    private String content;

    @NotBlank(message = "优先级不能为空")
    private String priority;

    private String expectedDate;

    /** 可选的提出人ID，不传则默认当前用户 */
    private Long submitterId;

    /** 审批意见（审批节点操作时填写） */
    private String comment;

    /** 涉及系统JSON（架构管理岗填写） */
    private String systemItems;

    /** 指派的开发人员（项目经理填写） */
    private String assignedDev;

    /** 指派项目经理（团队组长填写） */
    private String assignedPm;

    /** 指派产品经理（团队组长填写） */
    private String assignedPd;

    /** 投产计划JSON（第二次项目经理节点填写） */
    private String deployments;

    /** 主责系统名称（架构管理岗指定） */
    private String primarySystemName;

    /** 需求说明书路径（产品经理上传） */
    private String specDocumentPath;

    /** 确认人员JSON（产品经理设置） */
    private String specReviewers;

    /** 计划测试时间（PM启动实施时填写） */
    private String plannedTestDate;

    /** 计划投产时间（PM启动实施时填写） */
    private String plannedProductionDate;

    /** 正式投产日期（PM/PD在测试通过后填写） */
    private String productionDate;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getExpectedDate() { return expectedDate; }
    public void setExpectedDate(String expectedDate) { this.expectedDate = expectedDate; }
    public Long getSubmitterId() { return submitterId; }
    public void setSubmitterId(Long submitterId) { this.submitterId = submitterId; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
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
    public String getPrimarySystemName() { return primarySystemName; }
    public void setPrimarySystemName(String primarySystemName) { this.primarySystemName = primarySystemName; }
    public String getSpecDocumentPath() { return specDocumentPath; }
    public void setSpecDocumentPath(String specDocumentPath) { this.specDocumentPath = specDocumentPath; }
    public String getSpecReviewers() { return specReviewers; }
    public void setSpecReviewers(String specReviewers) { this.specReviewers = specReviewers; }
    public String getPlannedTestDate() { return plannedTestDate; }
    public void setPlannedTestDate(String plannedTestDate) { this.plannedTestDate = plannedTestDate; }
    public String getPlannedProductionDate() { return plannedProductionDate; }
    public void setPlannedProductionDate(String plannedProductionDate) { this.plannedProductionDate = plannedProductionDate; }
    public String getProductionDate() { return productionDate; }
    public void setProductionDate(String productionDate) { this.productionDate = productionDate; }
}
