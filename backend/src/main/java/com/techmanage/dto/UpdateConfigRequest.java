package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新配置请求（含口令验证）
 */
public record UpdateConfigRequest(
    @NotBlank(message = "配置值不能为空")
    String configValue,

    String description,

    @NotBlank(message = "口令不能为空")
    String password
) {}
