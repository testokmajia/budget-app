package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

public record SystemInfoRequest(
    @NotBlank(message = "系统编号不能为空") String code,
    @NotBlank(message = "系统名称不能为空") String name,
    String leader,
    String team,
    boolean enabled
) {}
