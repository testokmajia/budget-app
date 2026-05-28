package com.techmanage.service.impl;

import com.techmanage.dto.DepartmentReportRequest;
import com.techmanage.dto.DepartmentReportResponse;
import com.techmanage.dto.WeeklyReportResponse;
import com.techmanage.entity.DepartmentReport;
import com.techmanage.entity.User;
import com.techmanage.entity.WeeklyReport;
import com.techmanage.repository.DepartmentReportRepository;
import com.techmanage.repository.UserRepository;
import com.techmanage.repository.WeeklyReportRepository;
import com.techmanage.service.AiService;
import com.techmanage.service.DepartmentReportService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentReportServiceImpl implements DepartmentReportService {

    private final DepartmentReportRepository deptReportRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final UserRepository userRepository;
    private final AiService aiService;

    public DepartmentReportServiceImpl(DepartmentReportRepository deptReportRepository,
                                        WeeklyReportRepository weeklyReportRepository,
                                        UserRepository userRepository,
                                        AiService aiService) {
        this.deptReportRepository = deptReportRepository;
        this.weeklyReportRepository = weeklyReportRepository;
        this.userRepository = userRepository;
        this.aiService = aiService;
    }

    @Override
    public DepartmentReportResponse mergeAi(Long clerkUserId, DepartmentReportRequest request) {
        LocalDate weekStart = request.weekStartDate();
        List<WeeklyReport> approvedReports = weeklyReportRepository.findApprovedByWeek(weekStart);
        if (approvedReports.isEmpty()) {
            throw new RuntimeException("本周暂无已审批的周报");
        }

        List<WeeklyReportResponse> reportResponses = approvedReports.stream()
            .map(this::toWeeklyResponse)
            .toList();

        String aiResult = aiService.mergeReports(reportResponses);

        var existing = deptReportRepository.findByDepartmentAndWeekStartDate("信息科技部", weekStart);
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
            report.setWeekStartDate(weekStart);
            report.setWeekEndDate(request.weekEndDate());
            report.setMergedContent(aiResult);
        }
        report.setSourceReportIds(approvedReports.stream()
            .map(r -> String.valueOf(r.getId()))
            .collect(Collectors.joining(",")));

        // Mark reports as merged
        for (var r : approvedReports) {
            r.setMerged(true);
            weeklyReportRepository.save(r);
        }

        deptReportRepository.save(report);
        return toResponse(report);
    }

    @Override
    public DepartmentReportResponse getById(Long id) {
        var report = deptReportRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("部门周报不存在"));
        return toResponse(report);
    }

    @Override
    public DepartmentReportResponse update(Long id, Long clerkUserId, String editedContent) {
        var report = deptReportRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("部门周报不存在"));
        if (!"DRAFT".equals(report.getStatus())) {
            throw new RuntimeException("当前状态不可编辑");
        }
        report.setEditedContent(editedContent);
        deptReportRepository.save(report);
        return toResponse(report);
    }

    @Override
    public DepartmentReportResponse submit(Long id, Long clerkUserId) {
        var report = deptReportRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("部门周报不存在"));
        if (!"DRAFT".equals(report.getStatus())) {
            throw new RuntimeException("当前状态不可提交");
        }
        report.setStatus("PENDING_REVIEW");
        report.setSubmittedAt(LocalDateTime.now());
        deptReportRepository.save(report);
        return toResponse(report);
    }

    @Override
    public DepartmentReportResponse finalize(Long id, Long headUserId) {
        var report = deptReportRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("部门周报不存在"));
        if (!"PENDING_REVIEW".equals(report.getStatus())) {
            throw new RuntimeException("当前状态不可审定");
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
            .orElseThrow(() -> new RuntimeException("部门周报不存在"));
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
            throw new RuntimeException("导出失败", e);
        }
    }

    @Override
    public String exportHtml(Long id) {
        var report = deptReportRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("部门周报不存在"));
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
        User user = userRepository.findById(r.getUserId()).orElse(null);
        if (user != null) {
            userName = user.getName();
            userDepartment = user.getDepartment() != null ? user.getDepartment() : "";
        }
        return new WeeklyReportResponse(
            r.getId(), r.getUserId(), userName, userDepartment,
            r.getWeekStartDate(), r.getWeekEndDate(),
            r.getDoneWork(), r.getPlanWork(),
            r.getProblems(), r.getSupportNeeded(),
            r.getStatus(), r.getReviewComment(),
            r.getSubmittedAt(), r.getReviewedAt(),
            null, false, r.getCreatedAt(), r.getUpdatedAt()
        );
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
