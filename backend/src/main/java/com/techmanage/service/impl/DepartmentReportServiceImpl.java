package com.techmanage.service.impl;

import com.techmanage.common.BusinessException;
import com.techmanage.dto.DepartmentReportRequest;
import com.techmanage.dto.DepartmentReportResponse;
import com.techmanage.dto.WeeklyReportResponse;
import com.techmanage.entity.DepartmentReport;
import com.techmanage.entity.TeamSummary;
import com.techmanage.entity.User;
import com.techmanage.entity.WeeklyReport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import java.time.DayOfWeek;
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
    private final ObjectMapper objectMapper;

    private static final String EMPTY_TEMPLATE = """
        {"overview":"","keyProgress":"","commonIssues":"","nextWeekPlans":"","coordinationItems":""}""";

    public DepartmentReportServiceImpl(DepartmentReportRepository deptReportRepository,
                                        WeeklyReportRepository weeklyReportRepository,
                                        UserRepository userRepository,
                                        TeamRepository teamRepository,
                                        TeamSummaryRepository teamSummaryRepository,
                                        AiService aiService,
                                        ObjectMapper objectMapper) {
        this.deptReportRepository = deptReportRepository;
        this.weeklyReportRepository = weeklyReportRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.teamSummaryRepository = teamSummaryRepository;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
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

        String aiResult;
        StringBuilder sourceIds = new StringBuilder();

        if (submittedSummaries.isEmpty() && fallbackReports.isEmpty()) {
            // 无任何本周数据：尝试基于上周部门周报的下周计划生成
            LocalDate lastMonday = requestedWeek.minusDays(7);
            var lastReport = deptReportRepository.findByDepartmentAndWeekStartDate(
                "信息科技部", lastMonday);

            String lastWeekContent = lastReport
                .map(r -> r.getEditedContent() != null ? r.getEditedContent() : r.getMergedContent())
                .orElse(null);
            String lastWeekPlans = extractNextWeekPlans(lastWeekContent);

            if (lastWeekPlans != null) {
                // 用上周计划构造合成报告，AI 生成本周部门周报
                WeeklyReportResponse synthetic = new WeeklyReportResponse(
                    null, null, "上周部门计划", "信息科技部", null,
                    targetWeek, weekEnd,
                    lastWeekPlans, "", "", "", "APPROVED",
                    null, null, null, null, false, 1, null, null
                );
                aiResult = aiService.mergeReports(List.of(synthetic));
                // 清空本周的下周计划（不能从历史周继承）
                aiResult = clearNextWeekPlans(aiResult);
                sourceIds.append("last_week_plan");
            } else {
                // 上周也无记录或计划为空，生成空模板
                aiResult = EMPTY_TEMPLATE;
                sourceIds.append("empty");
            }
        } else if (!submittedSummaries.isEmpty()) {
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

    /**
     * 从周报 JSON 内容中提取下周计划字段（nextWeekPlans）。
     * @return 非空计划字符串，如果不存在或解析失败则返回 null
     */
    private String extractNextWeekPlans(String jsonContent) {
        if (jsonContent == null || jsonContent.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(jsonContent);
            String plans = node.path("nextWeekPlans").asText(null);
            return (plans != null && !plans.isBlank()) ? plans : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将 AI 返回的 JSON 中的 nextWeekPlans 字段清空，
     * 确保本周周报不会从历史周继承下周计划。
     */
    private String clearNextWeekPlans(String aiJson) {
        try {
            JsonNode node = objectMapper.readTree(aiJson);
            if (node.isObject()) {
                ((ObjectNode) node).put("nextWeekPlans", "");
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return aiJson;
        }
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
