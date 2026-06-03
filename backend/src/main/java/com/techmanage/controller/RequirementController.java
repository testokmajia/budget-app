package com.techmanage.controller;

import com.techmanage.common.ApiResponse;
import com.techmanage.dto.PageResponse;
import com.techmanage.dto.RequirementRequest;
import com.techmanage.dto.RequirementResponse;
import com.techmanage.entity.*;
import com.techmanage.repository.*;
import com.techmanage.service.RequirementService;
import com.techmanage.util.TeamUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/requirements")
public class RequirementController {

    private final RequirementService requirementService;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final SystemInfoRepository systemInfoRepository;
    private final TeamRepository teamRepository;

    public RequirementController(RequirementService requirementService,
                                  UserRepository userRepository,
                                  DepartmentRepository departmentRepository,
                                  SystemInfoRepository systemInfoRepository,
                                  TeamRepository teamRepository) {
        this.requirementService = requirementService;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.systemInfoRepository = systemInfoRepository;
        this.teamRepository = teamRepository;
    }

    /** 获取当前用户 */
    private User currentUser(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return userRepository.findById(userId).orElseThrow();
    }

    // ==================== 查询接口 ====================

    /**
     * 需求列表（支持多条件筛选）
     */
    @GetMapping
    public ApiResponse<PageResponse<RequirementResponse>> list(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String dept,
            @RequestParam(required = false) Long submitterId,
            @RequestParam(required = false) String sysOwner,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo) {

        var result = requirementService.list(
                keyword, status, priority, dept, submitterId, sysOwner,
                dateFrom, dateTo, page, size, sortBy, sortDir);
        return ApiResponse.ok(PageResponse.of(
                result.getContent(), result.getTotalElements(), page, size));
    }

