package com.techmanage.service.impl;

import com.techmanage.dto.*;
import com.techmanage.entity.IssueFeedback;
import com.techmanage.entity.IssueLog;
import com.techmanage.repository.IssueFeedbackRepository;
import com.techmanage.repository.IssueLogRepository;
import com.techmanage.repository.IssueOccasionRepository;
import com.techmanage.repository.UserRepository;
import com.techmanage.service.IssueFeedbackService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class IssueFeedbackServiceImpl implements IssueFeedbackService {

    private static final DateTimeFormatter CODE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final IssueFeedbackRepository issueRepository;
    private final IssueLogRepository logRepository;
    private final UserRepository userRepository;
    private final IssueOccasionRepository occasionRepository;

    public IssueFeedbackServiceImpl(IssueFeedbackRepository issueRepository,
                                     IssueLogRepository logRepository,
                                     UserRepository userRepository,
                                     IssueOccasionRepository occasionRepository) {
        this.issueRepository = issueRepository;
        this.logRepository = logRepository;
        this.userRepository = userRepository;
        this.occasionRepository = occasionRepository;
    }

    @Override
    public PageResponse<IssueResponse> list(int page, int size, String sortBy, String sortDir,
            List<String> statuses, List<Long> submitterIds, String submitterDepartment,
            Long occasionId, String issueType, String responsibleTeam,
            Long responsiblePersonId, LocalDate dateFrom, LocalDate dateTo,
            Long currentUserId, boolean isAdmin, boolean isIssueAdmin) {

        Specification<IssueFeedback> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!isAdmin && !isIssueAdmin) {
                predicates.add(cb.equal(root.get("submitterId"), currentUserId));
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
        List<IssueResponse> content = issuePage.getContent().stream()
                .map(this::toResponse)
                .toList();
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
        issue.setRootCause(request.rootCause());
        issue.setPermanentSolution(request.permanentSolution());
        issue.setPermanentDeadline(request.permanentDeadline());
        issue.setStatus("待分派");
        issueRepository.save(issue);

        addLog(issue.getId(), userId, "提交问题", null);
        return toResponse(issue);
    }

    @Override
    public IssueResponse assign(Long id, Long userId, IssueAssignRequest request) {
        var issue = find(id);
        issue.setResponsibleTeam(request.responsibleTeam());
        issue.setResponsiblePersonId(request.responsiblePersonId());
        issue.setStatus("已分派");
        issueRepository.save(issue);

        String personName = userRepository.findById(request.responsiblePersonId())
                .map(u -> u.getName()).orElse("");
        addLog(issue.getId(), userId, "分配团队和责任人",
                "团队: " + request.responsibleTeam() + ", 责任人: " + personName);
        return toResponse(issue);
    }

    @Override
    public IssueResponse submitSolution(Long id, Long userId, IssueSolutionRequest request) {
        var issue = find(id);
        if (!"已分派".equals(issue.getStatus()) && !"整改中".equals(issue.getStatus())) {
            throw new RuntimeException("当前状态不允许提交方案");
        }
        if (!issue.getResponsiblePersonId().equals(userId)) {
            throw new RuntimeException("只有责任人可以提交方案");
        }
        issue.setTemporarySolution(request.temporarySolution());
        issue.setTemporaryDeadline(request.temporaryDeadline());
        issue.setRootCause(request.rootCause() != null ? request.rootCause() : issue.getRootCause());
        issue.setPermanentSolution(request.permanentSolution() != null ? request.permanentSolution() : issue.getPermanentSolution());
        issue.setPermanentDeadline(request.permanentDeadline() != null ? request.permanentDeadline() : issue.getPermanentDeadline());
        issue.setStatus("整改中");
        issueRepository.save(issue);

        addLog(issue.getId(), userId, "提交整改方案", "临时时限: " + request.temporaryDeadline());
        return toResponse(issue);
    }

    @Override
    public IssueResponse complete(Long id, Long userId) {
        var issue = find(id);
        if (!"整改中".equals(issue.getStatus())) {
            throw new RuntimeException("当前状态不允许提交完成");
        }
        if (!issue.getResponsiblePersonId().equals(userId)) {
            throw new RuntimeException("只有责任人可以提交完成");
        }
        issue.setStatus("待确认");
        issueRepository.save(issue);

        addLog(issue.getId(), userId, "提交完成", "等待提出人确认");
        return toResponse(issue);
    }

    @Override
    public IssueResponse confirm(Long id, Long userId, IssueConfirmRequest request) {
        var issue = find(id);
        if (!"待确认".equals(issue.getStatus())) {
            throw new RuntimeException("当前状态不允许确认");
        }
        if (!issue.getSubmitterId().equals(userId)) {
            throw new RuntimeException("只有问题提出人可以确认");
        }
        if (request.satisfied()) {
            issue.setStatus("已完成");
            addLog(issue.getId(), userId, "确认完成", "问题已关闭");
        } else {
            issue.setStatus("整改中");
            addLog(issue.getId(), userId, "退回重改", request.remark());
        }
        issueRepository.save(issue);
        return toResponse(issue);
    }

    @Override
    public IssueResponse reject(Long id, Long userId, IssueRejectRequest request) {
        var issue = find(id);
        if (!"待分派".equals(issue.getStatus())) {
            throw new RuntimeException("当前状态不允许驳回");
        }
        issue.setStatus("已驳回");
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

        issueRepository.save(issue);
        addLog(issue.getId(), userId, "管理员编辑", "修改了问题信息");
        return toResponse(issue);
    }

    private IssueFeedback find(Long id) {
        return issueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("问题不存在"));
    }

    private void addLog(Long issueId, Long userId, String action, String remark) {
        logRepository.save(new IssueLog(issueId, userId, action, remark));
    }

    private String generateIssueCode() {
        String today = LocalDate.now().format(CODE_DATE_FMT);
        String prefix = "ISS-" + today + "-";
        List<IssueFeedback> all = issueRepository.findAll();
        int maxSeq = 0;
        for (IssueFeedback i : all) {
            if (i.getIssueCode() != null && i.getIssueCode().startsWith(prefix)) {
                try {
                    int seq = Integer.parseInt(i.getIssueCode().substring(prefix.length()));
                    if (seq > maxSeq) maxSeq = seq;
                } catch (NumberFormatException ignored) {}
            }
        }
        return prefix + String.format("%03d", maxSeq + 1);
    }

    private IssueResponse toResponse(IssueFeedback issue) {
        String submitterName = userRepository.findById(issue.getSubmitterId())
                .map(u -> u.getName()).orElse("");
        String responsiblePersonName = issue.getResponsiblePersonId() != null
                ? userRepository.findById(issue.getResponsiblePersonId()).map(u -> u.getName()).orElse("")
                : "";
        String occasionName = issue.getOccasionId() != null
                ? occasionRepository.findById(issue.getOccasionId()).map(o -> o.getName()).orElse("")
                : "";
        String occasionType = issue.getOccasionId() != null
                ? occasionRepository.findById(issue.getOccasionId()).map(o -> o.getType()).orElse("")
                : "";

        List<IssueResponse.IssueLogInfo> logs = logRepository.findByIssueIdOrderByCreatedAtAsc(issue.getId())
                .stream()
                .map(l -> {
                    String userName = userRepository.findById(l.getUserId())
                            .map(u -> u.getName()).orElse("");
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
                issue.getStatus(), logs,
                issue.getCreatedAt(), issue.getUpdatedAt()
        );
    }
}
