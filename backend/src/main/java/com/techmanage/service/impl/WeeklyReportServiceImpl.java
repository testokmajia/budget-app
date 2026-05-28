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
        var existing = weeklyReportRepository.findByUserIdAndWeekStartDate(userId, monday);
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
        var existing = weeklyReportRepository.findByUserIdAndWeekStartDate(userId, request.weekStartDate());
        WeeklyReport report;
        if (existing.isPresent()) {
            report = existing.get();
            if (!"DRAFT".equals(report.getStatus()) && !"REJECTED".equals(report.getStatus())) {
                throw new RuntimeException("当前状态的周报不可编辑");
            }
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
        var existing = weeklyReportRepository.findByUserIdAndWeekStartDate(userId, request.weekStartDate());
        WeeklyReport report;
        if (existing.isPresent()) {
            report = existing.get();
            if (!"DRAFT".equals(report.getStatus()) && !"REJECTED".equals(report.getStatus())) {
                throw new RuntimeException("当前状态的周报不可提交");
            }
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
        report.setStatus("PENDING_REVIEW");
        report.setSubmittedAt(LocalDateTime.now());
        report.setReviewComment(null);
        report.setReviewerId(null);
        report.setReviewedAt(null);
        weeklyReportRepository.save(report);
        return toResponse(report);
    }

    @Override
    public SmartDraftResponse getSmartDraft(Long userId) {
        LocalDate lastMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
        var lastWeek = weeklyReportRepository.findByUserIdAndWeekStartDate(userId, lastMonday);
        if (lastWeek.isPresent() && lastWeek.get().getPlanWork() != null) {
            String planWork = lastWeek.get().getPlanWork();
            return new SmartDraftResponse(planWork, planWork, true);
        }
        return new SmartDraftResponse(null, null, false);
    }

    @Override
    public List<WeeklyReportResponse> listMyReports(Long userId, LocalDate from, LocalDate to) {
        return weeklyReportRepository.findByUserIdAndDateRange(userId, from, to).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    public List<WeeklyReportResponse> listPendingForLeader(Long leaderUserId) {
        List<Long> memberIds = resolveTeamMemberIds(leaderUserId);
        if (memberIds.isEmpty()) return List.of();
        return weeklyReportRepository.findPendingByUserIds(memberIds).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    public long countPendingForLeader(Long leaderUserId) {
        List<Long> memberIds = resolveTeamMemberIds(leaderUserId);
        if (memberIds.isEmpty()) return 0;
        return weeklyReportRepository.countPendingByUserIds(memberIds);
    }

    @Override
    public WeeklyReportResponse approve(Long reportId, Long leaderUserId, ReviewRequest request) {
        var report = weeklyReportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("周报不存在"));
        if (!"PENDING_REVIEW".equals(report.getStatus())) {
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
        if (!"PENDING_REVIEW".equals(report.getStatus())) {
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
        return weeklyReportRepository.findByDateRange(from, to).stream()
            .filter(r -> memberIds.contains(r.getUserId()))
            .map(this::toResponse)
            .toList();
    }

    @Override
    public List<WeeklyReportResponse> listAllHistory(LocalDate from, LocalDate to) {
        return weeklyReportRepository.findByDateRange(from, to).stream()
            .map(this::toResponse)
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
            r.getCreatedAt(), r.getUpdatedAt()
        );
    }
}
