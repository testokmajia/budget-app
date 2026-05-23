package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

public record WechatLoginRequest(
    @NotBlank String sessionId,
    @NotBlank String code
) {}
