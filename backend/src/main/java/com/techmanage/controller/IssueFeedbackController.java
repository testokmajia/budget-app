package com.techmanage.controller;

import com.techmanage.common.ApiResponse;
import com.techmanage.dto.*;
import com.techmanage.entity.Team;
import com.techmanage.entity.User;
import com.techmanage.repository.TeamRepository;
import com.techmanage.repository.UserRepository;
import com.techmanage.service.IssueFeedbackService;
import com.techmanage.util.ExcelUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/issues")
public class IssueFeedbackController {

    private final IssueFeedbackService issueService;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    public IssueFeedbackController(IssueFeedbackService issueService,
                                    UserRepository userRepository,
                                    TeamRepository teamRepository) {
        this.issueService = issueService;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
    }

    private User currentUser(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return userRepository.findById(userId).orElseThrow();
    }

    private boolean isAdmin(User u) {
        return u.getRoles().stream().anyMatch(r -> r.getCode().equals("ROLE_ADMIN"));
    }

    private boolean isIssueAdmin(User u) {
        return u.getRoles().stream().anyMatch(r -> r.getCode().equals("ROLE_ISSUE_ADMIN"));
    }

    private List<Long> getTeamMemberIds(User u) {
        List<Team> ledTeams = teamRepository.findByLeader(u.getName());
        if (ledTeams.isEmpty()) return Collections.emptyList();
        Set<String> memberNames = new HashSet<>();
        for (Team t : ledTeams) {
            if (t.getMembers() != null) {
                for (String name : t.getMembers().split(",")) {
                    memberNames.add(name.trim());
                }
            }
        }
        if (memberNames.isEmpty()) return Collections.emptyList();
        return userRepository.findByNameIn(new ArrayList<>(memberNames))
                .stream().map(User::getId).toList();
    }

