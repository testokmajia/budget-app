package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

public record WechatBindRequest(
    @NotBlank(message = "会话ID不能为空") String sessionId,
    @NotBlank(message = "用户名不能为空") String username,
    @NotBlank(message = "密码不能为空") String password
) {}
