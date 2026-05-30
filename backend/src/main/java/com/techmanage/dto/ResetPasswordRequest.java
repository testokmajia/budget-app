package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank(message = "新密码不能为空") @Size(min = 6, max = 100, message = "密码至少6位") String password
) {}
