package com.techmanage.service.impl;

import com.techmanage.common.BusinessException;
import com.techmanage.dto.TeamInfo;
import com.techmanage.dto.TeamSummaryRequest;
import com.techmanage.dto.TeamSummaryResponse;
import com.techmanage.dto.WeeklyReportResponse;
import com.techmanage.entity.Team;
import com.techmanage.entity.TeamSummary;
import com.techmanage.entity.User;
import com.techmanage.entity.WeeklyReport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.techmanage.repository.TeamRepository;
import com.techmanage.repository.TeamSummaryRepository;
import com.techmanage.repository.UserRepository;
import com.techmanage.repository.WeeklyReportRepository;
import com.techmanage.service.AiService;
import com.techmanage.service.TeamSummaryService;
import com.techmanage.util.TeamUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeamSummaryServiceImpl implements TeamSummaryService {

    private final TeamSummaryRepository teamSummaryRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    private static final String EMPTY_TEMPLATE = """
        {"overview":"","keyProgress":"","commonIssues":"","nextWeekPlans":"","coordinationItems":""}""";

    public TeamSummaryServiceImpl(TeamSummaryRepository teamSummaryRepository,
                                   WeeklyReportRepository weeklyReportRepository,
                                   UserRepository userRepository,
                                   TeamRepository teamRepository,
                                   AiService aiService,
                                   ObjectMapper objectMapper) {
        this.teamSummaryRepository = teamSummaryRepository;
        this.weeklyReportRepository = weeklyReportRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    @Override
    public TeamSummaryResponse mergeAi(Long leaderId, TeamSummaryRequest request) {
        User leader = userRepository.findById(leaderId)
            .orElseThrow(() -> new BusinessException("用户不存在"));

        List<Team> teams = teamRepository.findByLeader(leader.getName());
        if (teams.isEmpty()) {
            throw new BusinessException("您不是组长，无法汇总组内周报");
        }

        // Collect member IDs from ALL teams the leader manages
        Set<String> memberNames = new HashSet<>();
        for (Team t : teams) {
            if (t.getMembers() != null && !t.getMembers().isBlank()) {
                Arrays.stream(t.getMembers().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(memberNames::add);
            }
        }

        if (memberNames.isEmpty()) {
            throw new BusinessException("您的团队暂无成员，请先在团队管理中配置");
        }

        // 提前确定目标周和团队名称（在检查报告之前），确保回退逻辑可用
        final LocalDate targetWeek;
        if (request.weekStartDate() != null) {
            targetWeek = request.weekStartDate();
        } else {
            targetWeek = LocalDate.now().with(DayOfWeek.MONDAY);
        }

        final String teamName;
        if (request.teamName() != null && !request.teamName().isBlank()) {
            teamName = teams.stream()
                .filter(t -> trim(t.getName()).equals(trim(request.teamName())))
                .findFirst()
                .orElse(teams.get(0))
                .getName();
        } else {
            teamName = teams.get(0).getName();
        }

        List<Long> memberIds = userRepository.findByNameIn(new ArrayList<>(memberNames)).stream()
            .map(User::getId)
            .toList();

        if (memberIds.isEmpty()) {
            throw new BusinessException("团队成员账号未找到，请检查团队管理中的成员姓名");
        }

        // Get all submitted reports from team members
        List<WeeklyReport> allSubmitted = weeklyReportRepository
            .findSubmittedOrApprovedByUserIds(memberIds);

        // 筛选目标周的报告
        List<WeeklyReport> reports = allSubmitted.stream()
            .filter(r -> targetWeek.equals(r.getWeekStartDate()))
            .collect(Collectors.toList());

        // Keep only latest version per user
        reports = reports.stream()
            .collect(Collectors.groupingBy(
                r -> r.getUserId() + "_" + r.getWeekStartDate(),
                Collectors.maxBy(Comparator.comparingInt(r -> r.getVersion() != null ? r.getVersion() : 1))
            ))
            .values().stream()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();

        String aiResult;
        List<WeeklyReport> sourceReports;

        if (!reports.isEmpty()) {
            // 有成员周报：使用现有 AI 合并逻辑
            List<WeeklyReportResponse> reportResponses = reports.stream()
                .map(this::toWeeklyResponse)
                .toList();
            aiResult = aiService.mergeReports(reportResponses);
            sourceReports = reports;
        } else {
            // 无成员周报：尝试基于上周团队周报的下周计划生成
            LocalDate lastMonday = targetWeek.minusDays(7);
            var lastSummary = teamSummaryRepository.findByTeamNameAndWeekStartDate(teamName, lastMonday);

            String lastWeekContent = lastSummary
                .map(ts -> ts.getEditedContent() != null ? ts.getEditedContent() : ts.getMergedContent())
                .orElse(null);
            String lastWeekPlans = extractNextWeekPlans(lastWeekContent);

            if (lastWeekPlans != null) {
                // 用上周计划构造合成报告，AI 生成本周周报
                WeeklyReportResponse synthetic = new WeeklyReportResponse(
                    null, null, "上周计划", "信息科技部", teamName,
                    targetWeek, targetWeek.plusDays(4),
                    lastWeekPlans, "", "", "", "APPROVED",
                    null, null, null, null, false, 1, null, null
                );
                aiResult = aiService.mergeReports(List.of(synthetic));
                // 清空本周的下周计划（不能从历史周继承）
                aiResult = clearNextWeekPlans(aiResult);
            } else {
                // 上周也无记录或计划为空，生成空模板，供组长手动编辑
                aiResult = EMPTY_TEMPLATE;
            }
            sourceReports = List.of();
        }

        var existing = teamSummaryRepository.findByTeamNameAndWeekStartDate(teamName, targetWeek);
        TeamSummary summary;
        if (existing.isPresent()) {
            summary = existing.get();
            summary.setMergedContent(aiResult);
            summary.setEditedContent(null);
            summary.setStatus("DRAFT");
            summary.setSubmittedAt(null);
        } else {
            summary = new TeamSummary();
            summary.setTeamName(teamName);
            summary.setLeaderId(leaderId);
            summary.setWeekStartDate(targetWeek);
            summary.setWeekEndDate(targetWeek.plusDays(4));
            summary.setMergedContent(aiResult);
        }
        summary.setSourceReportIds(sourceReports.stream()
            .map(r -> String.valueOf(r.getId()))
            .collect(Collectors.joining(",")));

        teamSummaryRepository.save(summary);
        return toResponse(summary);
    }

    @Override
    public TeamSummaryResponse update(Long id, Long leaderId, String editedContent) {
        var summary = teamSummaryRepository.findById(id)
            .orElseThrow(() -> new BusinessException("组内汇总不存在"));
        if (!summary.getLeaderId().equals(leaderId)) {
            throw new BusinessException("只能编辑自己组的汇总");
        }
        if (!"DRAFT".equals(summary.getStatus())) {
            throw new BusinessException("当前状态不可编辑");
        }
        summary.setEditedContent(editedContent);
        teamSummaryRepository.save(summary);
        return toResponse(summary);
    }

    @Override
    @Transactional
    public TeamSummaryResponse submit(Long id, Long leaderId) {
        var summary = teamSummaryRepository.findById(id)
            .orElseThrow(() -> new BusinessException("组内汇总不存在"));
        if (!summary.getLeaderId().equals(leaderId)) {
            throw new BusinessException("只能提交自己组的汇总");
        }
        if (!"DRAFT".equals(summary.getStatus())) {
            throw new BusinessException("当前状态不可提交");
        }
        summary.setStatus("SUBMITTED");
        summary.setSubmittedAt(LocalDateTime.now());
        teamSummaryRepository.save(summary);

        // Auto-approve all member reports for this week
        autoApproveMemberReports(summary);

        return toResponse(summary);
    }

    /**
     * When team leader submits summary, auto-approve all member weekly reports
     * for this week - bypassing individual approval step.
     */
    private void autoApproveMemberReports(TeamSummary summary) {
        User leader = userRepository.findById(summary.getLeaderId()).orElse(null);
        if (leader == null) return;

        List<Team> teams = teamRepository.findByLeader(leader.getName());
        Set<String> memberNames = new HashSet<>();
        for (Team t : teams) {
            if (t.getMembers() != null && !t.getMembers().isBlank()) {
                Arrays.stream(t.getMembers().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(memberNames::add);
            }
        }

        List<Long> memberIds = userRepository.findByNameIn(new ArrayList<>(memberNames)).stream()
            .map(User::getId)
            .toList();

        for (Long memberId : memberIds) {
            var report = weeklyReportRepository.findLatestByUserIdAndWeekStartDate(
                memberId, summary.getWeekStartDate());
            if (report.isPresent() && "SUBMITTED".equals(report.get().getStatus())) {
                var r = report.get();
                r.setStatus("APPROVED");
                r.setReviewerId(leader.getId());
                r.setReviewedAt(LocalDateTime.now());
                r.setReviewComment("组长已提交组内汇总，自动审批通过");
                weeklyReportRepository.save(r);
            }
        }
    }

    @Override
    public TeamSummaryResponse getById(Long id) {
        var summary = teamSummaryRepository.findById(id)
            .orElseThrow(() -> new BusinessException("组内汇总不存在"));
        return toResponse(summary);
    }

    @Override
    public List<TeamSummaryResponse> listByLeader(Long leaderId) {
        return teamSummaryRepository.findByLeaderIdOrderByWeekStartDateDesc(leaderId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    public List<TeamSummaryResponse> listByWeek(LocalDate weekStartDate) {
        return teamSummaryRepository.findByWeekStartDate(weekStartDate).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    public List<TeamInfo> getMyTeams(Long leaderId) {
        User leader = userRepository.findById(leaderId).orElse(null);
        if (leader == null) return List.of();
        List<Team> teams = teamRepository.findByLeader(leader.getName());
        return teams.stream()
            .map(t -> {
                List<String> members = new ArrayList<>();
                if (t.getMembers() != null && !t.getMembers().isBlank()) {
                    Arrays.stream(t.getMembers().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .forEach(members::add);
                }
                return new TeamInfo(t.getName(), members);
            })
            .toList();
    }

    private WeeklyReportResponse toWeeklyResponse(WeeklyReport r) {
        String userName = "";
        String userDepartment = "";
        String teamName = null;
        User user = userRepository.findById(r.getUserId()).orElse(null);
        if (user != null) {
            userName = user.getName();
            userDepartment = user.getDepartment() != null ? user.getDepartment() : "";
            teamName = findTeamNameForUser(userName);
        }
        return new WeeklyReportResponse(
            r.getId(), r.getUserId(), userName, userDepartment, teamName,
            r.getWeekStartDate(), r.getWeekEndDate(),
            r.getDoneWork(), r.getPlanWork(),
            r.getProblems(), r.getSupportNeeded(),
            r.getStatus(), r.getReviewComment(),
            r.getSubmittedAt(), r.getReviewedAt(),
            null, false, r.getVersion(),
            r.getCreatedAt(), r.getUpdatedAt()
        );
    }

    private String findTeamNameForUser(String userName) {
        return TeamUtils.findTeamNameForUser(teamRepository.findAll(), userName);
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
            return aiJson; // 解析失败则返回原文
        }
    }

    private TeamSummaryResponse toResponse(TeamSummary s) {
        String leaderName = "";
        User leader = userRepository.findById(s.getLeaderId()).orElse(null);
        if (leader != null) leaderName = leader.getName();
        return new TeamSummaryResponse(
            s.getId(), s.getTeamName(), s.getLeaderId(), leaderName,
            s.getWeekStartDate(), s.getWeekEndDate(),
            s.getMergedContent(), s.getEditedContent(),
            s.getStatus(), s.getSourceReportIds(),
            s.getSubmittedAt(), s.getCreatedAt(), s.getUpdatedAt()
        );
    }

    /** 安全的 trim，null 安全 */
    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
