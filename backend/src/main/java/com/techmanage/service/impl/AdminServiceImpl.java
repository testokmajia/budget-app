package com.techmanage.service.impl;

import com.techmanage.dto.CreateUserRequest;
import com.techmanage.dto.IssueCategoryRequest;
import com.techmanage.dto.UpdateUserRequest;
import com.techmanage.dto.UserResponse;
import com.techmanage.entity.IssueCategory;
import com.techmanage.entity.Role;
import com.techmanage.entity.User;
import com.techmanage.repository.IssueCategoryRepository;
import com.techmanage.repository.RoleRepository;
import com.techmanage.repository.UserRepository;
import com.techmanage.service.AdminService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final IssueCategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           IssueCategoryRepository categoryRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
            .map(this::toUserResponse)
            .toList();
    }

    @Override
    public UserResponse toggleUserEnabled(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        if ("admin".equals(user.getUsername())) {
            throw new RuntimeException("不能禁用超级管理员");
        }
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        return toUserResponse(user);
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setName(request.name());
        user.setDepartment(request.department());
        user.setPosition(request.position());
        user.setPhone(request.phone());
        user.setEnabled(true);
        userRepository.save(user);
        return toUserResponse(user);
    }

    @Override
    public void resetPassword(Long userId, String password) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    @Override
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setName(request.name());
        user.setDepartment(request.department());
        user.setPosition(request.position());
        user.setPhone(request.phone());
        user.setEnabled(request.enabled());
        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            List<Role> roles = roleRepository.findAllById(request.roleIds());
            user.setRoles(new HashSet<>(roles));
        }
        userRepository.save(user);
        return toUserResponse(user);
    }

    @Override
    public List<Role> listRoles() {
        return roleRepository.findAll();
    }

    @Override
    public List<IssueCategory> listCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc();
    }

    @Override
    public IssueCategory createCategory(IssueCategoryRequest request) {
        IssueCategory category = new IssueCategory();
        category.setName(request.name());
        category.setSortOrder(request.sortOrder());
        category.setEnabled(request.enabled());
        return categoryRepository.save(category);
    }

    @Override
    public IssueCategory updateCategory(Long id, IssueCategoryRequest request) {
        IssueCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("分类不存在"));
        category.setName(request.name());
        category.setSortOrder(request.sortOrder());
        category.setEnabled(request.enabled());
        return categoryRepository.save(category);
    }

    @Override
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    private UserResponse toUserResponse(User user) {
        List<UserResponse.RoleInfo> roles = user.getRoles().stream()
            .map(r -> new UserResponse.RoleInfo(r.getId(), r.getCode(), r.getName()))
            .toList();
        return new UserResponse(
            user.getId(), user.getUsername(), user.getName(),
            user.getDepartment(), user.getPosition(), user.getPhone(),
            user.isEnabled(), roles, user.getCreatedAt()
        );
    }
}
