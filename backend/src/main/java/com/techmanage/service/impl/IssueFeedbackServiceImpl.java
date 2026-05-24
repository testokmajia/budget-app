package com.techmanage.service.impl;

import com.techmanage.dto.*;
import com.techmanage.entity.IssueFeedback;
import com.techmanage.entity.IssueLog;
import com.techmanage.entity.IssueOccasion;
import com.techmanage.entity.User;
import com.techmanage.repository.IssueFeedbackRepository;
import com.techmanage.repository.IssueLogRepository;
import com.techmanage.repository.IssueOccasionRepository;
import com.techmanage.repository.TeamRepository;
import com.techmanage.repository.UserRepository;
import com.techmanage.service.IssueFeedbackService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IssueFeedbackServiceImpl implements IssueFeedbackService {

    private static final DateTimeFormatter CODE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final IssueFeedbackRepository issueRepository;
    private final IssueLogRepository logRepository;
    private final UserRepository userRepository;
    private final IssueOccasionRepository occasionRepository;
    private final TeamRepository teamRepository;

    public IssueFeedbackServiceImpl(IssueFeedbackRepository issueRepository,
                                     IssueLogRepository logRepository,
                                     UserRepository userRepository,
                                     IssueOccasionRepository occasionRepository,
                                     TeamRepository teamRepository) {
        this.issueRepository = issueRepository;
        this.logRepository = logRepository;
        this.userRepository = userRepository;
        this.occasionRepository = occasionRepository;
        this.teamRepository = teamRepository;
    }

    @Override
    public PageResponse<IssueResponse> list(int page, int size, String sortBy, String sortDir,
            List<String> statuses, List<Long> submitterIds, String submitterDepartment,
            Long occasionId, String issueType, String responsibleTeam,
            Long responsiblePersonId, LocalDate dateFrom, LocalDate dateTo,
            Long currentUserId, boolean isAdmin, boolean isIssueAdmin,
            boolean isItEmployee, boolean myScope, List<Long> teamMemberIds) {

        Specification<IssueFeedback> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!isAdmin && !isIssueAdmin && !isItEmployee) {
                predicates.add(cb.equal(root.get("submitterId"), currentUserId));
            } else if (myScope && isItEmployee && !isAdmin && !isIssueAdmin) {
                if (teamMemberIds != null && !teamMemberIds.isEmpty()) {
                    List<Long> scopeIds = new ArrayList<>(teamMemberIds);
                    scopeIds.add(currentUserId);
                    predicates.add(cb.or(
                        cb.equal(root.get("submitterId"), currentUserId),
                        root.get("responsiblePersonId").in(scopeIds)
                    ));
                } else {
                    predicates.add(cb.or(
                        cb.equal(root.get("submitterId"), currentUserId),
                        cb.equal(root.get("responsiblePersonId"), currentUserId)
                    ));
                }
            }

            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }
            if (submitterIds != null && !submitterIds.isEmpty()) {
                predicates.add(root.get("submitterId").in(submitterIds));
            }
            if (submitterDepartment != null && !submitterDepartment.isBlank()) {
                predicates.add(cb.equal(root.get("submitterDepartment"), submitterDepartment));
            }
            if (occasionId != null) {
                predicates.add(cb.equal(root.get("occasionId"), occasionId));
            }
            if (issueType != null && !issueType.isBlank()) {
                predicates.add(cb.equal(root.get("issueType"), issueType));
            }
            if (responsibleTeam != null && !responsibleTeam.isBlank()) {
                predicates.add(cb.equal(root.get("responsibleTeam"), responsibleTeam));
            }
            if (responsiblePersonId != null) {
                predicates.add(cb.equal(root.get("responsiblePersonId"), responsiblePersonId));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom.atStartOfDay()));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo.plusDays(1).atStartOfDay()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, validateSortBy(sortBy)));
        Page<IssueFeedback> issuePage = issueRepository.findAll(spec, pageable);

        List<IssueFeedback> issues = issuePage.getContent();
        List<IssueResponse> content;
        if (issues.isEmpty()) {
            content = List.of();
        } else {
            content = batchToResponse(issues);
        }
        return PageResponse.of(content, issuePage.getTotalElements(), page, size);
    }

    private String validateSortBy(String sortBy) {
        return switch (sortBy) {
            case "issueCode", "title", "submitterDepartment", "submitterId",
                 "issueType", "responsibleTeam", "responsiblePersonId",
                 "temporaryDeadline", "permanentDeadline", "status", "createdAt" -> sortBy;
            default -> "createdAt";
        };
    }

    @Override
    public IssueResponse getById(Long id) {
        return toResponse(find(id));
    }

    // ==================== 工作流方法 ====================

    @Override
    public IssueResponse create(Long userId, IssueRequest request) {
        var issue = new IssueFeedback();
        issue.setIssueCode(generateIssueCode());
        issue.setSubmitterId(userId);
        issue.setTitle(request.title());
        issue.setDescription(request.description());
        issue.setSubmitterDepartment(request.submitterDepartment());
        issue.setOccasionId(request.occasionId());
        issue.setMeetingDepartment(request.meetingDepartment());
        issue.setMeetingDate(request.meetingDate());
        issue.setIssueType(request.issueType());
        issue.setStatus("待分派");
        issueRepository.save(issue);
        addLog(issue.getId(), userId, "提交问题", null);
        return toResponse(issue);
    }

    @Override
    public IssueResponse assign(Long id, Long userId, IssueAssignRequest request) {
        var issue = find(id);
        if (!"待分派".equals(issue.getStatus())) {
            throw new RuntimeException("当前状态不允许分派");
        }
        saveUndoInfo(issue, userId);
        issue.setResponsibleTeam(request.responsibleTeam());

        Long responsiblePersonId = request.responsiblePersonId();
        String personName;
        if (responsiblePersonId == null) {
            var team = teamRepository.findByName(request.responsibleTeam())
                    .orElseThrow(() -> new RuntimeException("团队不存在: " + request.responsibleTeam()));
            if (team.getLeader() == null || team.getLeader().isBlank()) {
                throw new RuntimeException("该团队未设置负责人");
            }
            var leader = userRepository.findByName(team.getLeader())
                    .orElseThrow(() -> new RuntimeException("团队负责人用户不存在: " + team.getLeader()));
            responsiblePersonId = leader.getId();
            personName = leader.getName();
        } else {
            personName = userRepository.findById(responsiblePersonId)
                    .map(u -> u.getName()).orElse("");
        }
        issue.setResponsiblePersonId(responsiblePersonId);
        issue.setSystem(request.system());
        issue.setStatus("待员工处理");
        issueRepository.save(issue);

        addLog(issue.getId(), userId, "分派问题",
                "团队: " + request.responsibleTeam() + ", 责任人: " + personName
                + (request.system() != null ? ", 系统: " + request.system() : ""));
        return toResponse(issue);
    }

    @Override
    public IssueResponse submitSolution(Long id, Long userId, IssueSolutionRequest request) {
        var issue = find(id);
        if (!"待员工处理".equals(issue.getStatus()) && !"已驳回".equals(issue.getStatus())) {
            throw new RuntimeException("当前状态不允许提交方案");
        }
        saveUndoInfo(issue, userId);
        issue.setTemporarySolution(request.temporarySolution());
        issue.setTemporaryDeadline(request.temporaryDeadline());
        issue.setRootCause(request.rootCause() != null ? request.rootCause() : issue.getRootCause());
        issue.setPermanentSolution(request.permanentSolution() != null ? request.permanentSolution() : issue.getPermanentSolution());
        issue.setPermanentDeadline(request.permanentDeadline() != null ? request.permanentDeadline() : issue.getPermanentDeadline());
        issue.setStatus("待组长审核");
        // 将责任人改回团队负责人，等待审核
        if (issue.getResponsibleTeam() != null && !issue.getResponsibleTeam().isBlank()) {
            var team = teamRepository.findByName(issue.getResponsibleTeam());
            if (team.isPresent() && team.get().getLeader() != null && !team.get().getLeader().isBlank()) {
                var leader = userRepository.findByName(team.get().getLeader());
                if (leader.isPresent()) {
                    issue.setResponsiblePersonId(leader.get().getId());
                }
            }
        }
        issueRepository.save(issue);
        addLog(issue.getId(), userId, "提交整改方案", "临时时限: " + request.temporaryDeadline());
        return toResponse(issue);
    }

    @Override
    public IssueResponse reviewByLeader(Long id, Long userId, IssueReviewRequest request) {
        var issue = find(id);
        if (!"待组长审核".equals(issue.getStatus())) {
            throw new RuntimeException("当前状态不允许审核");
        }
        saveUndoInfo(issue, userId);
        if (request.approved()) {
            issue.setStatus("待管理员审核");
            addLog(issue.getId(), userId, "负责人审核通过", request.comment());
        } else {
            issue.setStatus("待员工处理");
            addLog(issue.getId(), userId, "负责人退回修改",
                    request.comment() != null ? request.comment() : "方案需修改");
        }
        issueRepository.save(issue);
        return toResponse(issue);
    }

    @Override
    public IssueResponse reviewByAdmin(Long id, Long userId, IssueReviewRequest request) {
        var issue = find(id);
        if (!"待管理员审核".equals(issue.getStatus())) {
            throw new RuntimeException("当前状态不允许审核");
        }
        saveUndoInfo(issue, userId);
        if (request.approved()) {
            issue.setStatus("待确认");
            addLog(issue.getId(), userId, "管理员审核通过", request.comment());
        } else {
            issue.setStatus("待员工处理");
            addLog(issue.getId(), userId, "管理员退回修改",
                    request.comment() != null ? request.comment() : "方案需修改");
        }
        issueRepository.save(issue);
        return toResponse(issue);
    }

    @Override
    public IssueResponse confirm(Long id, Long userId, IssueConfirmRequest request) {
        var issue = find(id);
        if (!"待确认".equals(issue.getStatus())) {
            throw new RuntimeException("当前状态不允许确认");
        }
        saveUndoInfo(issue, userId);
        if (!issue.getSubmitterId().equals(userId)) {
            throw new RuntimeException("只有问题提出人可以确认");
        }
        if (request.satisfied()) {
            issue.setStatus("已完成");
            addLog(issue.getId(), userId, "确认完成", "问题已关闭");
        } else {
            issue.setStatus("待员工处理");
            addLog(issue.getId(), userId, "退回重改", request.remark());
        }
        issueRepository.save(issue);
        return toResponse(issue);
    }

    @Override
    public IssueResponse reject(Long id, Long userId, IssueRejectRequest request) {
        var issue = find(id);
        if ("已完成".equals(issue.getStatus()) || "已关闭".equals(issue.getStatus())) {
            throw new RuntimeException("已完成或已关闭的问题不能驳回");
        }
        saveUndoInfo(issue, userId);
        issue.setStatus(issue.getResponsiblePersonId() != null ? "待员工处理" : "待分派");
        issueRepository.save(issue);
        addLog(issue.getId(), userId, "驳回问题", request.reason());
        return toResponse(issue);
    }

    @Override
    public IssueResponse close(Long id, Long userId, IssueCloseRequest request) {
        var issue = find(id);
        if ("已关闭".equals(issue.getStatus())) {
            throw new RuntimeException("该问题已关闭");
        }
        saveUndoInfo(issue, userId);
        issue.setStatus("已关闭");
        issueRepository.save(issue);
        String remark = request.reason() != null && !request.reason().isBlank()
                ? "关闭原因: " + request.reason() : "管理员关闭";
        addLog(issue.getId(), userId, "关闭问题", remark);
        return toResponse(issue);
    }

    @Override
    public IssueResponse update(Long id, Long userId, IssueUpdateRequest request) {
        var issue = find(id);
        if (request.title() != null) issue.setTitle(request.title());
        if (request.description() != null) issue.setDescription(request.description());
        if (request.submitterDepartment() != null) issue.setSubmitterDepartment(request.submitterDepartment());
        if (request.occasionId() != null) issue.setOccasionId(request.occasionId());
        if (request.meetingDepartment() != null) issue.setMeetingDepartment(request.meetingDepartment());
        if (request.meetingDate() != null) issue.setMeetingDate(request.meetingDate());
        if (request.issueType() != null) issue.setIssueType(request.issueType());
        if (request.responsibleTeam() != null) issue.setResponsibleTeam(request.responsibleTeam());
        if (request.responsiblePersonId() != null) issue.setResponsiblePersonId(request.responsiblePersonId());
        if (request.temporarySolution() != null) issue.setTemporarySolution(request.temporarySolution());
        if (request.temporaryDeadline() != null) issue.setTemporaryDeadline(request.temporaryDeadline());
        if (request.rootCause() != null) issue.setRootCause(request.rootCause());
        if (request.permanentSolution() != null) issue.setPermanentSolution(request.permanentSolution());
        if (request.permanentDeadline() != null) issue.setPermanentDeadline(request.permanentDeadline());
        if (request.status() != null) issue.setStatus(request.status());
        if (request.system() != null) issue.setSystem(request.system());
        if (request.submitterId() != null) issue.setSubmitterId(request.submitterId());
        issueRepository.save(issue);
        addLog(issue.getId(), userId, "管理员编辑", "修改了问题信息");
        return toResponse(issue);
    }

    // ==================== 辅助方法 ====================

    private IssueFeedback find(Long id) {
        return issueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("问题不存在"));
    }

    private void addLog(Long issueId, Long userId, String action, String remark) {
        logRepository.save(new IssueLog(issueId, userId, action, remark));
    }

    private void saveUndoInfo(IssueFeedback issue, Long operatorId) {
        issue.setPreviousStatus(issue.getStatus());
        issue.setLastOperatorId(operatorId);
    }

    public IssueResponse undo(Long id, Long userId) {
        var issue = find(id);
        if (!userId.equals(issue.getLastOperatorId())) {
            throw new RuntimeException("只有上一操作人可以撤回");
        }
        if (issue.getPreviousStatus() == null) {
            throw new RuntimeException("没有可撤回的操作");
        }
        String prevStatus = issue.getPreviousStatus();
        issue.setStatus(prevStatus);
        issue.setPreviousStatus(null);
        issue.setLastOperatorId(null);
        issueRepository.save(issue);
        addLog(issue.getId(), userId, "撤回操作", "状态恢复为: " + prevStatus);
        return toResponse(issue);
    }

    private String generateIssueCode() {
        String today = LocalDate.now().format(CODE_DATE_FMT);
        String prefix = "ISS-" + today + "-";
        IssueFeedback latest = issueRepository.findTopByIssueCodeStartingWithOrderByIssueCodeDesc(prefix);
        int seq = 0;
        if (latest != null && latest.getIssueCode() != null && latest.getIssueCode().startsWith(prefix)) {
            try {
                seq = Integer.parseInt(latest.getIssueCode().substring(prefix.length()));
            } catch (NumberFormatException ignored) {}
        }
        return prefix + String.format("%03d", seq + 1);
    }

    private IssueResponse toResponse(IssueFeedback issue) {
        return batchToResponse(List.of(issue)).get(0);
    }

    private List<IssueResponse> batchToResponse(List<IssueFeedback> issues) {
        if (issues.isEmpty()) return List.of();

        Set<Long> userIds = new HashSet<>();
        Set<Long> occasionIds = new HashSet<>();
        for (IssueFeedback issue : issues) {
            userIds.add(issue.getSubmitterId());
            if (issue.getResponsiblePersonId() != null) userIds.add(issue.getResponsiblePersonId());
            if (issue.getOccasionId() != null) occasionIds.add(issue.getOccasionId());
        }

        List<Long> issueIds = issues.stream().map(IssueFeedback::getId).toList();
        Map<Long, List<IssueLog>> logsMap = new HashMap<>();
        List<IssueLog> allLogs = logRepository.findByIssueIdInOrderByCreatedAtAsc(issueIds);
        for (IssueLog log : allLogs) {
            logsMap.computeIfAbsent(log.getIssueId(), k -> new ArrayList<>()).add(log);
            userIds.add(log.getUserId());
        }

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, IssueOccasion> occasionMap = occasionRepository.findAllById(occasionIds).stream()
                .collect(Collectors.toMap(IssueOccasion::getId, o -> o));

        return issues.stream().map(issue -> {
            User submitter = userMap.get(issue.getSubmitterId());
            String submitterName = submitter != null ? submitter.getName() : "";

            String responsiblePersonName = "";
            if (issue.getResponsiblePersonId() != null) {
                User resp = userMap.get(issue.getResponsiblePersonId());
                if (resp != null) responsiblePersonName = resp.getName();
            }

            String occasionName = "";
            String occasionType = "";
            if (issue.getOccasionId() != null) {
                IssueOccasion o = occasionMap.get(issue.getOccasionId());
                if (o != null) {
                    occasionName = o.getName();
                    occasionType = o.getType();
                }
            }

            List<IssueLog> logs = logsMap.getOrDefault(issue.getId(), List.of());
            List<IssueResponse.IssueLogInfo> logInfos = logs.stream()
                    .map(l -> {
                        User u = userMap.get(l.getUserId());
                        String userName = u != null ? u.getName() : "";
                        return new IssueResponse.IssueLogInfo(l.getId(), userName, l.getAction(), l.getRemark(), l.getCreatedAt());
                    })
                    .toList();

            return new IssueResponse(
                    issue.getId(), issue.getIssueCode(), issue.getTitle(), issue.getDescription(),
                    issue.getSubmitterDepartment(), issue.getSubmitterId(), submitterName,
                    issue.getOccasionId(), occasionName, occasionType,
                    issue.getMeetingDepartment(), issue.getMeetingDate(),
                    issue.getIssueType(), issue.getResponsibleTeam(),
                    issue.getResponsiblePersonId(), responsiblePersonName,
                    issue.getTemporarySolution(), issue.getTemporaryDeadline(),
                    issue.getRootCause(), issue.getPermanentSolution(), issue.getPermanentDeadline(),
                    issue.getStatus(), issue.getLastOperatorId(), issue.getSystem(), logInfos,
                    issue.getCreatedAt(), issue.getUpdatedAt()
            );
        }).toList();
    }
}
