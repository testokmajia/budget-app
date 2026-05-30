package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpdateUserRequest(
    @NotBlank(message = "姓名不能为空") String name,
    String department,
    String position,
    String phone,
    boolean enabled,
    List<Long> roleIds
) {}
