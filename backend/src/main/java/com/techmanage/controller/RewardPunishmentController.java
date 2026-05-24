package com.techmanage.controller;

import com.techmanage.common.ApiResponse;
import com.techmanage.dto.RewardPunishmentRequest;
import com.techmanage.dto.RewardPunishmentResponse;
import com.techmanage.service.RewardPunishmentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rewards")
public class RewardPunishmentController {

    private final RewardPunishmentService service;

    public RewardPunishmentController(RewardPunishmentService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<RewardPunishmentResponse>> list(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        boolean canViewAll = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                || a.getAuthority().equals("ROLE_CLERK"));
        return ApiResponse.ok(service.list(userId, canViewAll));
    }

    @GetMapping("/{id}")
    public ApiResponse<RewardPunishmentResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_CLERK', 'ROLE_ADMIN')")
    public ApiResponse<RewardPunishmentResponse> create(Authentication auth,
                                                        @Valid @RequestBody RewardPunishmentRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(service.create(userId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_CLERK', 'ROLE_ADMIN')")
    public ApiResponse<RewardPunishmentResponse> update(@PathVariable Long id,
                                                        Authentication auth,
                                                        @Valid @RequestBody RewardPunishmentRequest request) {
        Long userId = (Long) auth.getPrincipal();
        boolean canEditAll = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                || a.getAuthority().equals("ROLE_CLERK"));
        return ApiResponse.ok(service.update(id, userId, canEditAll, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_CLERK', 'ROLE_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        boolean canEditAll = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                || a.getAuthority().equals("ROLE_CLERK"));
        service.delete(id, userId, canEditAll);
        return ApiResponse.ok();
    }
}
