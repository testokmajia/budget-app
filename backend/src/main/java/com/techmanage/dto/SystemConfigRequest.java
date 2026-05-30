package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

public record SystemConfigRequest(
    @NotBlank(message = "配置键不能为空") String configKey,
    @NotBlank(message = "配置值不能为空") String configValue,
    String description
) {}