    /**
     * 需求详情
     */
    @GetMapping("/{id}")
    public ApiResponse<RequirementResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(requirementService.getDetail(id));
    }

    /**
     * 统计各状态数量
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Long>> stats() {
        return ApiResponse.ok(requirementService.stats());
    }

    /**
     * 获取系统列表（架构管理岗选择系统时使用）
     */
    @GetMapping("/systems")
    public ApiResponse<List<Map<String, Object>>> systems() {
        List<SystemInfo> systems = systemInfoRepository.findByEnabledTrue();
        List<Map<String, Object>> list = systems.stream().map(s -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("code", s.getCode());
            map.put("name", s.getName());
            map.put("leader", s.getLeader());
            map.put("team", s.getTeam());
            return map;
        }).collect(Collectors.toList());
        return ApiResponse.ok(list);
    }

    /**
     * 获取筛选选项（部门、提出人、系统负责人、当前用户团队等）
     */
    @GetMapping("/filter-options")
    public ApiResponse<Map<String, Object>> filterOptions(Authentication auth) {
        Map<String, Object> options = new java.util.LinkedHashMap<>();

        // 部门列表
        List<Department> depts = departmentRepository.findByEnabledTrue();
        options.put("departments", depts.stream().map(d -> Map.of("name", d.getName())).collect(Collectors.toList()));

        // 用户列表（提出人选项）
        List<User> users = userRepository.findByEnabledTrue();
        options.put("submitters", users.stream().map(u -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("department", u.getDepartment() != null ? u.getDepartment() : "");
            return m;
        }).collect(Collectors.toList()));

        // 系统负责人（从SystemInfo中提取去重）
        List<SystemInfo> systems = systemInfoRepository.findByEnabledTrue();
        options.put("sysOwners", systems.stream()
                .map(SystemInfo::getLeader)
                .filter(l -> l != null && !l.isEmpty())
                .distinct()
                .map(l -> Map.of("name", l))
                .collect(Collectors.toList()));

        // 当前用户所属团队名称（团队组长指派PM/PD时使用）
        if (auth != null) {
            User currentUser = currentUser(auth);
            String teamName = TeamUtils.findTeamNameForUser(
                    teamRepository.findAllByOrderByIdAsc(), currentUser.getName());
            options.put("myTeamName", teamName != null ? teamName : "");
        } else {
            options.put("myTeamName", "");
        }

        return ApiResponse.ok(options);
    }

    // ==================== 操作接口 ====================

    /**
     * 创建需求
     */
    @PostMapping
    public ApiResponse<RequirementResponse> create(@Valid @RequestBody RequirementRequest request,
                                                    Authentication auth) {
        User user = currentUser(auth);
        // 如果传了 submitterId，使用该用户作为提出人
        if (request.getSubmitterId() != null) {
            user = userRepository.findById(request.getSubmitterId())
                    .orElseThrow(() -> new RuntimeException("指定的提出人不存在"));
        }
        return ApiResponse.ok(requirementService.create(user, request));
    }

    /**
     * 审批通过
     */
    @PostMapping("/{id}/approve")
    public ApiResponse<RequirementResponse> approve(@PathVariable Long id,
                                                     @RequestBody(required = false) RequirementRequest request,
                                                     Authentication auth) {
        User user = currentUser(auth);
        String comment = request != null ? request.getComment() : null;
        return ApiResponse.ok(requirementService.approve(id, user, comment));
    }

    /**
     * 驳回
     */
    @PostMapping("/{id}/reject")
    public ApiResponse<RequirementResponse> reject(@PathVariable Long id,
                                                    @RequestBody(required = false) RequirementRequest request,
                                                    Authentication auth) {
        User user = currentUser(auth);
        String comment = request != null ? request.getComment() : null;
        return ApiResponse.ok(requirementService.reject(id, user, comment));
    }

    /**
     * 架构管理岗提交评估（含主责系统）
     */
    @PostMapping("/{id}/evaluate")
    @PreAuthorize("hasRole('ARCHITECT') or hasRole('ADMIN')")
    public ApiResponse<RequirementResponse> evaluate(@PathVariable Long id,
                                                      @RequestBody RequirementRequest request,
                                                      Authentication auth) {
        User user = currentUser(auth);
        return ApiResponse.ok(requirementService.evaluate(id, user,
                request.getSystemItems(), request.getPrimarySystemName(), request.getComment()));
    }

    /**
     * 团队组长指派项目经理和产品经理（按系统，一步完成）
     */
    @PostMapping("/{id}/assign-team")
    public ApiResponse<RequirementResponse> teamLeaderAssign(@PathVariable Long id,
                                                              @RequestBody RequirementRequest request,
                                                              Authentication auth) {
        User user = currentUser(auth);
        return ApiResponse.ok(requirementService.teamLeaderAssign(id, user,
                request.getSystemItems(), request.getComment()));
    }

    /**
     * 产品经理上传需求说明书
     */
    @PostMapping("/{id}/upload-spec")
    public ApiResponse<RequirementResponse> uploadSpec(@PathVariable Long id,
                                                        @RequestBody RequirementRequest request,
                                                        Authentication auth) {
        User user = currentUser(auth);
        return ApiResponse.ok(requirementService.uploadSpec(id, user,
                request.getSpecDocumentPath(), request.getSpecReviewers(), request.getComment()));
    }

    /**
     * 多方确认需求说明书
     */
    @PostMapping("/{id}/confirm-spec")
    public ApiResponse<RequirementResponse> confirmSpec(@PathVariable Long id,
                                                         @RequestBody(required = false) RequirementRequest request,
                                                         Authentication auth) {
        User user = currentUser(auth);
        String comment = request != null ? request.getComment() : null;
        return ApiResponse.ok(requirementService.confirmSpec(id, user, comment));
    }

    /**
     * 获取默认确认人员
     */
    @GetMapping("/{id}/default-reviewers")
    public ApiResponse<List<java.util.Map<String, Object>>> getDefaultReviewers(@PathVariable Long id) {
        return ApiResponse.ok(requirementService.getDefaultReviewers(id));
    }

    /**
     * 已驳回需求重新提交
     */
    @PostMapping("/{id}/resubmit")
    public ApiResponse<RequirementResponse> resubmit(@PathVariable Long id,
                                                      @RequestBody RequirementRequest request,
                                                      Authentication auth) {
        User user = currentUser(auth);
        return ApiResponse.ok(requirementService.resubmit(id, user, request));
    }

    // ==================== 确认后操作 ====================

    /**
     * PM启动实施，填写计划时间
     */
    @PostMapping("/{id}/start-implementation")
    public ApiResponse<RequirementResponse> startImplementation(@PathVariable Long id,
                                                                 @RequestBody RequirementRequest request,
                                                                 Authentication auth) {
        User user = currentUser(auth);
        return ApiResponse.ok(requirementService.startImplementation(id, user,
                request.getPlannedTestDate(), request.getPlannedProductionDate()));
    }

    /**
     * PM/PD填写正式投产日期
     */
    @PostMapping("/{id}/set-production")
    public ApiResponse<RequirementResponse> setProduction(@PathVariable Long id,
                                                           @RequestBody RequirementRequest request,
                                                           Authentication auth) {
        User user = currentUser(auth);
        return ApiResponse.ok(requirementService.setProduction(id, user, request.getProductionDate()));
    }

    /**
     * 业务提出人确认关闭
     */
    @PostMapping("/{id}/close")
    public ApiResponse<RequirementResponse> close(@PathVariable Long id, Authentication auth) {
        User user = currentUser(auth);
        return ApiResponse.ok(requirementService.close(id, user));
    }
}
