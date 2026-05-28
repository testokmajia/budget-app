package com.techmanage.service.impl;

import com.techmanage.dto.CreateUserRequest;
import com.techmanage.dto.DepartmentRequest;
import com.techmanage.dto.IssueCategoryRequest;
import com.techmanage.dto.IssueOccasionRequest;
import com.techmanage.dto.PageResponse;
import com.techmanage.dto.SystemInfoRequest;
import com.techmanage.dto.TeamRequest;
import com.techmanage.dto.UpdateUserRequest;
import com.techmanage.dto.UserResponse;
import com.techmanage.entity.Department;
import com.techmanage.entity.IssueCategory;
import com.techmanage.entity.IssueOccasion;
import com.techmanage.entity.Role;
import com.techmanage.entity.SystemConfig;
import com.techmanage.entity.SystemInfo;
import com.techmanage.entity.Team;
import com.techmanage.entity.User;
import com.techmanage.repository.DepartmentRepository;
import com.techmanage.repository.IssueCategoryRepository;
import com.techmanage.repository.IssueFeedbackRepository;
import com.techmanage.repository.IssueOccasionRepository;
import com.techmanage.repository.RoleRepository;
import com.techmanage.repository.SystemConfigRepository;
import com.techmanage.repository.SystemInfoRepository;
import com.techmanage.repository.TeamRepository;
import com.techmanage.repository.UserRepository;
import com.techmanage.service.AdminService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final IssueCategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;
    private final SystemInfoRepository systemInfoRepository;
    private final TeamRepository teamRepository;
    private final IssueOccasionRepository occasionRepository;
    private final IssueFeedbackRepository issueFeedbackRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           IssueCategoryRepository categoryRepository,
                           DepartmentRepository departmentRepository,
                           SystemInfoRepository systemInfoRepository,
                           TeamRepository teamRepository,
                           IssueOccasionRepository occasionRepository,
                           IssueFeedbackRepository issueFeedbackRepository,
                           SystemConfigRepository systemConfigRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.categoryRepository = categoryRepository;
        this.departmentRepository = departmentRepository;
        this.systemInfoRepository = systemInfoRepository;
        this.teamRepository = teamRepository;
        this.occasionRepository = occasionRepository;
        this.issueFeedbackRepository = issueFeedbackRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResponse<UserResponse> listUsers(int page, int size, String username, String name, String department, Boolean enabled) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (username != null && !username.isBlank()) {
                predicates.add(cb.like(root.get("username"), "%" + username.trim() + "%"));
            }
            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(root.get("name"), "%" + name.trim() + "%"));
            }
            if (department != null && !department.isBlank()) {
                predicates.add(cb.equal(root.get("department"), department));
            }
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<User> userPage = userRepository.findAll(spec, pageable);
        List<UserResponse> content = userPage.getContent().stream()
            .map(this::toUserResponse)
            .toList();
        return PageResponse.of(content, userPage.getTotalElements(), page, size);
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
    @Transactional
    public IssueCategory updateCategory(Long id, IssueCategoryRequest request) {
        IssueCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("分类不存在"));
        String oldName = category.getName();
        category.setName(request.name());
        category.setSortOrder(request.sortOrder());
        category.setEnabled(request.enabled());
        IssueCategory saved = categoryRepository.save(category);
        if (!request.name().equals(oldName)) {
            issueFeedbackRepository.updateIssueType(oldName, request.name());
        }
        return saved;
    }

    @Override
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    @Override
    public List<Department> listDepartments() {
        return departmentRepository.findAllByOrderByIdAsc();
    }

    @Override
    public Department createDepartment(DepartmentRequest request) {
        Department department = new Department();
        department.setName(request.name());
        department.setLeader(request.leader());
        department.setEnabled(request.enabled());
        return departmentRepository.save(department);
    }

    @Override
    @Transactional
    public Department updateDepartment(Long id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("部门不存在"));
        String oldName = department.getName();
        department.setName(request.name());
        department.setLeader(request.leader());
        department.setEnabled(request.enabled());
        Department saved = departmentRepository.save(department);
        if (!request.name().equals(oldName)) {
            issueFeedbackRepository.updateSubmitterDepartment(oldName, request.name());
            userRepository.updateDepartment(oldName, request.name());
        }
        return saved;
    }

    @Override
    public void deleteDepartment(Long id) {
        departmentRepository.deleteById(id);
    }

    @Override
    public List<SystemInfo> listSystems() {
        return systemInfoRepository.findAllByOrderByIdAsc();
    }

    @Override
    public SystemInfo createSystem(SystemInfoRequest request) {
        SystemInfo system = new SystemInfo();
        system.setName(request.name());
        system.setLeader(request.leader());
        system.setTeam(request.team());
        system.setEnabled(request.enabled());
        return systemInfoRepository.save(system);
    }

    @Override
    public SystemInfo updateSystem(Long id, SystemInfoRequest request) {
        SystemInfo system = systemInfoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("系统不存在"));
        system.setName(request.name());
        system.setLeader(request.leader());
        system.setTeam(request.team());
        system.setEnabled(request.enabled());
        return systemInfoRepository.save(system);
    }

    @Override
    public void deleteSystem(Long id) {
        systemInfoRepository.deleteById(id);
    }

    @Override
    public List<Team> listTeams() {
        return teamRepository.findAllByOrderByIdAsc();
    }

    @Override
    public Team createTeam(TeamRequest request) {
        Team team = new Team();
        team.setName(request.name());
        team.setDepartment(request.department());
        team.setLeader(request.leader());
        team.setMembers(request.members());
        team.setSystems(request.systems());
        team.setSystemOwners(request.systemOwners());
        team.setEnabled(request.enabled());
        return teamRepository.save(team);
    }

    @Override
    public Team updateTeam(Long id, TeamRequest request) {
        Team team = teamRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("团队不存在"));
        team.setName(request.name());
        team.setDepartment(request.department());
        team.setLeader(request.leader());
        team.setMembers(request.members());
        team.setSystems(request.systems());
        team.setSystemOwners(request.systemOwners());
        team.setEnabled(request.enabled());
        return teamRepository.save(team);
    }

    @Override
    public void deleteTeam(Long id) {
        teamRepository.deleteById(id);
    }

    @Override
    public List<IssueOccasion> listOccasions() {
        return occasionRepository.findAllByOrderByIdAsc();
    }

    @Override
    public IssueOccasion createOccasion(IssueOccasionRequest request) {
        IssueOccasion occasion = new IssueOccasion();
        occasion.setName(request.name());
        occasion.setType(request.type());
        occasion.setEnabled(request.enabled());
        return occasionRepository.save(occasion);
    }

    @Override
    public IssueOccasion updateOccasion(Long id, IssueOccasionRequest request) {
        IssueOccasion occasion = occasionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("场合不存在"));
        occasion.setName(request.name());
        occasion.setType(request.type());
        occasion.setEnabled(request.enabled());
        return occasionRepository.save(occasion);
    }

    @Override
    public void deleteOccasion(Long id) {
        occasionRepository.deleteById(id);
    }

    @Override
    public List<SystemConfig> listConfigs() {
        return systemConfigRepository.findAll();
    }

    @Override
    public SystemConfig saveConfig(String configKey, String configValue, String description) {
        var existing = systemConfigRepository.findByConfigKey(configKey);
        SystemConfig config;
        if (existing.isPresent()) {
            config = existing.get();
        } else {
            config = new SystemConfig();
            config.setConfigKey(configKey);
        }
        config.setConfigValue(configValue);
        if (description != null) config.setDescription(description);
        return systemConfigRepository.save(config);
    }

    @Override
    public void deleteConfig(Long id) {
        systemConfigRepository.deleteById(id);
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
