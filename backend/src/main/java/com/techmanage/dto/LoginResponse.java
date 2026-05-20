package com.techmanage.dto;

import java.util.List;

public record LoginResponse(
    String token,
    UserInfo user
) {
    public record UserInfo(
        Long id,
        String username,
        String name,
        String department,
        String position,
        List<String> roles
    ) {}
}
