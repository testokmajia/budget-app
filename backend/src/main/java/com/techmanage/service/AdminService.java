package com.techmanage.service;

import com.techmanage.dto.CreateUserRequest;
import com.techmanage.dto.IssueCategoryRequest;
import com.techmanage.dto.UpdateUserRequest;
import com.techmanage.dto.UserResponse;
import com.techmanage.entity.IssueCategory;
import com.techmanage.entity.Role;

import java.util.List;

public interface AdminService {
    // 用户管理
    List<UserResponse> listUsers();
    UserResponse createUser(CreateUserRequest request);
    UserResponse updateUser(Long userId, UpdateUserRequest request);
    UserResponse toggleUserEnabled(Long userId);
    void resetPassword(Long userId, String password);
    List<Role> listRoles();

    // 问题分类管理
    List<IssueCategory> listCategories();
    IssueCategory createCategory(IssueCategoryRequest request);
    IssueCategory updateCategory(Long id, IssueCategoryRequest request);
    void deleteCategory(Long id);
}
