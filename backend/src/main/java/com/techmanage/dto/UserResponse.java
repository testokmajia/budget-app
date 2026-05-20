package com.techmanage.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
    Long id,
    String username,
    String name,
    String department,
    String position,
    String phone,
    boolean enabled,
    List<RoleInfo> roles,
    LocalDateTime createdAt
) {
    public record RoleInfo(Long id, String code, String name) {}
}
