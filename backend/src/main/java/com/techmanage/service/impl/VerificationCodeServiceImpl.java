package com.techmanage.service.impl;

import com.techmanage.common.BusinessException;
import com.techmanage.entity.VerificationCode;
import com.techmanage.entity.User;
import com.techmanage.repository.VerificationCodeRepository;
import com.techmanage.repository.UserRepository;
import com.techmanage.service.EmailService;
import com.techmanage.service.VerificationCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 密码重置验证码服务实现 — 包含所有安全策略
 */
@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private static final Logger log = LoggerFactory.getLogger(VerificationCodeServiceImpl.class);

    /** 验证码有效期（分钟） */
    private static final int CODE_EXPIRE_MINUTES = 10;
    /** 最大错误尝试次数 */
    private static final int MAX_ATTEMPTS = 5;
    /** 发送间隔（秒） */
    private static final int SEND_INTERVAL_SECONDS = 60;
    /** IP每分钟最大请求数 */
    private static final int IP_MAX_REQUESTS_PER_MINUTE = 3;

    private final VerificationCodeRepository verificationCodeRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public VerificationCodeServiceImpl(
            VerificationCodeRepository verificationCodeRepository,
            UserRepository userRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {
        this.verificationCodeRepository = verificationCodeRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void sendCode(String email, String clientIp) {
        // 1. 检查60秒内是否已发送
        Optional<VerificationCode> latestCode =
            verificationCodeRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email);
        if (latestCode.isPresent()) {
            LocalDateTime lastSent = latestCode.get().getCreatedAt();
            if (lastSent != null && lastSent.plusSeconds(SEND_INTERVAL_SECONDS).isAfter(LocalDateTime.now())) {
                throw new BusinessException("发送过于频繁，请60秒后再试");
            }
        }

        // 2. IP频率限制：同一IP每分钟最多3条
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        long recentCount = verificationCodeRepository.countByCreatedAtAfter(oneMinuteAgo);
        if (recentCount >= IP_MAX_REQUESTS_PER_MINUTE) {
            log.warn("IP {} 发送验证码过于频繁，最近1分钟记录数: {}", clientIp, recentCount);
            throw new BusinessException("操作过于频繁，请稍后再试");
        }

        // 3. 生成6位随机验证码（ThreadLocalRandom，安全随机）
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));

        // 4. 保存验证码记录（无论邮箱是否存在，统一保存以防范枚举攻击）
        VerificationCode vc = new VerificationCode();
        vc.setEmail(email);
        vc.setCode(code);
        vc.setUsed(false);
        vc.setAttempts(0);
        verificationCodeRepository.save(vc);

        // 5. 仅当邮箱对应的用户存在时才实际发送邮件
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            try {
                emailService.sendVerificationCode(email, code);
                log.info("密码重置验证码已发送至: {}", email);
            } catch (Exception e) {
                log.error("邮件发送失败: {}", e.getMessage(), e);
                throw new BusinessException("邮件发送失败：" + e.getMessage());
            }
        } else {
            log.info("邮箱未注册，跳过发送验证码: {}", email);
        }
        // 无论是否发送，统一返回成功（防邮箱枚举攻击）
    }

    @Override
    @Transactional
    public String verifyCode(String email, String code) {
        VerificationCode vc = verificationCodeRepository
            .findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email)
            .orElseThrow(() -> new BusinessException("验证码错误或已过期"));

        // 检查是否过期
        if (vc.isExpired()) {
            throw new BusinessException("验证码已过期，请重新获取");
        }

        // 检查是否锁定
        if (vc.isLocked()) {
            throw new BusinessException("验证码错误次数过多，请重新获取");
        }

        // 验证码不匹配
        if (!vc.getCode().equals(code)) {
            vc.setAttempts(vc.getAttempts() + 1);
            verificationCodeRepository.save(vc);
            int remaining = MAX_ATTEMPTS - vc.getAttempts();
            if (remaining <= 0) {
                throw new BusinessException("验证码错误次数过多，请重新获取");
            }
            throw new BusinessException("验证码错误，剩余尝试次数：" + remaining);
        }

        // 匹配成功 — 标记为已使用，生成重置令牌
        vc.setUsed(true);
        String resetToken = UUID.randomUUID().toString();
        vc.setResetToken(resetToken);
        verificationCodeRepository.save(vc);

        return resetToken;
    }

    @Override
    @Transactional
    public void resetPassword(String email, String resetToken, String newPassword) {
        // 1. 查找验证记录
        VerificationCode vc = verificationCodeRepository
            .findTopByEmailAndResetTokenOrderByCreatedAtDesc(email, resetToken)
            .orElseThrow(() -> new BusinessException("重置链接已失效，请重新操作"));

        // 2. 查找用户
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException("用户不存在"));

        // 3. 密码强度校验（二次保障，前端+DTO已校验）
        if (newPassword == null || newPassword.length() < 8) {
            throw new BusinessException("密码至少8位");
        }
        if (!newPassword.matches("^(?=.*[a-zA-Z])(?=.*[0-9]).+$")) {
            throw new BusinessException("密码必须包含字母和数字");
        }

        // 4. 加密并更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 5. 清除resetToken，防止重放
        vc.setResetToken(null);
        verificationCodeRepository.save(vc);

        log.info("用户 {} 密码重置成功", user.getUsername());
    }

    /** 定时清理过期验证码记录（每小时执行一次） */
    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void cleanupExpiredCodes() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        int deleted = verificationCodeRepository.deleteByCreatedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("已清理 {} 条过期验证码记录", deleted);
        }
    }
}
