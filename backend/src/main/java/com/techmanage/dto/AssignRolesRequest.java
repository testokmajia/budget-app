package com.techmanage.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AssignRolesRequest(
    @NotEmpty(message = "角色列表不能为空") List<Long> roleIds
) {}
