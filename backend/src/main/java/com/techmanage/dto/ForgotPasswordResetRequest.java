package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 忘记密码流程 — 重置密码请求
 */
public record ForgotPasswordResetRequest(
    @NotBlank(message = "邮箱不能为空")
    String email,

    @NotBlank(message = "验证令牌不能为空")
    String resetToken,

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 100, message = "密码至少8位")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9]).+$", message = "密码必须包含字母和数字")
    String newPassword
) {}
