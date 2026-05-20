package com.techmanage.controller;

import com.techmanage.common.ApiResponse;
import com.techmanage.dto.*;
import com.techmanage.entity.IssueCategory;
import com.techmanage.entity.Role;
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
    public ApiResponse<List<UserResponse>> listUsers() {
        return ApiResponse.ok(adminService.listUsers());
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
}
