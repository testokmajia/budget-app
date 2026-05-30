package com.techmanage.service.impl;

import com.techmanage.common.BusinessException;
import com.techmanage.dto.ReviewRequest;
import com.techmanage.dto.SmartDraftResponse;
import com.techmanage.dto.WeeklyReportRequest;
import com.techmanage.dto.WeeklyReportResponse;
import com.techmanage.entity.Team;
import com.techmanage.entity.TeamSummary;
import com.techmanage.entity.User;
import com.techmanage.entity.WeeklyReport;
import com.techmanage.repository.TeamRepository;
import com.techmanage.repository.TeamSummaryRepository;
import com.techmanage.repository.UserRepository;
import com.techmanage.repository.WeeklyReportRepository;
import com.techmanage.service.WeeklyReportService;
import com.techmanage.util.TeamUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final TeamSummaryRepository teamSummaryRepository;
    private final ObjectMapper objectMapper;

    public WeeklyReportServiceImpl(WeeklyReportRepository weeklyReportRepository,
                                   UserRepository userRepository,
                                   TeamRepository teamRepository,
                                   TeamSummaryRepository teamSummaryRepository,
                                   ObjectMapper objectMapper) {
        this.weeklyReportRepository = weeklyReportRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.teamSummaryRepository = teamSummaryRepository;
        this.objectMapper = objectMapper;
    }

    private LocalDate getCurrentMonday() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private LocalDate getCurrentFriday() {
        return getCurrentMonday().plusDays(4);
    }

    @Override
    public WeeklyReportResponse getCurrentWeek(Long userId) {
        LocalDate monday = getCurrentMonday();
        LocalDate friday = getCurrentFriday();
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
        LocalDate weekStart = request.weekStartDate() != null ? request.weekStartDate() : getCurrentMonday();
        LocalDate weekEnd = request.weekEndDate() != null ? request.weekEndDate() : getCurrentFriday();
        var existing = weeklyReportRepository.findLatestByUserIdAndWeekStartDate(userId, weekStart);
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
            report.setWeekStartDate(weekStart);
            report.setWeekEndDate(weekEnd);
            report.setVersion(existing.get().getVersion() + 1);
        } else {
            report = new WeeklyReport();
            report.setUserId(userId);
            report.setWeekStartDate(weekStart);
            report.setWeekEndDate(weekEnd);
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
    @Transactional
    public WeeklyReportResponse submit(Long userId, WeeklyReportRequest request) {
        LocalDate weekStart = request.weekStartDate() != null ? request.weekStartDate() : getCurrentMonday();
        LocalDate weekEnd = request.weekEndDate() != null ? request.weekEndDate() : getCurrentFriday();
        var existing = weeklyReportRepository.findLatestByUserIdAndWeekStartDate(userId, weekStart);
        WeeklyReport report;
        if (existing.isPresent()) {
            var prev = existing.get();
            // Allow re-submit if not APPROVED (DRAFT, SUBMITTED, REJECTED all editable)
            if (!"APPROVED".equals(prev.getStatus())) {
                prev.setDoneWork(request.doneWork());
                prev.setPlanWork(request.planWork());
                prev.setProblems(request.problems());
                prev.setSupportNeeded(request.supportNeeded());
                prev.setStatus("SUBMITTED");
                prev.setSubmittedAt(LocalDateTime.now());
                prev.setReviewComment(null);
                prev.setReviewerId(null);
                prev.setReviewedAt(null);
                report = prev;
                weeklyReportRepository.save(report);
                return toResponse(report);
            }
            // If APPROVED, create new version
            report = new WeeklyReport();
            report.setUserId(userId);
            report.setWeekStartDate(weekStart);
            report.setWeekEndDate(weekEnd);
            report.setVersion(prev.getVersion() + 1);
        } else {
            report = new WeeklyReport();
            report.setUserId(userId);
            report.setWeekStartDate(weekStart);
            report.setWeekEndDate(weekEnd);
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
        LocalDate lastMonday = getCurrentMonday().minusWeeks(1);
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
        return buildMemberStatusList(leaderUserId, true);
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
            .orElseThrow(() -> new BusinessException("周报不存在"));
        if (!"SUBMITTED".equals(report.getStatus())) {
            throw new BusinessException("当前状态不可审批");
        }
        List<Long> memberIds = resolveTeamMemberIds(leaderUserId);
        if (!memberIds.contains(report.getUserId())) {
            throw new BusinessException("无权审批该周报");
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
            .orElseThrow(() -> new BusinessException("周报不存在"));
        if (!"SUBMITTED".equals(report.getStatus())) {
            throw new BusinessException("当前状态不可审批");
        }
        List<Long> memberIds = resolveTeamMemberIds(leaderUserId);
        if (!memberIds.contains(report.getUserId())) {
            throw new BusinessException("无权审批该周报");
        }
        if (request.comment() == null || request.comment().isBlank()) {
            throw new BusinessException("驳回必须填写批注");
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

    /**
     * Build a list of all team members with their submission status.
     * Leader sees ALL members (including those who haven't submitted).
     */
    private List<WeeklyReportResponse> buildMemberStatusList(Long leaderUserId, boolean onlySubmitted) {
        User leader = userRepository.findById(leaderUserId).orElse(null);
        if (leader == null) return List.of();

        List<Team> teams = teamRepository.findByLeader(leader.getName());
        if (teams.isEmpty()) return List.of();

        LocalDate thisMonday = getCurrentMonday();
        LocalDate thisFriday = getCurrentFriday();

        // Collect all member names with team info
        List<MemberTeamPair> allMembers = new ArrayList<>();
        for (Team t : teams) {
            if (t.getMembers() != null && !t.getMembers().isBlank()) {
                Arrays.stream(t.getMembers().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(name -> allMembers.add(new MemberTeamPair(name, t.getName())));
            }
        }

        if (allMembers.isEmpty()) return List.of();

        List<String> memberNames = allMembers.stream().map(m -> m.name).distinct().toList();
        List<User> users = userRepository.findByNameIn(new ArrayList<>(memberNames));
        Map<String, Long> nameToId = new HashMap<>();
        for (User u : users) nameToId.put(u.getName(), u.getId());

        List<WeeklyReportResponse> result = new ArrayList<>();
        for (MemberTeamPair pair : allMembers) {
            Long memberId = nameToId.get(pair.name);
            if (memberId == null) continue;

            var report = weeklyReportRepository.findLatestByUserIdAndWeekStartDate(memberId, thisMonday);
            if (report.isPresent()) {
                result.add(toResponse(report.get(), pair.teamName));
            } else if (!onlySubmitted) {
                // Create placeholder for member who hasn't submitted
                result.add(createPlaceholder(memberId, pair.name, pair.teamName, thisMonday, thisFriday));
            }
        }

        return result;
    }

    private WeeklyReportResponse createPlaceholder(Long userId, String userName, String teamName,
                                                    LocalDate weekStart, LocalDate weekEnd) {
        String userDepartment = "";
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) userDepartment = user.getDepartment() != null ? user.getDepartment() : "";
        return new WeeklyReportResponse(
            null, userId, userName, userDepartment, teamName,
            weekStart, weekEnd,
            null, null, null, null,
            "NOT_SUBMITTED", null,
            null, null, null, false, 1,
            null, null
        );
    }

    private record MemberTeamPair(String name, String teamName) {}

    /**
     * Find the team name for a user by scanning all teams' members fields.
     * Delegates to shared {@link TeamUtils}.
     */
    private String findTeamNameForUser(String userName) {
        return TeamUtils.findTeamNameForUser(teamRepository.findAll(), userName);
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
        return toResponse(r, null);
    }

    private WeeklyReportResponse toResponse(WeeklyReport r, String knownTeamName) {
        String userName = "";
        String userDepartment = "";
        String teamName = knownTeamName;
        String reviewerName = "";
        User user = userRepository.findById(r.getUserId()).orElse(null);
        if (user != null) {
            userName = user.getName();
            userDepartment = user.getDepartment() != null ? user.getDepartment() : "";
            if (teamName == null) {
                teamName = findTeamNameForUser(userName);
            }
        }
        if (r.getReviewerId() != null) {
            User reviewer = userRepository.findById(r.getReviewerId()).orElse(null);
            if (reviewer != null) reviewerName = reviewer.getName();
        }
        return new WeeklyReportResponse(
            r.getId(), r.getUserId(), userName, userDepartment, teamName,
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

    @Override
    public Map<String, Object> getTeamStats(Long leaderUserId) {
        User leader = userRepository.findById(leaderUserId).orElseThrow();
        List<Team> ledTeams = teamRepository.findByLeader(leader.getName());

        Set<String> allMemberNames = new LinkedHashSet<>();
        Map<String, Set<String>> teamMembers = new LinkedHashMap<>();
        for (Team t : ledTeams) {
            Set<String> names = new LinkedHashSet<>();
            if (t.getMembers() != null) {
                for (String name : t.getMembers().split(",")) {
                    var trimmed = name.trim();
                    if (!trimmed.isEmpty()) {
                        names.add(trimmed);
                        allMemberNames.add(trimmed);
                    }
                }
            }
            teamMembers.put(t.getName(), names);
        }

        List<User> allUsers = userRepository.findByNameIn(new ArrayList<>(allMemberNames));
        Map<String, Long> nameToId = new HashMap<>();
        for (User u : allUsers) nameToId.put(u.getName(), u.getId());

        LocalDate thisMonday = getCurrentMonday();
        int totalSubmitted = 0;
        int totalMembers = allMemberNames.size();

        List<Map<String, Object>> teams = new ArrayList<>();
        for (Team t : ledTeams) {
            int teamSubmitted = 0;
            List<Map<String, Object>> members = new ArrayList<>();
            for (String name : teamMembers.getOrDefault(t.getName(), Set.of())) {
                Long userId = nameToId.get(name);
                boolean submitted = false;
                Long reportId = null;
                if (userId != null) {
                    var r = weeklyReportRepository.findLatestByUserIdAndWeekStartDate(userId, thisMonday);
                    if (r.isPresent()) {
                        submitted = "SUBMITTED".equals(r.get().getStatus()) || "APPROVED".equals(r.get().getStatus());
                        reportId = r.get().getId();
                    }
                }
                if (submitted) teamSubmitted++;
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("name", name);
                m.put("submitted", submitted);
                m.put("reportId", reportId);
                members.add(m);
            }
            totalSubmitted += teamSubmitted;
            Map<String, Object> tm = new java.util.LinkedHashMap<>();
            tm.put("teamName", t.getName());
            tm.put("totalMembers", members.size());
            tm.put("submittedCount", teamSubmitted);
            tm.put("members", members);
            teams.add(tm);
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("teams", teams);
        result.put("totalMembers", totalMembers);
        result.put("totalSubmitted", totalSubmitted);
        result.put("overallRate", totalMembers > 0 ? (double) totalSubmitted / totalMembers : 0);
        return result;
    }

    /**
     * Build department-level team submission status for clerks/admins.
     */
    @Override
    public Map<String, Object> getDeptTeamStatus(Long clerkUserId) {
        User clerk = userRepository.findById(clerkUserId).orElseThrow();
        String department = clerk.getDepartment();
        if (department == null || department.isBlank()) {
            throw new BusinessException("您没有所属部门");
        }

        // Get all teams in this department
        List<Team> allTeams = teamRepository.findAll();
        List<Team> deptTeams = allTeams.stream()
            .filter(t -> department.equals(t.getDepartment()))
            .toList();

        LocalDate thisMonday = getCurrentMonday();
        LocalDate thisFriday = getCurrentFriday();

        // Load all team summaries for this week in one query
        var summaries = teamSummaryRepository.findByWeekStartDate(thisMonday);
        Map<String, TeamSummary> summaryByTeam = new HashMap<>();
        for (var s : summaries) {
            summaryByTeam.put(s.getTeamName(), s);
        }

        // Load all members in department teams to check submission status
        Set<String> allMemberNames = new HashSet<>();
        for (Team t : deptTeams) {
            if (t.getMembers() != null && !t.getMembers().isBlank()) {
                Arrays.stream(t.getMembers().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(allMemberNames::add);
            }
        }
        List<User> allUsers = userRepository.findByNameIn(new ArrayList<>(allMemberNames));
        Map<String, Long> nameToId = new HashMap<>();
        for (User u : allUsers) nameToId.put(u.getName(), u.getId());

        // Batch-load all reports for this week
        List<Long> allMemberIds = allUsers.stream().map(User::getId).toList();
        var allReports = allMemberIds.isEmpty() ? List.<WeeklyReport>of()
            : weeklyReportRepository.findSubmittedOrApprovedByUserIds(allMemberIds);
        Map<Long, WeeklyReport> reportByUserId = new HashMap<>();
        for (var r : allReports) {
            if (thisMonday.equals(r.getWeekStartDate())) {
                reportByUserId.put(r.getUserId(), r);
            }
        }

        List<Map<String, Object>> teams = new ArrayList<>();
        int totalSubmitted = 0;

        for (Team t : deptTeams) {
            TeamSummary summary = summaryByTeam.get(t.getName());
            boolean hasSummary = summary != null;
            Long summaryId = hasSummary ? summary.getId() : null;
            String summaryStatus = hasSummary ? summary.getStatus() : null;

            int memberCount = 0;
            int submittedCount = 0;
            if (t.getMembers() != null && !t.getMembers().isBlank()) {
                for (String name : t.getMembers().split(",")) {
                    String trimmed = name.trim();
                    if (trimmed.isEmpty()) continue;
                    memberCount++;
                    Long uid = nameToId.get(trimmed);
                    if (uid != null && reportByUserId.containsKey(uid)) {
                        submittedCount++;
                    }
                }
            }

            totalSubmitted += submittedCount;
            Map<String, Object> tm = new LinkedHashMap<>();
            tm.put("teamName", t.getName());
            tm.put("leaderName", t.getLeader());
            tm.put("memberCount", memberCount);
            tm.put("submittedCount", submittedCount);
            tm.put("hasSummary", hasSummary);
            tm.put("summaryId", summaryId);
            tm.put("summaryStatus", summaryStatus);
            teams.add(tm);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("department", department);
        result.put("weekStartDate", thisMonday.toString());
        result.put("weekEndDate", thisFriday.toString());
        result.put("teams", teams);
        result.put("totalTeams", deptTeams.size());
        result.put("totalSubmitted", totalSubmitted);
        return result;
    }

    @Override
    public void remindMember(Long reportId, Long leaderUserId) {
        var report = weeklyReportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException("周报不存在"));
        User leader = userRepository.findById(leaderUserId).orElseThrow();
        User member = userRepository.findById(report.getUserId()).orElseThrow();
        List<Team> ledTeams = teamRepository.findByLeader(leader.getName());
        boolean isLeader = ledTeams.stream().anyMatch(t -> {
            if (t.getMembers() == null) return false;
            for (String n : t.getMembers().split(",")) {
                if (n.trim().equals(member.getName())) return true;
            }
            return false;
        });
        if (!isLeader) throw new BusinessException("无权催办");
        // Save to mark activity (reminder signal for future notification system)
        weeklyReportRepository.save(report);
    }

    @Override
    public Map<String, Object> compareWeeks(Long userId, LocalDate from, LocalDate to) {
        var r1 = weeklyReportRepository.findLatestByUserIdAndWeekStartDate(userId, from);
        var r2 = weeklyReportRepository.findLatestByUserIdAndWeekStartDate(userId, to);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        if (r1.isPresent() && r2.isPresent()) {
            int done1 = countDoneItems(r1.get().getDoneWork());
            int done2 = countDoneItems(r2.get().getDoneWork());
            int plan1 = countDoneItems(r1.get().getPlanWork());
            int plan2 = countDoneItems(r2.get().getPlanWork());
            result.put("doneDiff", done2 - done1);
            result.put("planDiff", plan2 - plan1);
            result.put("fromWeek", r1.get().getWeekStartDate());
            result.put("toWeek", r2.get().getWeekStartDate());
        }
        return result;
    }

    private int countDoneItems(String text) {
        if (text == null || text.isBlank()) return 0;
        try {
            var arr = objectMapper.readTree(text);
            return arr.size();
        } catch (Exception e) {
            return (int) text.lines().filter(l -> !l.isBlank()).count();
        }
    }
}
