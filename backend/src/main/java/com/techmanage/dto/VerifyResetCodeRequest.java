package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 校验密码重置验证码请求
 */
public record VerifyResetCodeRequest(
    @NotBlank(message = "邮箱不能为空")
    String email,

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码为6位数字")
    String code
) {}
