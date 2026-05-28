package com.techmanage.controller;

import com.techmanage.common.ApiResponse;
import com.techmanage.dto.ChecklistRequest;
import com.techmanage.dto.ChecklistResponse;
import com.techmanage.service.ChecklistService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/checklists")
public class ChecklistController {

    private final ChecklistService checklistService;

    public ChecklistController(ChecklistService checklistService) {
        this.checklistService = checklistService;
    }

    @GetMapping
    public ApiResponse<List<ChecklistResponse>> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String responsiblePerson,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(checklistService.list(userId, status, keyword, responsiblePerson, startDate, endDate));
    }

    @GetMapping("/{id}")
    public ApiResponse<ChecklistResponse> getById(@PathVariable Long id,
                                                  @AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(checklistService.getById(id, userId));
    }

    @PostMapping
    public ApiResponse<ChecklistResponse> create(@AuthenticationPrincipal Long userId,
                                                 @Valid @RequestBody ChecklistRequest request) {
        return ApiResponse.ok(checklistService.create(userId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ChecklistResponse> update(@PathVariable Long id,
                                                 @AuthenticationPrincipal Long userId,
                                                 @Valid @RequestBody ChecklistRequest request) {
        return ApiResponse.ok(checklistService.update(id, userId, request));
    }

    @PutMapping("/{id}/complete")
    public ApiResponse<ChecklistResponse> complete(@PathVariable Long id,
                                                   @AuthenticationPrincipal Long userId,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate actualDate) {
        return ApiResponse.ok(checklistService.complete(id, userId, actualDate));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id,
                                     @AuthenticationPrincipal Long userId) {
        checklistService.delete(id, userId);
        return ApiResponse.ok();
    }
}
