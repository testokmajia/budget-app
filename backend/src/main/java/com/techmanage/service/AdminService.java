package com.techmanage.service;

import com.techmanage.dto.CreateUserRequest;
import com.techmanage.dto.DepartmentRequest;
import com.techmanage.dto.IssueCategoryRequest;
import com.techmanage.dto.PageResponse;
import com.techmanage.dto.SystemInfoRequest;
import com.techmanage.dto.TeamRequest;
import com.techmanage.dto.UpdateUserRequest;
import com.techmanage.dto.UserResponse;
import com.techmanage.entity.Department;
import com.techmanage.entity.IssueCategory;
import com.techmanage.entity.Role;
import com.techmanage.entity.SystemInfo;
import com.techmanage.entity.Team;

import java.util.List;

public interface AdminService {
    // 用户管理
    PageResponse<UserResponse> listUsers(int page, int size, String username, String name, String department, Boolean enabled);
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

    // 部门管理
    List<Department> listDepartments();
    Department createDepartment(DepartmentRequest request);
    Department updateDepartment(Long id, DepartmentRequest request);
    void deleteDepartment(Long id);

    // 所属系统管理
    List<SystemInfo> listSystems();
    SystemInfo createSystem(SystemInfoRequest request);
    SystemInfo updateSystem(Long id, SystemInfoRequest request);
    void deleteSystem(Long id);

    // 团队管理
    List<Team> listTeams();
    Team createTeam(TeamRequest request);
    Team updateTeam(Long id, TeamRequest request);
    void deleteTeam(Long id);
}
