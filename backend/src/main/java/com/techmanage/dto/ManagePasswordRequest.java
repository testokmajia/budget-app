package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 管理口令验证请求
 */
public record ManagePasswordRequest(
    @NotBlank(message = "口令不能为空")
    String password
) {}
