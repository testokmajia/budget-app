package com.techmanage.security;

import com.techmanage.entity.User;
import com.techmanage.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String loginName) throws UsernameNotFoundException {
        // 1. 按用户名（username）查找
        Optional<User> userOpt = userRepository.findByUsername(loginName);
        if (userOpt.isPresent()) {
            return userOpt.get();
        }

        // 2. 按完整邮箱地址查找
        if (loginName.contains("@")) {
            userOpt = userRepository.findByEmail(loginName);
            if (userOpt.isPresent()) {
                return userOpt.get();
            }
        }

        // 3. 按邮箱前缀（@之前的部分）查找
        List<User> users = userRepository.findByEmailLocalPart(loginName);
        if (users.size() == 1) {
            return users.get(0);
        } else if (users.size() > 1) {
            log.warn("邮箱前缀 '{}' 匹配到多个用户，请使用完整邮箱或用户名登录", loginName);
            throw new UsernameNotFoundException("该账号匹配到多个用户，请使用完整邮箱或用户名登录");
        }

        // 4. 按手机号查找
        userOpt = userRepository.findByPhone(loginName);
        if (userOpt.isPresent()) {
            return userOpt.get();
        }

        throw new UsernameNotFoundException("用户不存在: " + loginName);
    }
}
