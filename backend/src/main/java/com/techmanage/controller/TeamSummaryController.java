package com.techmanage.controller;

import com.techmanage.common.ApiResponse;
import com.techmanage.dto.TeamInfo;
import com.techmanage.dto.TeamSummaryRequest;
import com.techmanage.dto.TeamSummaryResponse;
import com.techmanage.service.TeamSummaryService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/team-summaries")
public class TeamSummaryController {

    private final TeamSummaryService teamSummaryService;

    public TeamSummaryController(TeamSummaryService teamSummaryService) {
        this.teamSummaryService = teamSummaryService;
    }

    @PostMapping("/merge")
    public ApiResponse<TeamSummaryResponse> mergeAi(Authentication auth,
                                                     @Valid @RequestBody TeamSummaryRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(teamSummaryService.mergeAi(userId, request));
    }

    @PutMapping("/{id:\\d+}")
    public ApiResponse<TeamSummaryResponse> update(@PathVariable Long id,
                                                    Authentication auth,
                                                    @RequestBody Map<String, String> body) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(teamSummaryService.update(id, userId, body.get("editedContent")));
    }

    @PostMapping("/{id:\\d+}/submit")
    public ApiResponse<TeamSummaryResponse> submit(@PathVariable Long id,
                                                    Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(teamSummaryService.submit(id, userId));
    }

    @GetMapping("/my")
    public ApiResponse<List<TeamSummaryResponse>> listMy(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(teamSummaryService.listByLeader(userId));
    }

    @GetMapping("/my/teams")
    public ApiResponse<List<TeamInfo>> getMyTeams(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(teamSummaryService.getMyTeams(userId));
    }

    @GetMapping("/by-week")
    @PreAuthorize("hasAnyRole('ROLE_CLERK', 'ROLE_ADMIN')")
    public ApiResponse<List<TeamSummaryResponse>> listByWeek(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate) {
        return ApiResponse.ok(teamSummaryService.listByWeek(weekStartDate));
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<TeamSummaryResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(teamSummaryService.getById(id));
    }
}
