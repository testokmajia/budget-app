package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

public record WechatBindRequest(
    @NotBlank String sessionId,
    @NotBlank String username,
    @NotBlank String password
) {}
