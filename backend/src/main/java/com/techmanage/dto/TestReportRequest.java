package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 测试报告创建/更新请求
 */
public class TestReportRequest {

    @NotBlank(message = "测试报告标题不能为空")
    private String title;

    @NotBlank(message = "请选择需求")
    private String requirementIds; // JSON数组字符串

    private String requirementCodes;
    private String systemNames;    // JSON数组字符串
    private String reviewPersons;  // JSON数组字符串

    /** 测试报告附件路径 */
    private String testReportPath;

    /** 测试报告原始文件名 */
    private String testReportName;

    /** 确认意见 */
    private String comment;

    /** 确认日期（确认测试报告时填写，YYYY-MM-DD） */
    private String confirmedDate;

    /** 计划测试时间 */
    private String plannedTestDate;

    /** 计划投产时间 */
    private String plannedProductionDate;

    // ==================== getters & setters ====================

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getRequirementIds() { return requirementIds; }
    public void setRequirementIds(String requirementIds) { this.requirementIds = requirementIds; }
    public String getRequirementCodes() { return requirementCodes; }
    public void setRequirementCodes(String requirementCodes) { this.requirementCodes = requirementCodes; }
    public String getSystemNames() { return systemNames; }
    public void setSystemNames(String systemNames) { this.systemNames = systemNames; }
    public String getReviewPersons() { return reviewPersons; }
    public void setReviewPersons(String reviewPersons) { this.reviewPersons = reviewPersons; }
    public String getTestReportPath() { return testReportPath; }
    public void setTestReportPath(String testReportPath) { this.testReportPath = testReportPath; }
    public String getTestReportName() { return testReportName; }
    public void setTestReportName(String testReportName) { this.testReportName = testReportName; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getConfirmedDate() { return confirmedDate; }
    public void setConfirmedDate(String confirmedDate) { this.confirmedDate = confirmedDate; }
    public String getPlannedTestDate() { return plannedTestDate; }
    public void setPlannedTestDate(String plannedTestDate) { this.plannedTestDate = plannedTestDate; }
    public String getPlannedProductionDate() { return plannedProductionDate; }
    public void setPlannedProductionDate(String plannedProductionDate) { this.plannedProductionDate = plannedProductionDate; }
}
