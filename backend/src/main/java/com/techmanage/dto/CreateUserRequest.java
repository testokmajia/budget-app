package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank(message = "用户名不能为空") @Size(min = 3, max = 50, message = "用户名长度3-50位") String username,
    @NotBlank(message = "密码不能为空") @Size(min = 6, max = 100, message = "密码至少6位") String password,
    @NotBlank(message = "姓名不能为空") String name,
    String department,
    String position,
    String phone
) {}
