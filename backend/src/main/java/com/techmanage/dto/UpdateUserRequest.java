package com.techmanage.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpdateUserRequest(
    @NotBlank String name,
    String department,
    String position,
    String phone,
    boolean enabled,
    List<Long> roleIds
) {}
