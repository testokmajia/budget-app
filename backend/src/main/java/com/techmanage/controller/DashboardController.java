package com.techmanage.controller;

import com.techmanage.common.ApiResponse;
import com.techmanage.dto.DashboardStats;
import com.techmanage.entity.IssueFeedback;
import com.techmanage.entity.RewardPunishment;
import com.techmanage.entity.Team;
import com.techmanage.entity.User;
import com.techmanage.repository.IssueFeedbackRepository;
import com.techmanage.repository.RequirementRepository;
import com.techmanage.repository.RewardPunishmentRepository;
import com.techmanage.repository.TeamRepository;
import com.techmanage.repository.TestReportRepository;
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
        "待分派", "待员工处理", "待组长审核", "待管理员审核", "解决中", "待确认", "已完成", "已驳回", "已关闭"
    );

    private final IssueFeedbackRepository issueRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final RewardPunishmentRepository rewardRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final RequirementRepository requirementRepository;
    private final TestReportRepository testReportRepository;

    public DashboardController(IssueFeedbackRepository issueRepository,
                               TeamRepository teamRepository,
                               UserRepository userRepository,
                               RewardPunishmentRepository rewardRepository,
                               WeeklyReportRepository weeklyReportRepository,
                               RequirementRepository requirementRepository,
                               TestReportRepository testReportRepository) {
        this.issueRepository = issueRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.rewardRepository = rewardRepository;
        this.weeklyReportRepository = weeklyReportRepository;
        this.requirementRepository = requirementRepository;
        this.testReportRepository = testReportRepository;
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
        boolean isItEmployee = currentUser != null && "信息科技部".equals(trim(currentUser.getDepartment()));

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

        // Reward ranking (top 10 by count)
        List<RewardPunishment> allRewards = rewardRepository.findAllActive();
        List<RewardPunishment> rewardList = allRewards.stream()
            .filter(r -> "奖励".equals(r.getType()))
            .toList();
        Map<String, Long> rewardCountMap = rewardList.stream()
            .collect(Collectors.groupingBy(RewardPunishment::getInvolvedPerson,
                Collectors.counting()));
        Map<String, String> rewardPersonDept = rewardList.stream()
            .collect(Collectors.toMap(RewardPunishment::getInvolvedPerson,
                r -> r.getDepartment() != null ? r.getDepartment() : "",
                (d1, d2) -> d1));
        List<DashboardStats.RewardRanking> rewardRanking = rewardCountMap.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .map(e -> new DashboardStats.RewardRanking(e.getKey(),
                rewardPersonDept.getOrDefault(e.getKey(), ""), e.getValue().intValue()))
            .toList();

        // Punishment ranking (top 10 by count)
        List<RewardPunishment> punishmentList = allRewards.stream()
            .filter(r -> "惩罚".equals(r.getType()))
            .toList();
        Map<String, Long> punishmentCountMap = punishmentList.stream()
            .collect(Collectors.groupingBy(RewardPunishment::getInvolvedPerson,
                Collectors.counting()));
        Map<String, String> punishmentPersonDept = punishmentList.stream()
            .collect(Collectors.toMap(RewardPunishment::getInvolvedPerson,
                r -> r.getDepartment() != null ? r.getDepartment() : "",
                (d1, d2) -> d1));
        List<DashboardStats.RewardRanking> punishmentRanking = punishmentCountMap.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .map(e -> new DashboardStats.RewardRanking(e.getKey(),
                punishmentPersonDept.getOrDefault(e.getKey(), ""), e.getValue().intValue()))
            .toList();

        // Requirement stats
        long reqTotal = requirementRepository.count();
        long reqInApproval = requirementRepository.countByStatus("审批中");
        long reqConfirmed = requirementRepository.countByStatus("需求已确认");
        long reqInProgress = requirementRepository.countByStatus("实施中");
        long reqTestPassed = requirementRepository.countByStatus("测试通过");
        long reqProduction = requirementRepository.countByStatus("已投产");
        long reqClosed = requirementRepository.countByStatus("已关闭");
        long reqRejected = requirementRepository.countByStatus("已驳回");
        var requirementStats = new DashboardStats.RequirementStats(
            reqTotal, reqInApproval, reqConfirmed, reqInProgress,
            reqTestPassed, reqProduction, reqClosed, reqRejected);

        // 需求审批待办
        if (userName != null && !userName.isBlank()) {
            List<com.techmanage.entity.Requirement> reqs = requirementRepository.findAll();
            String userDept = currentUser != null ? currentUser.getDepartment() : "";

            // 架构管理岗：有待评估需求
            boolean isArchitect = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ARCHITECT"));
            if (isArchitect) {
                long architectPending = reqs.stream()
                    .filter(r -> "审批中".equals(r.getStatus())
                        && "架构管理岗".equals(r.getCurrentNode()))
                    .count();
                if (architectPending > 0) {
                    pendingTasks.add(new DashboardStats.PendingTask(
                        "需求待评估", "有待评估涉及系统的需求", architectPending,
                        "RequirementList", "status=审批中&currentNode=架构管理岗"
                    ));
                }
            }

            // 部门负责人：有待审批需求（当前节点=部门负责人，且需求部门与用户所在部门匹配）
            if (userDept != null && !userDept.isBlank()) {
                long deptLeaderPending = reqs.stream()
                    .filter(r -> "审批中".equals(r.getStatus())
                        && "部门负责人".equals(r.getCurrentNode())
                        && userDept.equals(trim(r.getDept())))
                    .count();
                if (deptLeaderPending > 0) {
                    pendingTasks.add(new DashboardStats.PendingTask(
                        "需求待部门审批", "作为部门负责人，有待审批的需求", deptLeaderPending,
                        "RequirementList", "status=审批中&currentNode=部门负责人"
                    ));
                }
            }

            // 团队组长：有待指派PM/PD的需求
            List<Team> ledTeams = teamRepository.findByLeader(userName);
            if (!ledTeams.isEmpty()) {
                Set<String> teamNames = ledTeams.stream().map(Team::getName).collect(Collectors.toSet());
                long teamLeadPending = reqs.stream()
                    .filter(r -> "审批中".equals(r.getStatus())
                        && "团队组长".equals(r.getCurrentNode()))
                    .filter(r -> {
                        // 检查需求的涉及系统是否在当前用户负责的团队中
                        try {
                            if (r.getSystemItems() != null) {
                                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                                var items = om.readValue(r.getSystemItems(), new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                                return items.stream().anyMatch(item -> teamNames.contains(trim((String) item.get("team"))));
                            }
                        } catch (Exception e) { /* ignore */ }
                        return false;
                    })
                    .count();
                if (teamLeadPending > 0) {
                    pendingTasks.add(new DashboardStats.PendingTask(
                        "需求待指派", "有待指派项目经理和产品经理", teamLeadPending,
                        "RequirementList", "status=审批中&currentNode=团队组长"
                    ));
                }
            }

            // 产品经理：有待处理需求（当前节点=产品经理，且当前用户在assignedPd中）
            long pdPending = reqs.stream()
                .filter(r -> "审批中".equals(r.getStatus())
                    && "产品经理".equals(r.getCurrentNode())
                    && r.getAssignedPd() != null && containsName(r.getAssignedPd(), userName))
                .count();
            if (pdPending > 0) {
                pendingTasks.add(new DashboardStats.PendingTask(
                    "需求待提交评审", "您被指定为产品经理，待上传需求说明书", pdPending,
                    "RequirementList", "status=审批中&currentNode=产品经理"
                ));
            }

            // 多方确认：当前用户在确认列表中且未确认
            long confirmPending = reqs.stream()
                .filter(r -> "审批中".equals(r.getStatus())
                    && "多方确认".equals(r.getCurrentNode())
                    && r.getSpecReviewers() != null)
                .filter(r -> {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                        var reviewers = om.readValue(r.getSpecReviewers(),
                            new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                        return reviewers.stream().anyMatch(rv ->
                            trim(userName).equals(trim((String) rv.get("name"))) && !Boolean.TRUE.equals(rv.get("confirmed")));
                    } catch (Exception e) { return false; }
                })
                .count();
            if (confirmPending > 0) {
                pendingTasks.add(new DashboardStats.PendingTask(
                    "需求说明书待确认", "您需要确认需求说明书", confirmPending,
                    "RequirementList", "status=审批中&currentNode=多方确认"
                ));
            }

            // 项目经理：有待启动实施的需求
            long pmImplPending = reqs.stream()
                .filter(r -> "需求已确认".equals(r.getStatus())
                    && r.getAssignedPm() != null && containsName(r.getAssignedPm(), userName))
                .count();
            if (pmImplPending > 0) {
                pendingTasks.add(new DashboardStats.PendingTask(
                    "需求待启动实施", "您是项目经理，待启动实施", pmImplPending,
                    "RequirementList", "status=需求已确认"
                ));
            }

            // ========== 测试报告待办 ==========
            List<com.techmanage.entity.TestReport> reports = testReportRepository.findAll();
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();

            // 测试报告部门审核待办（待部门审核状态，当前用户在部门审核人列表中）
            long trDeptPending = reports.stream()
                .filter(r -> "待部门审核".equals(r.getStatus()) && r.getDeptReviewers() != null)
                .filter(r -> {
                    try {
                        var deptReviewers = om.readValue(r.getDeptReviewers(),
                            new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
                        return deptReviewers.stream().anyMatch(name -> trim(name).equals(trim(userName)));
                    } catch (Exception e) { return false; }
                })
                .count();
            if (trDeptPending > 0) {
                pendingTasks.add(new DashboardStats.PendingTask(
                    "测试报告待部门审核", "您有测试报告需要部门审核", trDeptPending,
                    "TestReportList", "status=待部门审核"
                ));
            }
        }

        return ApiResponse.ok(new DashboardStats(
            statusCounts, teamDistribution,
            new DashboardStats.OverdueInfo(tempOverdue, permOverdue),
            personnel,
            pendingTasks,
            rewardRanking,
            punishmentRanking,
            requirementStats
        ));
    }

    /** 安全的 trim，null 安全 */
    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    /** 逗号/顿号分隔的名称列表中是否包含指定名称（trim 后比较） */
    private static boolean containsName(String nameList, String targetName) {
        if (nameList == null || targetName == null) return false;
        String target = targetName.trim();
        return java.util.Arrays.stream(nameList.split("[、,，]"))
                .map(String::trim)
                .anyMatch(n -> n.equals(target));
    }
}
