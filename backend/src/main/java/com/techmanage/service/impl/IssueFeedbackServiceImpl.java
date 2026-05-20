package com.techmanage.service.impl;

import com.techmanage.dto.*;
import com.techmanage.entity.IssueFeedback;
import com.techmanage.entity.IssueLog;
import com.techmanage.repository.IssueFeedbackRepository;
import com.techmanage.repository.IssueLogRepository;
import com.techmanage.repository.UserRepository;
import com.techmanage.service.IssueFeedbackService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class IssueFeedbackServiceImpl implements IssueFeedbackService {

    private final IssueFeedbackRepository issueRepository;
    private final IssueLogRepository logRepository;
    private final UserRepository userRepository;

    public IssueFeedbackServiceImpl(IssueFeedbackRepository issueRepository,
                                    IssueLogRepository logRepository,
                                    UserRepository userRepository) {
        this.issueRepository = issueRepository;
        this.logRepository = logRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<IssueResponse> list(Long userId, String department, boolean isAdmin, boolean isIssueAdmin, String status) {
        List<IssueFeedback> list;
        if (isAdmin || isIssueAdmin) {
            list = issueRepository.findAllOrdered();
        } else {
            list = issueRepository.findByDepartmentOrderByCreatedAtDesc(department);
        }
        if (status != null && !status.isEmpty()) {
            list = list.stream().filter(i -> status.equals(i.getStatus())).toList();
        }
        return list.stream().map(this::toResponse).toList();
    }

    @Override
    public IssueResponse create(Long userId, IssueRequest request) {
        var issue = new IssueFeedback();
        issue.setTitle(request.title());
        issue.setDescription(request.description());
        issue.setDepartment(request.department());
        issue.setCategory(request.category());
        issue.setUrgency(request.urgency());
        issue.setSystem(request.system());
        issue.setSubmitterId(userId);
        issue.setStatus("待分派");
        issueRepository.save(issue);

        addLog(issue.getId(), userId, "提交问题", null);
        return toResponse(issue);
    }

    @Override
    public IssueResponse assign(Long id, Long userId, IssueAssignRequest request) {
        var issue = find(id);
        if (!"待分派".equals(issue.getStatus()) && !"整改中".equals(issue.getStatus())) {
            throw new RuntimeException("当前状态不允许分派");
        }
        issue.setCategory(request.category() != null ? request.category() : issue.getCategory());
        issue.setAssigneeId(request.assigneeId());
        issue.setStatus("整改中");
        issueRepository.save(issue);

        String assigneeName = userRepository.findById(request.assigneeId()).map(u -> u.getName()).orElse("");
        addLog(issue.getId(), userId, "分派责任人", "指派给" + assigneeName);
        return toResponse(issue);
    }

    @Override
    public IssueResponse submitSolution(Long id, Long userId, IssueSolutionRequest request) {
        var issue = find(id);
        if (!issue.getAssigneeId().equals(userId)) {
            throw new RuntimeException("只有责任人可以填写方案");
        }
        issue.setSolution(request.solution());
        issue.setDeadline(request.deadline());
        issue.setProgress(request.progress());
        issue.setStatus("整改中");
        issueRepository.save(issue);

        addLog(issue.getId(), userId, "提交方案", "整改时限: " + request.deadline());
        return toResponse(issue);
    }

    @Override
    public IssueResponse complete(Long id, Long userId) {
        var issue = find(id);
        if (!issue.getAssigneeId().equals(userId)) {
            throw new RuntimeException("只有责任人可以提交完成");
        }
        if (!"整改中".equals(issue.getStatus())) {
            throw new RuntimeException("当前状态不允许提交完成");
        }
        issue.setActualCompletionTime(LocalDateTime.now());
        issue.setStatus("待确认");
        issueRepository.save(issue);

        addLog(issue.getId(), userId, "提交完成", "等待提出人确认");
        return toResponse(issue);
    }

    @Override
    public IssueResponse confirm(Long id, Long userId, IssueConfirmRequest request) {
        var issue = find(id);
        if (!issue.getSubmitterId().equals(userId)) {
            throw new RuntimeException("只有问题提出人可以确认");
        }
        if (!"待确认".equals(issue.getStatus())) {
            throw new RuntimeException("当前状态不允许确认");
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
        issue.setRejectReason(request.reason());
        issueRepository.save(issue);

        addLog(issue.getId(), userId, "驳回问题", request.reason());
        return toResponse(issue);
    }

    @Override
    public IssueResponse getById(Long id) {
        return toResponse(find(id));
    }

    private IssueFeedback find(Long id) {
        return issueRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("问题不存在"));
    }

    private void addLog(Long issueId, Long userId, String action, String remark) {
        logRepository.save(new IssueLog(issueId, userId, action, remark));
    }

    private IssueResponse toResponse(IssueFeedback issue) {
        String submitterName = userRepository.findById(issue.getSubmitterId()).map(u -> u.getName()).orElse("");
        String assigneeName = issue.getAssigneeId() != null
            ? userRepository.findById(issue.getAssigneeId()).map(u -> u.getName()).orElse("")
            : "";
        List<IssueResponse.IssueLogInfo> logs = logRepository.findByIssueIdOrderByCreatedAtAsc(issue.getId())
            .stream()
            .map(l -> {
                String userName = userRepository.findById(l.getUserId()).map(u -> u.getName()).orElse("");
                return new IssueResponse.IssueLogInfo(l.getId(), userName, l.getAction(), l.getRemark(), l.getCreatedAt());
            })
            .toList();

        return new IssueResponse(
            issue.getId(), issue.getTitle(), issue.getDescription(),
            issue.getDepartment(), issue.getCategory(), issue.getUrgency(),
            issue.getSystem(), issue.getSubmitterId(), submitterName,
            issue.getAssigneeId(), assigneeName, issue.getSolution(),
            issue.getDeadline(), issue.getProgress(), issue.getActualCompletionTime(),
            issue.getStatus(), issue.getRejectReason(), logs,
            issue.getCreatedAt(), issue.getUpdatedAt()
        );
    }
}
