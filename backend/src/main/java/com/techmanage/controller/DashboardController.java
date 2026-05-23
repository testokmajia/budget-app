package com.techmanage.controller;

import com.techmanage.common.ApiResponse;
import com.techmanage.dto.DashboardStats;
import com.techmanage.entity.IssueFeedback;
import com.techmanage.entity.Team;
import com.techmanage.repository.IssueFeedbackRepository;
import com.techmanage.repository.TeamRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final List<String> STATUS_ORDER = Arrays.asList(
        "待分派", "已分派", "整改中", "待确认", "已完成", "已驳回", "已关闭"
    );

    private final IssueFeedbackRepository issueRepository;
    private final TeamRepository teamRepository;

    public DashboardController(IssueFeedbackRepository issueRepository, TeamRepository teamRepository) {
        this.issueRepository = issueRepository;
        this.teamRepository = teamRepository;
    }

    @GetMapping("/stats")
    public ApiResponse<DashboardStats> stats() {
        List<IssueFeedback> allIssues = issueRepository.findAll();

        // Status distribution
        Map<String, Long> statusMap = allIssues.stream()
            .collect(Collectors.groupingBy(IssueFeedback::getStatus, Collectors.counting()));
        List<DashboardStats.StatusCount> statusCounts = STATUS_ORDER.stream()
            .map(s -> new DashboardStats.StatusCount(s, statusMap.getOrDefault(s, 0L)))
            .toList();

        // Team distribution (only assigned issues, grouped by responsibleTeam)
        Map<String, Long> teamMap = allIssues.stream()
            .filter(i -> i.getResponsibleTeam() != null && !i.getResponsibleTeam().isBlank())
            .collect(Collectors.groupingBy(IssueFeedback::getResponsibleTeam, Collectors.counting()));
        List<DashboardStats.TeamCount> teamDistribution = teamMap.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .map(e -> new DashboardStats.TeamCount(e.getKey(), e.getValue()))
            .toList();

        // Overdue
        LocalDate today = LocalDate.now();
        long tempOverdue = allIssues.stream()
            .filter(i -> i.getTemporaryDeadline() != null
                && i.getTemporaryDeadline().isBefore(today)
                && !"已完成".equals(i.getStatus())
                && !"已关闭".equals(i.getStatus()))
            .count();
        long permOverdue = allIssues.stream()
            .filter(i -> i.getPermanentDeadline() != null
                && i.getPermanentDeadline().isBefore(today)
                && !"已完成".equals(i.getStatus())
                && !"已关闭".equals(i.getStatus()))
            .count();

        // Personnel distribution by team
        List<Team> teams = teamRepository.findAllByOrderByIdAsc();
        List<DashboardStats.TeamPersonnel> personnel = teams.stream()
            .filter(Team::isEnabled)
            .map(t -> {
                long count = t.getMembers() != null && !t.getMembers().isBlank()
                    ? Arrays.stream(t.getMembers().split(",")).filter(s -> !s.trim().isEmpty()).count()
                    : 0;
                return new DashboardStats.TeamPersonnel(t.getName(), count);
            })
            .filter(tp -> tp.memberCount() > 0)
            .toList();

        return ApiResponse.ok(new DashboardStats(
            statusCounts, teamDistribution,
            new DashboardStats.OverdueInfo(tempOverdue, permOverdue),
            personnel
        ));
    }
}
