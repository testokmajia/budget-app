package com.techmanage.controller;

import com.techmanage.common.ApiResponse;
import com.techmanage.dto.IssueResponse;
import com.techmanage.entity.IssueSystemAssignment;
import com.techmanage.entity.User;
import com.techmanage.repository.UserRepository;
import com.techmanage.service.IssueFeedbackService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/issues")
public class IssueSystemAssignmentController {

    private final IssueFeedbackService issueService;
    private final UserRepository userRepository;

    public IssueSystemAssignmentController(IssueFeedbackService issueService,
                                            UserRepository userRepository) {
        this.issueService = issueService;
        this.userRepository = userRepository;
    }

    private User currentUser(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return userRepository.findById(userId).orElseThrow();
    }

    @GetMapping("/pending")
    public ApiResponse<List<Map<String, Object>>> listPending(
            Authentication auth,
            @RequestParam(defaultValue = "pending") String status,
            @RequestParam(required = false) Long ownerId) {
        User u = currentUser(auth);
        boolean isAdmin = u.getRoles().stream().anyMatch(r -> r.getCode().equals("ROLE_ADMIN"));
        boolean isIssueAdmin = u.getRoles().stream().anyMatch(r -> r.getCode().equals("ROLE_ISSUE_ADMIN"));
        Long filterOwnerId = ownerId != null && ownerId == -1L ? null : (ownerId != null ? ownerId : u.getId());
        return ApiResponse.ok(issueService.listPending(u.getId(), isAdmin, isIssueAdmin, status, filterOwnerId));
    }

    @PutMapping("/{issueId}/systems/{assignmentId}/complete")
    public ApiResponse<Void> completeAssignment(@PathVariable Long issueId,
                                                 @PathVariable Long assignmentId,
                                                 @RequestBody Map<String, String> body,
                                                 Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String completionNote = body.get("completionNote");
        issueService.completeSystemAssignment(assignmentId, userId, completionNote);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{issueId}/systems")
    public ApiResponse<List<Map<String, Object>>> getSystemAssignments(@PathVariable Long issueId) {
        List<IssueSystemAssignment> assignments = issueService.getSystemAssignments(issueId);
        var result = assignments.stream().map(a -> {
            String ownerName = userRepository.findById(a.getSystemOwnerId())
                    .map(User::getName).orElse("");
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", a.getId());
            m.put("systemName", a.getSystemName());
            m.put("systemOwnerId", a.getSystemOwnerId());
            m.put("systemOwnerName", ownerName);
            m.put("completed", a.isCompleted());
            m.put("completedAt", a.getCompletedAt());
            return m;
        }).toList();
        return ApiResponse.ok(result);
    }

    @PutMapping("/{id}/feedback")
    @PreAuthorize("hasAnyRole('ROLE_ISSUE_ADMIN', 'ROLE_ADMIN')")
    public ApiResponse<IssueResponse> feedbackToSubmitter(@PathVariable Long id,
                                                            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(issueService.feedbackToSubmitter(id, userId));
    }
}
