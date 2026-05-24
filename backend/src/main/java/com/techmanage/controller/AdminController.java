package com.techmanage.controller;

import com.techmanage.common.ApiResponse;
import com.techmanage.dto.*;
import com.techmanage.entity.Department;
import com.techmanage.entity.IssueCategory;
import com.techmanage.entity.IssueOccasion;
import com.techmanage.entity.Role;
import com.techmanage.entity.SystemInfo;
import com.techmanage.entity.Team;
import com.techmanage.service.AdminService;
import com.techmanage.util.ExcelUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // === 用户管理 ===

    @GetMapping("/users")
    public ApiResponse<PageResponse<UserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.ok(adminService.listUsers(page, size, username, name, department, enabled));
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok(adminService.createUser(request));
    }

    @PutMapping("/users/{id}/toggle")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<UserResponse> toggleUser(@PathVariable Long id) {
        return ApiResponse.ok(adminService.toggleUserEnabled(id));
    }

    @PutMapping("/users/{id}/reset-password")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
                                           @Valid @RequestBody ResetPasswordRequest request) {
        adminService.resetPassword(id, request.password());
        return ApiResponse.ok();
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<UserResponse> updateUser(@PathVariable Long id,
                                                @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok(adminService.updateUser(id, request));
    }

    @GetMapping("/roles")
    public ApiResponse<List<Role>> listRoles() {
        return ApiResponse.ok(adminService.listRoles());
    }

    // === 问题分类管理 ===

    @GetMapping("/categories")
    public ApiResponse<List<IssueCategory>> listCategories() {
        return ApiResponse.ok(adminService.listCategories());
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<IssueCategory> createCategory(@Valid @RequestBody IssueCategoryRequest request) {
        return ApiResponse.ok(adminService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<IssueCategory> updateCategory(@PathVariable Long id,
                                                     @Valid @RequestBody IssueCategoryRequest request) {
        return ApiResponse.ok(adminService.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        adminService.deleteCategory(id);
        return ApiResponse.ok();
    }

    // === 部门管理 ===

    @GetMapping("/departments")
    public ApiResponse<List<Department>> listDepartments() {
        return ApiResponse.ok(adminService.listDepartments());
    }

    @PostMapping("/departments")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<Department> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        return ApiResponse.ok(adminService.createDepartment(request));
    }

    @PutMapping("/departments/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<Department> updateDepartment(@PathVariable Long id,
                                                     @Valid @RequestBody DepartmentRequest request) {
        return ApiResponse.ok(adminService.updateDepartment(id, request));
    }

    @DeleteMapping("/departments/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<Void> deleteDepartment(@PathVariable Long id) {
        adminService.deleteDepartment(id);
        return ApiResponse.ok();
    }

    // === 所属系统管理 ===

    @GetMapping("/systems")
    public ApiResponse<List<SystemInfo>> listSystems() {
        return ApiResponse.ok(adminService.listSystems());
    }

    @PostMapping("/systems")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<SystemInfo> createSystem(@Valid @RequestBody SystemInfoRequest request) {
        return ApiResponse.ok(adminService.createSystem(request));
    }

    @PutMapping("/systems/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<SystemInfo> updateSystem(@PathVariable Long id,
                                                 @Valid @RequestBody SystemInfoRequest request) {
        return ApiResponse.ok(adminService.updateSystem(id, request));
    }

    @DeleteMapping("/systems/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<Void> deleteSystem(@PathVariable Long id) {
        adminService.deleteSystem(id);
        return ApiResponse.ok();
    }

    // === 团队管理 ===

    @GetMapping("/teams")
    public ApiResponse<List<Team>> listTeams() {
        return ApiResponse.ok(adminService.listTeams());
    }

    @PostMapping("/teams")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<Team> createTeam(@Valid @RequestBody TeamRequest request) {
        return ApiResponse.ok(adminService.createTeam(request));
    }

    @PutMapping("/teams/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<Team> updateTeam(@PathVariable Long id,
                                         @Valid @RequestBody TeamRequest request) {
        return ApiResponse.ok(adminService.updateTeam(id, request));
    }

    @DeleteMapping("/teams/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<Void> deleteTeam(@PathVariable Long id) {
        adminService.deleteTeam(id);
        return ApiResponse.ok();
    }

    // === 提出场合管理 ===

    @GetMapping("/occasions")
    public ApiResponse<List<IssueOccasion>> listOccasions() {
        return ApiResponse.ok(adminService.listOccasions());
    }

    @PostMapping("/occasions")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<IssueOccasion> createOccasion(@Valid @RequestBody IssueOccasionRequest request) {
        return ApiResponse.ok(adminService.createOccasion(request));
    }

    @PutMapping("/occasions/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<IssueOccasion> updateOccasion(@PathVariable Long id,
                                                      @Valid @RequestBody IssueOccasionRequest request) {
        return ApiResponse.ok(adminService.updateOccasion(id, request));
    }

    @DeleteMapping("/occasions/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<Void> deleteOccasion(@PathVariable Long id) {
        adminService.deleteOccasion(id);
        return ApiResponse.ok();
    }

    // === Excel导出 ===

    private static ResponseEntity<byte[]> excelResponse(String filename, byte[] data) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/users/export")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_ISSUE_ADMIN')")
    public ResponseEntity<byte[]> exportUsers() {
        var result = adminService.listUsers(0, Integer.MAX_VALUE, null, null, null, null);
        var headers = List.of("用户名", "姓名", "部门", "职位", "电话", "状态", "角色");
        List<List<String>> rows = result.content().stream().map(u -> List.of(
                u.username(), u.name(), u.department() != null ? u.department() : "",
                u.position() != null ? u.position() : "",
                u.phone() != null ? u.phone() : "",
                u.enabled() ? "启用" : "禁用",
                u.roles().stream().map(UserResponse.RoleInfo::name).reduce((a, b) -> a + ", " + b).orElse("")
        )).toList();
        return excelResponse("users.xlsx", ExcelUtil.generate("用户管理", headers, rows));
    }

    @GetMapping("/categories/export")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_ISSUE_ADMIN')")
    public ResponseEntity<byte[]> exportCategories() {
        var list = adminService.listCategories();
        var headers = List.of("分类名称", "排序", "状态");
        List<List<String>> rows = list.stream().map(c -> List.of(
                c.getName(), String.valueOf(c.getSortOrder()), c.isEnabled() ? "启用" : "禁用"
        )).toList();
        return excelResponse("categories.xlsx", ExcelUtil.generate("问题分类", headers, rows));
    }

    @GetMapping("/departments/export")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_ISSUE_ADMIN')")
    public ResponseEntity<byte[]> exportDepartments() {
        var list = adminService.listDepartments();
        var headers = List.of("部门名称", "部门负责人", "状态");
        List<List<String>> rows = list.stream().map(d -> List.of(
                d.getName(), d.getLeader() != null ? d.getLeader() : "", d.isEnabled() ? "启用" : "禁用"
        )).toList();
        return excelResponse("departments.xlsx", ExcelUtil.generate("部门管理", headers, rows));
    }

    @GetMapping("/systems/export")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_ISSUE_ADMIN')")
    public ResponseEntity<byte[]> exportSystems() {
        var list = adminService.listSystems();
        var headers = List.of("系统名称", "系统负责人", "所属团队", "状态");
        List<List<String>> rows = list.stream().map(s -> List.of(
                s.getName(), s.getLeader() != null ? s.getLeader() : "",
                s.getTeam() != null ? s.getTeam() : "", s.isEnabled() ? "启用" : "禁用"
        )).toList();
        return excelResponse("systems.xlsx", ExcelUtil.generate("信息系统", headers, rows));
    }

    @GetMapping("/teams/export")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_ISSUE_ADMIN')")
    public ResponseEntity<byte[]> exportTeams() {
        var list = adminService.listTeams();
        var headers = List.of("团队名称", "所属部门", "团队负责人", "团队成员", "负责系统", "状态");
        List<List<String>> rows = list.stream().map(t -> List.of(
                t.getName(), t.getDepartment() != null ? t.getDepartment() : "",
                t.getLeader() != null ? t.getLeader() : "",
                t.getMembers() != null ? t.getMembers() : "",
                t.getSystems() != null ? t.getSystems() : "",
                t.isEnabled() ? "启用" : "禁用"
        )).toList();
        return excelResponse("teams.xlsx", ExcelUtil.generate("团队管理", headers, rows));
    }

    @GetMapping("/occasions/export")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_ISSUE_ADMIN')")
    public ResponseEntity<byte[]> exportOccasions() {
        var list = adminService.listOccasions();
        var headers = List.of("场合名称", "类型", "状态");
        List<List<String>> rows = list.stream().map(o -> List.of(
                o.getName(), "MEETING".equals(o.getType()) ? "会议" : "通用",
                o.isEnabled() ? "启用" : "禁用"
        )).toList();
        return excelResponse("occasions.xlsx", ExcelUtil.generate("提出场合", headers, rows));
    }
}
