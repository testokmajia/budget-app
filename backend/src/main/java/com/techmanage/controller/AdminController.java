package com.techmanage.controller;

import com.techmanage.common.ApiResponse;
import com.techmanage.dto.*;
import com.techmanage.entity.Department;
import com.techmanage.entity.IssueCategory;
import com.techmanage.entity.Role;
import com.techmanage.entity.SystemInfo;
import com.techmanage.entity.Team;
import com.techmanage.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_ISSUE_ADMIN')")
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
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_ISSUE_ADMIN')")
    public ApiResponse<List<Role>> listRoles() {
        return ApiResponse.ok(adminService.listRoles());
    }

    // === 问题分类管理 ===

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_ISSUE_ADMIN')")
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
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_ISSUE_ADMIN')")
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
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_ISSUE_ADMIN')")
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
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_ISSUE_ADMIN')")
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
}
