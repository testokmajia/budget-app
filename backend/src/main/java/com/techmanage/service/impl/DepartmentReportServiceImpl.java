package com.techmanage.service.impl;

import com.techmanage.common.BusinessException;
import com.techmanage.dto.DepartmentReportRequest;
import com.techmanage.dto.DepartmentReportResponse;
import com.techmanage.dto.WeeklyReportResponse;
import com.techmanage.entity.DepartmentReport;
import com.techmanage.entity.TeamSummary;
import com.techmanage.entity.User;
import com.techmanage.entity.WeeklyReport;
import com.techmanage.repository.DepartmentReportRepository;
import com.techmanage.repository.TeamRepository;
import com.techmanage.repository.TeamSummaryRepository;
import com.techmanage.repository.UserRepository;
import com.techmanage.repository.WeeklyReportRepository;
import com.techmanage.service.AiService;
import com.techmanage.service.DepartmentReportService;
import com.techmanage.util.TeamUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentReportServiceImpl implements DepartmentReportService {

    private final DepartmentReportRepository deptReportRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamSummaryRepository teamSummaryRepository;
    private final AiService aiService;

    public DepartmentReportServiceImpl(DepartmentReportRepository deptReportRepository,
                                        WeeklyReportRepository weeklyReportRepository,
                                        UserRepository userRepository,
                                        TeamRepository teamRepository,
                                        TeamSummaryRepository teamSummaryRepository,
                                        AiService aiService) {
        this.deptReportRepository = deptReportRepository;
        this.weeklyReportRepository = weeklyReportRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.teamSummaryRepository = teamSummaryRepository;
        this.aiService = aiService;
    }

    @Override
    public DepartmentReportResponse mergeAi(Long clerkUserId, DepartmentReportRequest request) {
        final LocalDate requestedWeek = request.weekStartDate() != null
            ? request.weekStartDate()
            : LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        final LocalDate requestedWeekEnd = request.weekEndDate() != null
            ? request.weekEndDate()
            : requestedWeek.plusDays(4);

        // 尝试请求的周，无数据则回退到最新有数据的周
        LocalDate targetWeek = requestedWeek;
        LocalDate weekEnd = requestedWeekEnd;

        // 从目标周获取已提交/已审批的组内汇总
        var teamSummaries = teamSummaryRepository.findByWeekStartDate(targetWeek);
        List<TeamSummary> submittedSummaries = teamSummaries.stream()
            .filter(ts -> "SUBMITTED".equals(ts.getStatus()) || "APPROVED".equals(ts.getStatus()))
            .toList();

        // 从目标周获取已提交/已审批的个人周报
        List<WeeklyReport> fallbackReports = List.of();
        if (submittedSummaries.isEmpty()) {
            fallbackReports = weeklyReportRepository.findByWeekStartDate(targetWeek).stream()
                .filter(r -> "APPROVED".equals(r.getStatus()) || "SUBMITTED".equals(r.getStatus()))
                .toList();
        }

        // 请求的周无任何数据，查找最新有数据的周
        if (submittedSummaries.isEmpty() && fallbackReports.isEmpty()) {
            LocalDate latestWeek = findLatestWeekWithData();
            if (latestWeek != null && !latestWeek.equals(targetWeek)) {
                targetWeek = latestWeek;
                weekEnd = targetWeek.plusDays(4);
                // 重新加载新目标周的数据
                teamSummaries = teamSummaryRepository.findByWeekStartDate(targetWeek);
                submittedSummaries = teamSummaries.stream()
                    .filter(ts -> "SUBMITTED".equals(ts.getStatus()) || "APPROVED".equals(ts.getStatus()))
                    .toList();
                if (submittedSummaries.isEmpty()) {
                    fallbackReports = weeklyReportRepository.findByWeekStartDate(targetWeek).stream()
                        .filter(r -> "APPROVED".equals(r.getStatus()) || "SUBMITTED".equals(r.getStatus()))
                        .toList();
                }
            }
        }

        if (submittedSummaries.isEmpty() && fallbackReports.isEmpty()) {
            throw new BusinessException("本周暂无已提交或已审批的周报，请等待各组提交汇总后再试");
        }

        String aiResult;
        StringBuilder sourceIds = new StringBuilder();

        if (!submittedSummaries.isEmpty()) {
            // Merge from team summaries
            List<WeeklyReportResponse> summaryContents = submittedSummaries.stream()
                .map(ts -> {
                    String content = ts.getEditedContent() != null ? ts.getEditedContent() : ts.getMergedContent();
                    return new WeeklyReportResponse(
                        ts.getId(), ts.getLeaderId(), ts.getTeamName(), "信息科技部", ts.getTeamName(),
                        ts.getWeekStartDate(), ts.getWeekEndDate(),
                        content, null, null, null,
                        "APPROVED", null, ts.getSubmittedAt(), null,
                        null, false, 1, ts.getCreatedAt(), ts.getUpdatedAt()
                    );
                })
                .toList();
            aiResult = aiService.mergeReports(summaryContents);
            sourceIds.append(submittedSummaries.stream()
                .map(ts -> "ts_" + ts.getId())
                .collect(Collectors.joining(",")));
        } else {
            // Fallback: merge from individual approved/submitted reports
            List<WeeklyReportResponse> reportResponses = fallbackReports.stream()
                .map(this::toWeeklyResponse)
                .toList();

            aiResult = aiService.mergeReports(reportResponses);
            sourceIds.append(fallbackReports.stream()
                .map(r -> String.valueOf(r.getId()))
                .collect(Collectors.joining(",")));

            // Mark individual reports as merged
            for (var r : fallbackReports) {
                r.setMerged(true);
                weeklyReportRepository.save(r);
            }
        }

        var existing = deptReportRepository.findByDepartmentAndWeekStartDate("信息科技部", targetWeek);
        DepartmentReport report;
        if (existing.isPresent()) {
            report = existing.get();
            report.setMergedContent(aiResult);
            report.setEditedContent(null);
            report.setStatus("DRAFT");
            report.setSubmittedAt(null);
            report.setFinalizedAt(null);
            report.setFinalizedBy(null);
        } else {
            report = new DepartmentReport();
            report.setDepartment("信息科技部");
            report.setWeekStartDate(targetWeek);
            report.setWeekEndDate(weekEnd);
            report.setMergedContent(aiResult);
        }
        report.setSourceReportIds(sourceIds.toString());

        deptReportRepository.save(report);
        return toResponse(report);
    }

    /**
     * 在所有已提交/已审批的组内汇总或个人周报中查找最新有数据的周。
     */
    private LocalDate findLatestWeekWithData() {
        final LocalDate latestSummaryWeek = teamSummaryRepository.findAll().stream()
            .filter(ts -> "SUBMITTED".equals(ts.getStatus()) || "APPROVED".equals(ts.getStatus()))
            .map(TeamSummary::getWeekStartDate)
            .max(LocalDate::compareTo)
            .orElse(null);

        final LocalDate latestReportWeek = weeklyReportRepository.findAll().stream()
            .filter(r -> "APPROVED".equals(r.getStatus()) || "SUBMITTED".equals(r.getStatus()))
            .map(WeeklyReport::getWeekStartDate)
            .max(LocalDate::compareTo)
            .orElse(null);

        if (latestSummaryWeek != null && latestReportWeek != null) {
            return latestSummaryWeek.isAfter(latestReportWeek) ? latestSummaryWeek : latestReportWeek;
        }
        return latestSummaryWeek != null ? latestSummaryWeek : latestReportWeek;
    }

    @Override
    public DepartmentReportResponse getById(Long id) {
        var report = deptReportRepository.findById(id)
            .orElseThrow(() -> new BusinessException("部门周报不存在"));
        return toResponse(report);
    }

    @Override
    public DepartmentReportResponse update(Long id, Long clerkUserId, String editedContent) {
        var report = deptReportRepository.findById(id)
            .orElseThrow(() -> new BusinessException("部门周报不存在"));
        if (!"DRAFT".equals(report.getStatus())) {
            throw new BusinessException("当前状态不可编辑");
        }
        report.setEditedContent(editedContent);
        deptReportRepository.save(report);
        return toResponse(report);
    }

    @Override
    @Transactional
    public DepartmentReportResponse submit(Long id, Long clerkUserId) {
        var report = deptReportRepository.findById(id)
            .orElseThrow(() -> new BusinessException("部门周报不存在"));
        if (!"DRAFT".equals(report.getStatus())) {
            throw new BusinessException("当前状态不可提交");
        }
        report.setStatus("PENDING_REVIEW");
        report.setSubmittedAt(LocalDateTime.now());
        deptReportRepository.save(report);

        // Auto-approve all team summaries for this week
        autoApproveTeamSummaries(report);

        return toResponse(report);
    }

    /**
     * When department clerk submits department report,
     * auto-approve all team summaries for this week.
     */
    private void autoApproveTeamSummaries(DepartmentReport deptReport) {
        var summaries = teamSummaryRepository.findByWeekStartDate(deptReport.getWeekStartDate());
        for (var summary : summaries) {
            if ("SUBMITTED".equals(summary.getStatus())) {
                summary.setStatus("APPROVED");
                teamSummaryRepository.save(summary);
            }
        }
    }

    @Override
    public DepartmentReportResponse finalize(Long id, Long headUserId) {
        var report = deptReportRepository.findById(id)
            .orElseThrow(() -> new BusinessException("部门周报不存在"));
        if (!"PENDING_REVIEW".equals(report.getStatus())) {
            throw new BusinessException("当前状态不可审定");
        }
        report.setStatus("FINALIZED");
        report.setFinalizedAt(LocalDateTime.now());
        report.setFinalizedBy(headUserId);
        deptReportRepository.save(report);
        return toResponse(report);
    }

    @Override
    public List<DepartmentReportResponse> listDepartmentReports() {
        return deptReportRepository.findByDepartment("信息科技部").stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    public byte[] exportWord(Long id) {
        var report = deptReportRepository.findById(id)
            .orElseThrow(() -> new BusinessException("部门周报不存在"));
        String content = report.getEditedContent() != null ? report.getEditedContent() : report.getMergedContent();
        if (content == null) content = "";
        // Simple HTML wrapping for Word export (Word can open HTML)
        String html = "<html><head><meta charset='UTF-8'><style>" +
            "body{font-family:'PingFang SC','Microsoft YaHei',sans-serif;line-height:1.8;padding:20px;}" +
            "h1{color:#c41230;}" +
            "</style></head><body><h1>信息科技部周报</h1>" +
            "<p>周期：" + report.getWeekStartDate() + " ~ " + report.getWeekEndDate() + "</p>" +
            "<div>" + content.replace("\n", "<br>") + "</div></body></html>";
        try {
            return html.getBytes("UTF-8");
        } catch (Exception e) {
            throw new BusinessException("导出失败", e);
        }
    }

    @Override
    public boolean isCurrentWeekFinalized() {
        LocalDate thisMonday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        return deptReportRepository.findByDepartmentAndWeekStartDate("信息科技部", thisMonday)
            .map(r -> "FINALIZED".equals(r.getStatus()))
            .orElse(false);
    }

    @Override
    public String exportHtml(Long id) {
        var report = deptReportRepository.findById(id)
            .orElseThrow(() -> new BusinessException("部门周报不存在"));
        String content = report.getEditedContent() != null ? report.getEditedContent() : report.getMergedContent();
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>部门周报</title>" +
            "<style>body{font-family:'PingFang SC','Microsoft YaHei',sans-serif;line-height:1.8;max-width:800px;margin:0 auto;padding:20px;}" +
            "h1{color:#c41230;}</style></head><body>" +
            "<h1>信息科技部周报</h1>" +
            "<p>" + report.getWeekStartDate() + " ~ " + report.getWeekEndDate() + "</p>" +
            "<div>" + (content != null ? content.replace("\n", "<br>") : "") + "</div>" +
            "</body></html>";
    }

    private WeeklyReportResponse toWeeklyResponse(WeeklyReport r) {
        String userName = "";
        String userDepartment = "";
        String teamName = null;
        User user = userRepository.findById(r.getUserId()).orElse(null);
        if (user != null) {
            userName = user.getName();
            userDepartment = user.getDepartment() != null ? user.getDepartment() : "";
            // Find team name for user
            teamName = findTeamNameForUser(userName);
        }
        return new WeeklyReportResponse(
            r.getId(), r.getUserId(), userName, userDepartment, teamName,
            r.getWeekStartDate(), r.getWeekEndDate(),
            r.getDoneWork(), r.getPlanWork(),
            r.getProblems(), r.getSupportNeeded(),
            r.getStatus(), r.getReviewComment(),
            r.getSubmittedAt(), r.getReviewedAt(),
            null, false, r.getVersion(), r.getCreatedAt(), r.getUpdatedAt()
        );
    }

    private String findTeamNameForUser(String userName) {
        return TeamUtils.findTeamNameForUser(teamRepository.findAll(), userName);
    }

    private DepartmentReportResponse toResponse(DepartmentReport r) {
        String finalizedByName = "";
        if (r.getFinalizedBy() != null) {
            User u = userRepository.findById(r.getFinalizedBy()).orElse(null);
            if (u != null) finalizedByName = u.getName();
        }
        return new DepartmentReportResponse(
            r.getId(), r.getWeekStartDate(), r.getWeekEndDate(),
            r.getDepartment(), r.getMergedContent(), r.getEditedContent(),
            r.getStatus(), r.getSourceReportIds(),
            r.getSubmittedAt(), r.getFinalizedAt(),
            finalizedByName, r.getCreatedAt(), r.getUpdatedAt()
        );
    }
}
