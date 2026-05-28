package com.techmanage.controller;

import com.techmanage.common.ApiResponse;
import com.techmanage.dto.DashboardStats;
import com.techmanage.entity.IssueFeedback;
import com.techmanage.entity.RewardPunishment;
import com.techmanage.entity.Team;
import com.techmanage.entity.User;
import com.techmanage.repository.IssueFeedbackRepository;
import com.techmanage.repository.RewardPunishmentRepository;
import com.techmanage.repository.TeamRepository;
import com.techmanage.repository.UserRepository;
import com.techmanage.repository.WeeklyReportRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final List<String> STATUS_ORDER = Arrays.asList(
        "待分派", "待员工处理", "待组长审核", "待管理员审核", "待确认", "已完成", "已驳回", "已关闭"
    );

    private final IssueFeedbackRepository issueRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final RewardPunishmentRepository rewardRepository;
    private final WeeklyReportRepository weeklyReportRepository;

    public DashboardController(IssueFeedbackRepository issueRepository,
                               TeamRepository teamRepository,
                               UserRepository userRepository,
                               RewardPunishmentRepository rewardRepository,
                               WeeklyReportRepository weeklyReportRepository) {
        this.issueRepository = issueRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.rewardRepository = rewardRepository;
        this.weeklyReportRepository = weeklyReportRepository;
    }

    @GetMapping("/stats")
    public ApiResponse<DashboardStats> stats(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isIssueAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ISSUE_ADMIN"));

        List<IssueFeedback> allIssues = issueRepository.findAll();

        // Status distribution
        Map<String, Long> statusMap = allIssues.stream()
            .collect(Collectors.groupingBy(IssueFeedback::getStatus, Collectors.counting()));
        List<DashboardStats.StatusCount> statusCounts = STATUS_ORDER.stream()
            .map(s -> new DashboardStats.StatusCount(s, statusMap.getOrDefault(s, 0L)))
            .toList();

        // Team distribution
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

        // Pending tasks for current user
        List<DashboardStats.PendingTask> pendingTasks = new ArrayList<>();
        boolean canManage = isAdmin || isIssueAdmin;

        if (canManage) {
            long todoAssign = allIssues.stream().filter(i -> "待分派".equals(i.getStatus())).count();
            if (todoAssign > 0) {
                pendingTasks.add(new DashboardStats.PendingTask(
                    "待分派问题", "需要分配责任团队和责任人", todoAssign,
                    "Issue", "statuses=待分派"
                ));
            }
            long todoAdminReview = allIssues.stream().filter(i -> "待管理员审核".equals(i.getStatus())).count();
            if (todoAdminReview > 0) {
                pendingTasks.add(new DashboardStats.PendingTask(
                    "待管理员审核", "问题整改方案需要管理员审核", todoAdminReview,
                    "Issue", "statuses=待管理员审核"
                ));
            }
        }

        // IT employee pending tasks
        User currentUser = userRepository.findById(userId).orElse(null);
        String userName = currentUser != null ? currentUser.getName() : "";
        boolean isItEmployee = currentUser != null && "信息科技部".equals(currentUser.getDepartment());

        if (isItEmployee) {
            // Tasks assigned to me
            long myTasks = allIssues.stream()
                .filter(i -> "待员工处理".equals(i.getStatus()) && userId.equals(i.getResponsiblePersonId()))
                .count();
            if (myTasks > 0) {
                pendingTasks.add(new DashboardStats.PendingTask(
                    "待处理问题", "分配给您的整改任务", myTasks,
                    "Issue", "statuses=待员工处理"
                ));
            }
            // My rejected issues
            long myRejected = allIssues.stream()
                .filter(i -> "已驳回".equals(i.getStatus()) && userId.equals(i.getResponsiblePersonId()))
                .count();
            if (myRejected > 0) {
                pendingTasks.add(new DashboardStats.PendingTask(
                    "已驳回问题", "被退回需要重新整改", myRejected,
                    "Issue", "statuses=已驳回"
                ));
            }
            // Team leader review
            List<Team> ledTeams = teamRepository.findByLeader(userName);
            if (!ledTeams.isEmpty()) {
                Set<String> memberNames = ledTeams.stream()
                    .flatMap(t -> {
                        if (t.getMembers() == null || t.getMembers().isBlank()) return java.util.stream.Stream.empty();
                        return Arrays.stream(t.getMembers().split(",")).map(String::trim).filter(s -> !s.isEmpty());
                    })
                    .collect(Collectors.toSet());
                if (ledTeams.size() > 0 && currentUser.getName() != null) {
                    memberNames.add(currentUser.getName());
                }
                Set<Long> memberIds = userRepository.findByNameIn(new ArrayList<>(memberNames)).stream()
                    .map(User::getId).collect(Collectors.toSet());

                long leaderReview = allIssues.stream()
                    .filter(i -> "待组长审核".equals(i.getStatus())
                        && i.getResponsiblePersonId() != null
                        && memberIds.contains(i.getResponsiblePersonId()))
                    .count();
                if (leaderReview > 0) {
                    pendingTasks.add(new DashboardStats.PendingTask(
                        "待组长审核", "团队成员提交的方案需要审核", leaderReview,
                        "Issue", "statuses=待组长审核"
                    ));
                }
            }
        }

        // Issues pending confirmation (submitted by me)
        long myConfirm = allIssues.stream()
            .filter(i -> "待确认".equals(i.getStatus()) && userId.equals(i.getSubmitterId()))
            .count();
        if (myConfirm > 0) {
            pendingTasks.add(new DashboardStats.PendingTask(
                "待确认完成", "您提交的问题等待确认", myConfirm,
                "Issue", "statuses=待确认"
            ));
        }

        // Weekly report pending approvals (for team leaders)
        if (userName != null && !userName.isBlank()) {
            List<Team> ledTeams = teamRepository.findByLeader(userName);
            if (!ledTeams.isEmpty()) {
                Set<String> memberNames = ledTeams.stream()
                    .flatMap(t -> {
                        if (t.getMembers() == null || t.getMembers().isBlank()) return java.util.stream.Stream.empty();
                        return Arrays.stream(t.getMembers().split(",")).map(String::trim).filter(s -> !s.isEmpty());
                    })
                    .collect(Collectors.toSet());
                List<Long> memberIds = userRepository.findByNameIn(new ArrayList<>(memberNames)).stream()
                    .map(User::getId).collect(Collectors.toList());
                if (!memberIds.isEmpty()) {
                    long pendingReports = weeklyReportRepository.countSubmittedByUserIds(memberIds);
                    if (pendingReports > 0) {
                        pendingTasks.add(new DashboardStats.PendingTask(
                            "组内周报汇总", "团队成员提交的周报待汇总", pendingReports,
                            "Weekly", ""
                        ));
                    }
                }
            }
        }

        // Reward/punishment ranking (top 10 by total score)
        List<RewardPunishment> allRewards = rewardRepository.findAllActive();
        Map<String, Integer> scoreSum = allRewards.stream()
            .collect(Collectors.groupingBy(RewardPunishment::getInvolvedPerson,
                Collectors.summingInt(r -> r.getScore() != null ? r.getScore() : 0)));
        Map<String, String> personDept = allRewards.stream()
            .collect(Collectors.toMap(RewardPunishment::getInvolvedPerson,
                r -> r.getDepartment() != null ? r.getDepartment() : "",
                (d1, d2) -> d1));
        List<DashboardStats.RewardRanking> rewardRanking = scoreSum.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .map(e -> new DashboardStats.RewardRanking(e.getKey(),
                personDept.getOrDefault(e.getKey(), ""), e.getValue()))
            .toList();

        return ApiResponse.ok(new DashboardStats(
            statusCounts, teamDistribution,
            new DashboardStats.OverdueInfo(tempOverdue, permOverdue),
            personnel,
            pendingTasks,
            rewardRanking
        ));
    }
}
