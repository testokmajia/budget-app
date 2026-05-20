package com.techmanage.controller;

import com.techmanage.common.ApiResponse;
import com.techmanage.dto.*;
import com.techmanage.entity.User;
import com.techmanage.repository.UserRepository;
import com.techmanage.service.IssueFeedbackService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/issues")
public class IssueFeedbackController {

    private final IssueFeedbackService issueService;
    private final UserRepository userRepository;

    public IssueFeedbackController(IssueFeedbackService issueService, UserRepository userRepository) {
        this.issueService = issueService;
        this.userRepository = userRepository;
    }

    private User currentUser(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return userRepository.findById(userId).orElseThrow();
    }

    private boolean isAdmin(User u) {
        return u.getRoles().stream().anyMatch(r -> r.getCode().equals("ROLE_ADMIN"));
    }

    private boolean isIssueAdmin(User u) {
        return u.getRoles().stream().anyMatch(r -> r.getCode().equals("ROLE_ISSUE_ADMIN"));
    }

    @GetMapping
    public ApiResponse<List<IssueResponse>> list(Authentication auth,
                                                  @RequestParam(required = false) String status) {
        User u = currentUser(auth);
        String dept = u.getDepartment() != null ? u.getDepartment() : "";
        return ApiResponse.ok(issueService.list(u.getId(), dept, isAdmin(u), isIssueAdmin(u), status));
    }

    @GetMapping("/{id}")
    public ApiResponse<IssueResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(issueService.getById(id));
    }

    @PostMapping
    public ApiResponse<IssueResponse> create(Authentication auth,
                                             @Valid @RequestBody IssueRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(issueService.create(userId, request));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ROLE_ISSUE_ADMIN', 'ROLE_ADMIN')")
    public ApiResponse<IssueResponse> assign(@PathVariable Long id,
                                             Authentication auth,
                                             @Valid @RequestBody IssueAssignRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(issueService.assign(id, userId, request));
    }

    @PutMapping("/{id}/solution")
    public ApiResponse<IssueResponse> submitSolution(@PathVariable Long id,
                                                      Authentication auth,
                                                      @Valid @RequestBody IssueSolutionRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(issueService.submitSolution(id, userId, request));
    }

    @PutMapping("/{id}/complete")
    public ApiResponse<IssueResponse> complete(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(issueService.complete(id, userId));
    }

    @PutMapping("/{id}/confirm")
    public ApiResponse<IssueResponse> confirm(@PathVariable Long id,
                                              Authentication auth,
                                              @Valid @RequestBody IssueConfirmRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(issueService.confirm(id, userId, request));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ROLE_ISSUE_ADMIN', 'ROLE_ADMIN')")
    public ApiResponse<IssueResponse> reject(@PathVariable Long id,
                                             Authentication auth,
                                             @Valid @RequestBody IssueRejectRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(issueService.reject(id, userId, request));
    }
}
