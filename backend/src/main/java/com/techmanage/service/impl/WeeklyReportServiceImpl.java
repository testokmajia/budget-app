package com.techmanage.service.impl;

import com.techmanage.dto.ReviewRequest;
import com.techmanage.dto.SmartDraftResponse;
import com.techmanage.dto.WeeklyReportRequest;
import com.techmanage.dto.WeeklyReportResponse;
import com.techmanage.entity.Team;
import com.techmanage.entity.User;
import com.techmanage.entity.WeeklyReport;
import com.techmanage.repository.TeamRepository;
import com.techmanage.repository.UserRepository;
import com.techmanage.repository.WeeklyReportRepository;
import com.techmanage.service.WeeklyReportService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WeeklyReportServiceImpl implements WeeklyReportService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    public WeeklyReportServiceImpl(WeeklyReportRepository weeklyReportRepository,
                                   UserRepository userRepository,
                                   TeamRepository teamRepository) {
        this.weeklyReportRepository = weeklyReportRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
    }

    @Override
    public WeeklyReportResponse getCurrentWeek(Long userId) {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate friday = monday.plusDays(4);
        var existing = weeklyReportRepository.findLatestByUserIdAndWeekStartDate(userId, monday);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }
        var draft = new WeeklyReport();
        draft.setUserId(userId);
        draft.setWeekStartDate(monday);
        draft.setWeekEndDate(friday);
        return toResponse(draft);
    }

    @Override
    public WeeklyReportResponse save(Long userId, WeeklyReportRequest request) {
        var existing = weeklyReportRepository.findLatestByUserIdAndWeekStartDate(userId, request.weekStartDate());
        WeeklyReport report;
        if (existing.isPresent()) {
            report = existing.get();
            if ("DRAFT".equals(report.getStatus())) {
                report.setDoneWork(request.doneWork());
                report.setPlanWork(request.planWork());
                report.setProblems(request.problems());
                report.setSupportNeeded(request.supportNeeded());
                weeklyReportRepository.save(report);
                return toResponse(report);
            }
            report = new WeeklyReport();
            report.setUserId(userId);
            report.setWeekStartDate(request.weekStartDate());
            report.setWeekEndDate(request.weekEndDate());
            report.setVersion(existing.get().getVersion() + 1);
        } else {
            report = new WeeklyReport();
            report.setUserId(userId);
            report.setWeekStartDate(request.weekStartDate());
            report.setWeekEndDate(request.weekEndDate());
        }
        report.setDoneWork(request.doneWork());
        report.setPlanWork(request.planWork());
        report.setProblems(request.problems());
        report.setSupportNeeded(request.supportNeeded());
        report.setStatus("DRAFT");
        weeklyReportRepository.save(report);
        return toResponse(report);
    }

    @Override
    public WeeklyReportResponse submit(Long userId, WeeklyReportRequest request) {
        var existing = weeklyReportRepository.findLatestByUserIdAndWeekStartDate(userId, request.weekStartDate());
        WeeklyReport report;
        if (existing.isPresent()) {
            var prev = existing.get();
            if ("DRAFT".equals(prev.getStatus())) {
                prev.setDoneWork(request.doneWork());
                prev.setPlanWork(request.planWork());
                prev.setProblems(request.problems());
                prev.setSupportNeeded(request.supportNeeded());
                prev.setStatus("SUBMITTED");
                prev.setSubmittedAt(LocalDateTime.now());
                prev.setReviewComment(null);
                prev.setReviewerId(null);
                prev.setReviewedAt(null);
                weeklyReportRepository.save(prev);
                return toResponse(prev);
            }
            report = new WeeklyReport();
            report.setUserId(userId);
            report.setWeekStartDate(request.weekStartDate());
            report.setWeekEndDate(request.weekEndDate());
            report.setVersion(prev.getVersion() + 1);
        } else {
            report = new WeeklyReport();
            report.setUserId(userId);
            report.setWeekStartDate(request.weekStartDate());
            report.setWeekEndDate(request.weekEndDate());
        }
        report.setDoneWork(request.doneWork());
        report.setPlanWork(request.planWork());
        report.setProblems(request.problems());
        report.setSupportNeeded(request.supportNeeded());
        report.setStatus("SUBMITTED");
        report.setSubmittedAt(LocalDateTime.now());
        weeklyReportRepository.save(report);
        return toResponse(report);
    }

    @Override
    public SmartDraftResponse getSmartDraft(Long userId) {
        LocalDate lastMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
        var lastWeek = weeklyReportRepository.findLatestByUserIdAndWeekStartDate(userId, lastMonday);
        if (lastWeek.isPresent() && lastWeek.get().getPlanWork() != null) {
            String planWork = lastWeek.get().getPlanWork();
            return new SmartDraftResponse(planWork, planWork, true);
        }
        return new SmartDraftResponse(null, null, false);
    }

    @Override
    public List<WeeklyReportResponse> listMyReports(Long userId, LocalDate from, LocalDate to) {
        var all = weeklyReportRepository.findByUserIdAndDateRange(userId, from, to);
        return latestPerWeek(all).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    public List<WeeklyReportResponse> listPendingForLeader(Long leaderUserId) {
        List<Long> memberIds = resolveTeamMemberIds(leaderUserId);
        if (memberIds.isEmpty()) return List.of();
        var all = weeklyReportRepository.findSubmittedByUserIds(memberIds);
        return latestPerWeek(all).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    public long countPendingForLeader(Long leaderUserId) {
        List<Long> memberIds = resolveTeamMemberIds(leaderUserId);
        if (memberIds.isEmpty()) return 0;
        return weeklyReportRepository.countSubmittedByUserIds(memberIds);
    }

    @Override
    public WeeklyReportResponse approve(Long reportId, Long leaderUserId, ReviewRequest request) {
        var report = weeklyReportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("周报不存在"));
        if (!"SUBMITTED".equals(report.getStatus())) {
            throw new RuntimeException("当前状态不可审批");
        }
        List<Long> memberIds = resolveTeamMemberIds(leaderUserId);
        if (!memberIds.contains(report.getUserId())) {
            throw new RuntimeException("无权审批该周报");
        }
        report.setStatus("APPROVED");
        report.setReviewComment(request.comment());
        report.setReviewerId(leaderUserId);
        report.setReviewedAt(LocalDateTime.now());
        weeklyReportRepository.save(report);
        return toResponse(report);
    }

    @Override
    public WeeklyReportResponse reject(Long reportId, Long leaderUserId, ReviewRequest request) {
        var report = weeklyReportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("周报不存在"));
        if (!"SUBMITTED".equals(report.getStatus())) {
            throw new RuntimeException("当前状态不可审批");
        }
        List<Long> memberIds = resolveTeamMemberIds(leaderUserId);
        if (!memberIds.contains(report.getUserId())) {
            throw new RuntimeException("无权审批该周报");
        }
        if (request.comment() == null || request.comment().isBlank()) {
            throw new RuntimeException("驳回必须填写批注");
        }
        report.setStatus("REJECTED");
        report.setReviewComment(request.comment());
        report.setReviewerId(leaderUserId);
        report.setReviewedAt(LocalDateTime.now());
        weeklyReportRepository.save(report);
        return toResponse(report);
    }

    @Override
    public List<WeeklyReportResponse> listTeamHistory(Long leaderUserId, LocalDate from, LocalDate to) {
        List<Long> memberIds = resolveTeamMemberIds(leaderUserId);
        if (memberIds.isEmpty()) return List.of();
        var all = weeklyReportRepository.findByDateRange(from, to).stream()
            .filter(r -> memberIds.contains(r.getUserId()))
            .toList();
        return latestPerWeek(all).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    public List<WeeklyReportResponse> listAllHistory(LocalDate from, LocalDate to) {
        var all = weeklyReportRepository.findByDateRange(from, to);
        return latestPerWeek(all).stream()
            .map(this::toResponse)
            .toList();
    }

    private List<WeeklyReport> latestPerWeek(List<WeeklyReport> reports) {
        return reports.stream()
            .collect(Collectors.groupingBy(
                r -> r.getUserId() + "_" + r.getWeekStartDate(),
                Collectors.maxBy(Comparator.comparingInt(r -> r.getVersion() != null ? r.getVersion() : 1))
            ))
            .values().stream()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .sorted(Comparator.comparing(WeeklyReport::getWeekStartDate).reversed())
            .toList();
    }

    private List<Long> resolveTeamMemberIds(Long leaderUserId) {
        User leader = userRepository.findById(leaderUserId).orElse(null);
        if (leader == null) return List.of();
        List<Team> teams = teamRepository.findByLeader(leader.getName());
        if (teams.isEmpty()) return List.of();
        Set<String> memberNames = new HashSet<>();
        for (Team t : teams) {
            if (t.getMembers() != null && !t.getMembers().isBlank()) {
                Arrays.stream(t.getMembers().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(memberNames::add);
            }
        }
        if (memberNames.isEmpty()) return List.of();
        return userRepository.findByNameIn(new ArrayList<>(memberNames)).stream()
            .map(User::getId)
            .toList();
    }

    private WeeklyReportResponse toResponse(WeeklyReport r) {
        String userName = "";
        String userDepartment = "";
        String reviewerName = "";
        User user = userRepository.findById(r.getUserId()).orElse(null);
        if (user != null) {
            userName = user.getName();
            userDepartment = user.getDepartment() != null ? user.getDepartment() : "";
        }
        if (r.getReviewerId() != null) {
            User reviewer = userRepository.findById(r.getReviewerId()).orElse(null);
            if (reviewer != null) reviewerName = reviewer.getName();
        }
        return new WeeklyReportResponse(
            r.getId(), r.getUserId(), userName, userDepartment,
            r.getWeekStartDate(), r.getWeekEndDate(),
            r.getDoneWork(), r.getPlanWork(),
            r.getProblems(), r.getSupportNeeded(),
            r.getStatus(), r.getReviewComment(),
            r.getSubmittedAt(), r.getReviewedAt(),
            reviewerName, r.isMerged(),
            r.getVersion(),
            r.getCreatedAt(), r.getUpdatedAt()
        );
    }
}