    @GetMapping
    public ApiResponse<PageResponse<IssueResponse>> list(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) List<Long> submitterIds,
            @RequestParam(required = false) List<String> submitterDepartments,
            @RequestParam(required = false) Long occasionId,
            @RequestParam(required = false) String issueType,
            @RequestParam(required = false) String responsibleTeam,
            @RequestParam(required = false) Long responsiblePersonId,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(defaultValue = "true") boolean myScope) {
        User u = currentUser(auth);
        boolean isItEmployee = "信息科技部".equals(u.getDepartment());
        List<Long> teamMemberIds = isItEmployee ? getTeamMemberIds(u) : Collections.emptyList();
        return ApiResponse.ok(issueService.list(page, size, sortBy, sortDir,
                statuses, submitterIds, submitterDepartments, occasionId, issueType,
                responsibleTeam, responsiblePersonId, dateFrom, dateTo,
                u.getId(), isAdmin(u), isIssueAdmin(u), isItEmployee, myScope, teamMemberIds));
    }

    @GetMapping("/{id}")
    public ApiResponse<IssueResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(issueService.getById(id));
    }

    @PostMapping
    public ApiResponse<IssueResponse> create(Authentication auth,
                                              @Valid @RequestBody IssueRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(issueService.create(userId, request));
    }

    // 管理员分派: 待分派 → 待员工处理
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ROLE_ISSUE_ADMIN', 'ROLE_ADMIN')")
    public ApiResponse<IssueResponse> assign(@PathVariable Long id,
                                              Authentication auth,
                                              @Valid @RequestBody IssueAssignRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(issueService.assign(id, userId, request));
    }

    // 员工填写方案: 待员工处理 → 待组长审核
    @PutMapping("/{id}/solution")
    public ApiResponse<IssueResponse> submitSolution(@PathVariable Long id,
                                                       Authentication auth,
                                                       @Valid @RequestBody IssueSolutionRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(issueService.submitSolution(id, userId, request));
    }

    // 团队负责人审核: 待组长审核 → 待管理员审核 (通过) or 待员工处理 (退回)
    @PutMapping("/{id}/review-leader")
    public ApiResponse<IssueResponse> reviewByLeader(@PathVariable Long id,
                                                      Authentication auth,
                                                      @Valid @RequestBody IssueReviewRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(issueService.reviewByLeader(id, userId, request));
    }

    // 管理员审核: 待管理员审核 → 待确认 (通过) or 待员工处理 (退回)
    @PutMapping("/{id}/review-admin")
    @PreAuthorize("hasAnyRole('ROLE_ISSUE_ADMIN', 'ROLE_ADMIN')")
    public ApiResponse<IssueResponse> reviewByAdmin(@PathVariable Long id,
                                                     Authentication auth,
                                                     @Valid @RequestBody IssueReviewRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(issueService.reviewByAdmin(id, userId, request));
    }

    // 提出人确认: 待确认 → 已完成 (确认) or 待员工处理 (退回)
    @PutMapping("/{id}/confirm")
    public ApiResponse<IssueResponse> confirm(@PathVariable Long id,
                                               Authentication auth,
                                               @Valid @RequestBody IssueConfirmRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(issueService.confirm(id, userId, request));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ROLE_ISSUE_ADMIN', 'ROLE_ADMIN')")
    public ApiResponse<IssueResponse> reject(@PathVariable Long id,
                                              Authentication auth,
                                              @Valid @RequestBody IssueRejectRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(issueService.reject(id, userId, request));
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ROLE_ISSUE_ADMIN', 'ROLE_ADMIN')")
    public ApiResponse<IssueResponse> close(@PathVariable Long id,
                                             Authentication auth,
                                             @RequestBody IssueCloseRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(issueService.close(id, userId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ISSUE_ADMIN', 'ROLE_ADMIN')")
    public ApiResponse<IssueResponse> update(@PathVariable Long id,
                                              Authentication auth,
                                              @Valid @RequestBody IssueUpdateRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(issueService.update(id, userId, request));
    }

    @PutMapping("/{id}/undo")
    public ApiResponse<IssueResponse> undo(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(issueService.undo(id, userId));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(Authentication auth,
                                          @RequestParam(required = false) List<String> statuses,
                                          @RequestParam(required = false) List<Long> submitterIds,
                                          @RequestParam(required = false) List<String> submitterDepartments,
                                          @RequestParam(required = false) Long occasionId,
                                          @RequestParam(required = false) String issueType,
                                          @RequestParam(required = false) String responsibleTeam,
                                          @RequestParam(required = false) Long responsiblePersonId,
                                          @RequestParam(required = false) LocalDate dateFrom,
                                          @RequestParam(required = false) LocalDate dateTo) {
        User u = currentUser(auth);
        boolean isItEmployee = "信息科技部".equals(u.getDepartment());
        List<Long> teamMemberIds = isItEmployee ? getTeamMemberIds(u) : Collections.emptyList();
        var result = issueService.list(0, Integer.MAX_VALUE, "createdAt", "desc",
                statuses, submitterIds, submitterDepartments, occasionId, issueType,
                responsibleTeam, responsiblePersonId, dateFrom, dateTo,
                u.getId(), isAdmin(u), isIssueAdmin(u), isItEmployee, false, teamMemberIds);

        var headers = List.of("问题编号", "标题", "详情", "提出部门", "提出人", "提出场合",
                "问题类型", "责任团队", "责任人", "涉及系统", "临时整改方案", "临时整改时限",
                "产生原因", "永久解决方案", "永久解决时限", "状态", "提出日期");
        var fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<List<String>> rows = result.content().stream().map(i -> {
            String desc = i.description() != null ? i.description() : "";
            String dept = i.submitterDepartment() != null ? i.submitterDepartment() : "";
            String occ = i.occasionName() != null ? i.occasionName() : "";
            String type = i.issueType() != null ? i.issueType() : "";
            String team = i.responsibleTeam() != null ? i.responsibleTeam() : "";
            String person = i.responsiblePersonName() != null ? i.responsiblePersonName() : "";
            String sys = i.system() != null ? i.system() : "";
            String tmpSol = i.temporarySolution() != null ? i.temporarySolution() : "";
            String tmpDl = i.temporaryDeadline() != null ? i.temporaryDeadline().toString() : "";
            String cause = i.rootCause() != null ? i.rootCause() : "";
            String permSol = i.permanentSolution() != null ? i.permanentSolution() : "";
            String permDl = i.permanentDeadline() != null ? i.permanentDeadline().toString() : "";
            String date = i.createdAt() != null ? i.createdAt().format(fmt) : "";
            return List.of(i.issueCode(), i.title(), desc, dept, i.submitterName(), occ,
                    type, team, person, sys, tmpSol, tmpDl, cause, permSol, permDl, i.status(), date);
        }).toList();

        byte[] excel = ExcelUtil.generate("科技问题管理", headers, rows);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=issues.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
