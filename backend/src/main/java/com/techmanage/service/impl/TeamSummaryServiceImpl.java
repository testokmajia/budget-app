package com.techmanage.service.impl;

import com.techmanage.dto.TeamInfo;
import com.techmanage.dto.TeamSummaryRequest;
import com.techmanage.dto.TeamSummaryResponse;
import com.techmanage.dto.WeeklyReportResponse;
import com.techmanage.entity.Team;
import com.techmanage.entity.TeamSummary;
import com.techmanage.entity.User;
import com.techmanage.entity.WeeklyReport;
import com.techmanage.repository.TeamRepository;
import com.techmanage.repository.TeamSummaryRepository;
import com.techmanage.repository.UserRepository;
import com.techmanage.repository.WeeklyReportRepository;
import com.techmanage.service.AiService;
import com.techmanage.service.TeamSummaryService;
import org.springframework.stereotype.Service;

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

    public TeamSummaryServiceImpl(TeamSummaryRepository teamSummaryRepository,
                                   WeeklyReportRepository weeklyReportRepository,
                                   UserRepository userRepository,
                                   TeamRepository teamRepository,
                                   AiService aiService) {
        this.teamSummaryRepository = teamSummaryRepository;
        this.weeklyReportRepository = weeklyReportRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.aiService = aiService;
    }

    @Override
    public TeamSummaryResponse mergeAi(Long leaderId, TeamSummaryRequest request) {
        User leader = userRepository.findById(leaderId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));

        List<Team> teams = teamRepository.findByLeader(leader.getName());
        if (teams.isEmpty()) {
            throw new RuntimeException("您不是组长，无法汇总组内周报");
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
            throw new RuntimeException("您的团队暂无成员，请先在团队管理中配置");
        }

        List<Long> memberIds = userRepository.findByNameIn(new ArrayList<>(memberNames)).stream()
            .map(User::getId)
            .toList();

        if (memberIds.isEmpty()) {
            throw new RuntimeException("团队成员账号未找到，请检查团队管理中的成员姓名");
        }

        // Get all submitted reports from team members
        List<WeeklyReport> allSubmitted = weeklyReportRepository
            .findSubmittedByUserIds(memberIds);

        if (allSubmitted.isEmpty()) {
            throw new RuntimeException("组内暂无已提交的周报");
        }

        // Find the latest week that has submitted reports
        LocalDate latestWeek = allSubmitted.stream()
            .map(WeeklyReport::getWeekStartDate)
            .max(LocalDate::compareTo)
            .orElseThrow(() -> new RuntimeException("无法确定周报日期"));

        // Filter to the latest week
        final LocalDate targetWeek = latestWeek;
        List<WeeklyReport> reports = allSubmitted.stream()
            .filter(r -> targetWeek.equals(r.getWeekStartDate()))
            .collect(Collectors.toList());

        // Use requested team name or default to first team's name
        final String teamName;
        if (request.teamName() != null && !request.teamName().isBlank()) {
            teamName = teams.stream()
                .filter(t -> t.getName().equals(request.teamName()))
                .findFirst()
                .orElse(teams.get(0))
                .getName();
        } else {
            teamName = teams.get(0).getName();
        }

        if (reports.isEmpty()) {
            throw new RuntimeException("本周组内暂无已提交的周报");
        }

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

        List<WeeklyReportResponse> reportResponses = reports.stream()
            .map(this::toWeeklyResponse)
            .toList();

        String aiResult = aiService.mergeReports(reportResponses);

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
        summary.setSourceReportIds(reports.stream()
            .map(r -> String.valueOf(r.getId()))
            .collect(Collectors.joining(",")));

        teamSummaryRepository.save(summary);
        return toResponse(summary);
    }

    @Override
    public TeamSummaryResponse update(Long id, Long leaderId, String editedContent) {
        var summary = teamSummaryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("组内汇总不存在"));
        if (!summary.getLeaderId().equals(leaderId)) {
            throw new RuntimeException("只能编辑自己组的汇总");
        }
        if (!"DRAFT".equals(summary.getStatus())) {
            throw new RuntimeException("当前状态不可编辑");
        }
        summary.setEditedContent(editedContent);
        teamSummaryRepository.save(summary);
        return toResponse(summary);
    }

    @Override
    public TeamSummaryResponse submit(Long id, Long leaderId) {
        var summary = teamSummaryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("组内汇总不存在"));
        if (!summary.getLeaderId().equals(leaderId)) {
            throw new RuntimeException("只能提交自己组的汇总");
        }
        if (!"DRAFT".equals(summary.getStatus())) {
            throw new RuntimeException("当前状态不可提交");
        }
        summary.setStatus("SUBMITTED");
        summary.setSubmittedAt(LocalDateTime.now());
        teamSummaryRepository.save(summary);
        return toResponse(summary);
    }

    @Override
    public TeamSummaryResponse getById(Long id) {
        var summary = teamSummaryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("组内汇总不存在"));
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
            null, false, r.getVersion(),
            r.getCreatedAt(), r.getUpdatedAt()
        );
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
}
