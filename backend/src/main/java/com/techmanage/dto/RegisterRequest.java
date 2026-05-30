package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "用户名不能为空") @Size(min = 3, max = 50, message = "用户名长度3-50位") String username,
    @NotBlank(message = "密码不能为空") @Size(min = 8, max = 100, message = "密码至少8位")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9]).+$", message = "密码必须包含字母和数字")
    String password,
    @NotBlank(message = "姓名不能为空") String name,
    String department,
    String position,
    String phone
) {}
