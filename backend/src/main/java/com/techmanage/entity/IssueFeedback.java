package com.techmanage.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "issue_feedbacks")
public class IssueFeedback extends BaseEntity {

    @Column(name = "issue_code", unique = true, nullable = false, length = 20)
    private String issueCode;

    @Column(name = "submitter_id", nullable = false)
    private Long submitterId;

    @Column(name = "submitter_department", nullable = false, length = 100)
    private String submitterDepartment;

    @Column(name = "occasion_id")
    private Long occasionId;

    @Column(name = "meeting_department", length = 200)
    private String meetingDepartment;

    @Column(name = "meeting_date")
    private LocalDate meetingDate;

    @Column(nullable = false, length = 60)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "responsible_team", length = 100)
    private String responsibleTeam;

    @Column(name = "responsible_person_id")
    private Long responsiblePersonId;

    @Column(name = "temporary_solution", length = 2000)
    private String temporarySolution;

    @Column(name = "temporary_deadline")
    private LocalDate temporaryDeadline;

    @Column(name = "issue_type", length = 50)
    private String issueType;

    @Column(name = "root_cause", length = 2000)
    private String rootCause;

    @Column(name = "permanent_solution", length = 1000)
    private String permanentSolution;

    @Column(name = "permanent_deadline")
    private LocalDate permanentDeadline;

    @Column(nullable = false, length = 20)
    private String status;

    public String getIssueCode() { return issueCode; }
    public void setIssueCode(String issueCode) { this.issueCode = issueCode; }
    public Long getSubmitterId() { return submitterId; }
    public void setSubmitterId(Long submitterId) { this.submitterId = submitterId; }
    public String getSubmitterDepartment() { return submitterDepartment; }
    public void setSubmitterDepartment(String submitterDepartment) { this.submitterDepartment = submitterDepartment; }
    public Long getOccasionId() { return occasionId; }
    public void setOccasionId(Long occasionId) { this.occasionId = occasionId; }
    public String getMeetingDepartment() { return meetingDepartment; }
    public void setMeetingDepartment(String meetingDepartment) { this.meetingDepartment = meetingDepartment; }
    public LocalDate getMeetingDate() { return meetingDate; }
    public void setMeetingDate(LocalDate meetingDate) { this.meetingDate = meetingDate; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getResponsibleTeam() { return responsibleTeam; }
    public void setResponsibleTeam(String responsibleTeam) { this.responsibleTeam = responsibleTeam; }
    public Long getResponsiblePersonId() { return responsiblePersonId; }
    public void setResponsiblePersonId(Long responsiblePersonId) { this.responsiblePersonId = responsiblePersonId; }
    public String getTemporarySolution() { return temporarySolution; }
    public void setTemporarySolution(String temporarySolution) { this.temporarySolution = temporarySolution; }
    public LocalDate getTemporaryDeadline() { return temporaryDeadline; }
    public void setTemporaryDeadline(LocalDate temporaryDeadline) { this.temporaryDeadline = temporaryDeadline; }
    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }
    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }
    public String getPermanentSolution() { return permanentSolution; }
    public void setPermanentSolution(String permanentSolution) { this.permanentSolution = permanentSolution; }
    public LocalDate getPermanentDeadline() { return permanentDeadline; }
    public void setPermanentDeadline(LocalDate permanentDeadline) { this.permanentDeadline = permanentDeadline; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
