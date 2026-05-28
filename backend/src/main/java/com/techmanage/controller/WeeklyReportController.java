package com.techmanage.controller;

import com.techmanage.common.ApiResponse;
import com.techmanage.dto.ReviewRequest;
import com.techmanage.dto.SmartDraftResponse;
import com.techmanage.dto.WeeklyReportRequest;
import com.techmanage.dto.WeeklyReportResponse;
import com.techmanage.service.WeeklyReportService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/weekly-reports")
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    public WeeklyReportController(WeeklyReportService weeklyReportService) {
        this.weeklyReportService = weeklyReportService;
    }

    @GetMapping("/current")
    public ApiResponse<WeeklyReportResponse> getCurrentWeek(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(weeklyReportService.getCurrentWeek(userId));
    }

    @PostMapping
    public ApiResponse<WeeklyReportResponse> save(Authentication auth,
                                                  @Valid @RequestBody WeeklyReportRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(weeklyReportService.save(userId, request));
    }

    @PostMapping("/submit")
    public ApiResponse<WeeklyReportResponse> submit(Authentication auth,
                                                    @Valid @RequestBody WeeklyReportRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(weeklyReportService.submit(userId, request));
    }

    @GetMapping("/smart-draft")
    public ApiResponse<SmartDraftResponse> getSmartDraft(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(weeklyReportService.getSmartDraft(userId));
    }

    @GetMapping("/my")
    public ApiResponse<List<WeeklyReportResponse>> listMyReports(
            Authentication auth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(weeklyReportService.listMyReports(userId, from, to));
    }

    @GetMapping("/pending")
    public ApiResponse<List<WeeklyReportResponse>> listPending(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(weeklyReportService.listPendingForLeader(userId));
    }

    @GetMapping("/pending/count")
    public ApiResponse<Long> countPending(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(weeklyReportService.countPendingForLeader(userId));
    }

    @PostMapping("/{id:\\d+}/approve")
    public ApiResponse<WeeklyReportResponse> approve(@PathVariable Long id,
                                                     Authentication auth,
                                                     @RequestBody(required = false) ReviewRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(weeklyReportService.approve(id, userId, request != null ? request : new ReviewRequest(null)));
    }

    @PostMapping("/{id:\\d+}/reject")
    public ApiResponse<WeeklyReportResponse> reject(@PathVariable Long id,
                                                    Authentication auth,
                                                    @Valid @RequestBody ReviewRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(weeklyReportService.reject(id, userId, request));
    }

    @GetMapping("/team")
    public ApiResponse<List<WeeklyReportResponse>> listTeamHistory(
            Authentication auth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(weeklyReportService.listTeamHistory(userId, from, to));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ROLE_CLERK', 'ROLE_ADMIN')")
    public ApiResponse<List<WeeklyReportResponse>> listAllHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(weeklyReportService.listAllHistory(from, to));
    }
}
