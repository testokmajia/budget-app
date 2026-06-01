package com.techmanage.service;

/**
 * 邮件发送服务
 */
public interface EmailService {

    /**
     * 发送密码重置验证码邮件
     *
     * @param to   收件人邮箱
     * @param code 6位验证码
     */
    void sendVerificationCode(String to, String code);
}
