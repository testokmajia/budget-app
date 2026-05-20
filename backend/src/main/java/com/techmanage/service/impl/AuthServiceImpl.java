package com.techmanage.service.impl;

import com.techmanage.dto.LoginRequest;
import com.techmanage.dto.LoginResponse;
import com.techmanage.dto.RegisterRequest;
import com.techmanage.entity.Role;
import com.techmanage.entity.User;
import com.techmanage.repository.RoleRepository;
import com.techmanage.repository.UserRepository;
import com.techmanage.security.JwtTokenProvider;
import com.techmanage.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(UserRepository userRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        var auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = (User) auth.getPrincipal();
        return buildResponse(user);
    }

    @Override
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new RuntimeException("用户名已存在");
        }

        Role userRole = roleRepository.findByCode("ROLE_USER")
            .orElseThrow(() -> new RuntimeException("默认角色未初始化"));

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setName(request.name());
        user.setDepartment(request.department());
        user.setPosition(request.position());
        user.setPhone(request.phone());
        user.setRoles(Set.of(userRole));
        userRepository.save(user);

        return buildResponse(user);
    }

    private LoginResponse buildResponse(User user) {
        if (!user.isEnabled()) {
            throw new BadCredentialsException("账号已被禁用");
        }

        var roles = user.getRoles().stream()
            .map(Role::getCode)
            .toList();

        var token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), roles);

        var userInfo = new LoginResponse.UserInfo(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getDepartment(),
            user.getPosition(),
            roles
        );

        return new LoginResponse(token, userInfo);
    }
}
