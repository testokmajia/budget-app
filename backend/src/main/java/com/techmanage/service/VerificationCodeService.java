package com.techmanage.service;

/**
 * 密码重置验证码服务
 */
public interface VerificationCodeService {

    /**
     * 步骤1：发送验证码到指定邮箱
     *
     * @param email    邮箱地址
     * @param clientIp 客户端IP（用于频率限制）
     */
    void sendCode(String email, String clientIp);

    /**
     * 步骤2：校验验证码
     *
     * @param email 邮箱地址
     * @param code  6位验证码
     * @return 重置令牌，用于步骤3
     */
    String verifyCode(String email, String code);

    /**
     * 步骤3：使用重置令牌完成密码重置
     *
     * @param email       邮箱地址
     * @param resetToken  重置令牌（步骤2返回）
     * @param newPassword 新密码
     */
    void resetPassword(String email, String resetToken, String newPassword);
}
