package com.techmanage.config;

import com.techmanage.entity.IssueOccasion;
import com.techmanage.entity.Role;
import com.techmanage.entity.User;
import com.techmanage.repository.IssueOccasionRepository;
import com.techmanage.repository.RoleRepository;
import com.techmanage.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final IssueOccasionRepository occasionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                          UserRepository userRepository,
                          IssueOccasionRepository occasionRepository,
                          PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.occasionRepository = occasionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initRoles();
        initAdminUser();
        initOccasions();
    }

    private void initRoles() {
        createRoleIfAbsent("ROLE_USER", "普通用户", "默认角色，使用清单管理、查询奖惩、提交问题");
        createRoleIfAbsent("ROLE_CLERK", "部门文书", "管理部门奖惩记录");
        createRoleIfAbsent("ROLE_ISSUE_ADMIN", "问题管理员", "问题分派、分类管理");
        createRoleIfAbsent("ROLE_ADMIN", "系统管理员", "用户管理、角色分配、系统配置");
    }

    private void createRoleIfAbsent(String code, String name, String desc) {
        if (roleRepository.findByCode(code).isEmpty()) {
            roleRepository.save(new Role(code, name, desc));
            log.info("角色已初始化: {}", code);
        }
    }

    private void initAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            Role adminRole = roleRepository.findByCode("ROLE_ADMIN")
                .orElseThrow();
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123456"));
            admin.setName("系统管理员");
            admin.setDepartment("信息科技部");
            admin.setRoles(Set.of(adminRole));
            userRepository.save(admin);
            log.info("管理员账号已初始化");
        }
    }

    private void initOccasions() {
        String[][] data = {
            {"20260515吐槽大会-信息科技部", "MEETING"},
            {"业务协调会", "MEETING"},
            {"线上提出", "GENERAL"},
            {"其他", "GENERAL"}
        };
        for (String[] item : data) {
            if (!occasionRepository.existsByName(item[0])) {
                var o = new IssueOccasion();
                o.setName(item[0]);
                o.setType(item[1]);
                o.setEnabled(true);
                occasionRepository.save(o);
                log.info("提出场合已初始化: {}", item[0]);
            }
        }
    }
}
