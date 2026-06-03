package com.techmanage.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 测试报告实体（一个报告可覆盖多个需求）
 */
@Entity
@Table(name = "test_reports")
public class TestReport extends BaseEntity {

    /** 测试报告标题 */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 关联需求ID，JSON数组：[1, 2, 3]
     */
    @Column(name = "requirement_ids", nullable = false, columnDefinition = "TEXT")
    private String requirementIds;

    /** 关联需求编号，冗余展示："REQ-2026-001、REQ-2026-002" */
    @Column(name = "requirement_codes", length = 500)
    private String requirementCodes;

    /**
     * 涉及系统名，JSON数组：["系统A", "系统B"]
     */
    @Column(name = "system_names", columnDefinition = "TEXT")
    private String systemNames;

    /**
     * 确认人员列表，JSON格式：
     * [{"name":"姓名","role":"角色","system":"所属系统","confirmed":false,"confirmedAt":null}]
     */
    @Column(name = "review_persons", columnDefinition = "TEXT")
    private String reviewPersons;

    /** 测试报告附件路径 */
    @Column(name = "test_report_path", length = 500)
    private String testReportPath;

    /** 状态：草稿/已确认 */
    @Column(nullable = false, length = 20)
    private String status;

    /** 计划测试时间 */
    @Column(name = "planned_test_date")
    private LocalDate plannedTestDate;

    /** 计划投产时间 */
    @Column(name = "planned_production_date")
    private LocalDate plannedProductionDate;

    /** 上传人ID */
    @Column(name = "submitter_id")
    private Long submitterId;

    /** 上传人姓名 */
    @Column(name = "submitter_name", length = 50)
    private String submitterName;

    /** 报告确认时间（全部确认后写入） */
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getPlannedTestDate() { return plannedTestDate; }
    public void setPlannedTestDate(LocalDate plannedTestDate) { this.plannedTestDate = plannedTestDate; }
    public LocalDate getPlannedProductionDate() { return plannedProductionDate; }
    public void setPlannedProductionDate(LocalDate plannedProductionDate) { this.plannedProductionDate = plannedProductionDate; }
    public Long getSubmitterId() { return submitterId; }
    public void setSubmitterId(Long submitterId) { this.submitterId = submitterId; }
    public String getSubmitterName() { return submitterName; }
    public void setSubmitterName(String submitterName) { this.submitterName = submitterName; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
}
