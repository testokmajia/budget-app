package com.techmanage.service.impl;

import com.techmanage.repository.SystemConfigRepository;
import com.techmanage.service.EmailService;
import com.techmanage.util.EncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * 邮件发送实现 — 配置优先级：数据库系统配置 > application.yml 默认值
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final String fallbackHost;
    private final int fallbackPort;
    private final String fallbackUsername;
    private final String fallbackPassword;
    private final String fallbackFrom;
    private final SystemConfigRepository systemConfigRepository;
    private final EncryptionUtil encryptionUtil;

    public EmailServiceImpl(
            @Value("${app.mail.host:smtp.163.com}") String host,
            @Value("${app.mail.port:465}") int port,
            @Value("${app.mail.username:}") String username,
            @Value("${app.mail.password:}") String password,
            @Value("${app.mail.from:}") String from,
            SystemConfigRepository systemConfigRepository,
            EncryptionUtil encryptionUtil) {
        this.fallbackHost = host;
        this.fallbackPort = port;
        this.fallbackUsername = username;
        this.fallbackPassword = password;
        this.fallbackFrom = from;
        this.systemConfigRepository = systemConfigRepository;
        this.encryptionUtil = encryptionUtil;
    }

    // ===== 配置解析方法（数据库优先，回退到 application.yml） =====

    private String resolveHost() {
        return resolveConfig("mail.host", fallbackHost);
    }

    private int resolvePort() {
        String val = resolveConfig("mail.port", String.valueOf(fallbackPort));
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return fallbackPort;
        }
    }

    private String resolveUsername() {
        return resolveConfig("mail.username", fallbackUsername);
    }

    private String resolvePassword() {
        return resolveConfig("mail.password", fallbackPassword);
    }

    private String resolveFrom() {
        String from = resolveConfig("mail.from", fallbackFrom);
        if (from == null || from.isBlank()) {
            from = resolveUsername();
        }
        return from;
    }

    /** 从数据库读取配置并尝试解密，解密失败则当作明文（兼容旧数据） */
    private String resolveConfig(String key, String fallback) {
        var dbConfig = systemConfigRepository.findByConfigKey(key);
        if (dbConfig.isPresent() && dbConfig.get().getConfigValue() != null
            && !dbConfig.get().getConfigValue().isBlank()) {
            try {
                return encryptionUtil.decrypt(dbConfig.get().getConfigValue());
            } catch (Exception e) {
                // 解密失败，当作明文返回（兼容旧未加密数据）
                log.debug("配置 {} 解密失败，按明文处理", key);
                return dbConfig.get().getConfigValue();
            }
        }
        return fallback;
    }

    // ===== 创建 JavaMailSender =====

    private JavaMailSender createMailSender() {
        String host = resolveHost();
        int port = resolvePort();
        String username = resolveUsername();
        String password = resolvePassword();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("邮件服务未配置，请在系统管理 → 系统配置中设置 mail.username 和 mail.password");
        }

        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        impl.setHost(host);
        impl.setPort(port);
        impl.setUsername(username);
        impl.setPassword(password);

        Properties props = impl.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.ssl.checkserveridentity", "false");
        props.put("mail.smtp.starttls.enable", "false");

        return impl;
    }

    @Override
    public void sendVerificationCode(String to, String code) {
        JavaMailSender mailSender = createMailSender();
        String from = resolveFrom();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("【科技管理平台】密码重置验证码");
        message.setText(String.format("""
                您好！

                您正在重置科技管理平台的登录密码。
                验证码：%s
                有效期：10分钟

                如非本人操作，请忽略此邮件。

                科技管理平台
                """, code));

        mailSender.send(message);
        log.info("验证码邮件已发送至: {}", to);
    }
}
