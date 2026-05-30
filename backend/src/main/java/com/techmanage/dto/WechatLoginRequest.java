package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

public record WechatLoginRequest(
    @NotBlank(message = "会话ID不能为空") String sessionId,
    @NotBlank(message = "授权码不能为空") String code
) {}
