package com.techmanage.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 密码重置验证码实体
 */
@Entity
@Table(name = "verification_codes")
public class VerificationCode extends BaseEntity {

    /** 邮箱地址 */
    @Column(nullable = false, length = 128)
    private String email;

    /** 6位数字验证码 */
    @Column(nullable = false, length = 6)
    private String code;

    /** 是否已使用 */
    @Column(nullable = false)
    private boolean used = false;

    /** 错误尝试次数 */
    @Column(nullable = false)
    private int attempts = 0;

    /** 验证通过后生成的令牌，用于重置密码步骤 */
    @Column(length = 64)
    private String resetToken;

    // getters/setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    /** 验证码是否已过期（10分钟有效） */
    public boolean isExpired() {
        return getCreatedAt() != null
            && getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }

    /** 是否已被锁定（5次错误尝试） */
    public boolean isLocked() {
        return attempts >= 5;
    }
}
