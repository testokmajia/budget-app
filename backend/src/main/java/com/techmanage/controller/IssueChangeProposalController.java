package com.techmanage.controller;

import com.techmanage.common.ApiResponse;
import com.techmanage.dto.ChangeProposalRequest;
import com.techmanage.dto.ChangeProposalResponse;
import com.techmanage.dto.ChangeReviewRequest;
import com.techmanage.entity.Team;
import com.techmanage.entity.User;
import com.techmanage.repository.TeamRepository;
import com.techmanage.repository.UserRepository;
import com.techmanage.service.IssueChangeProposalService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class IssueChangeProposalController {

    private final IssueChangeProposalService proposalService;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    public IssueChangeProposalController(IssueChangeProposalService proposalService,
                                          UserRepository userRepository,
                                          TeamRepository teamRepository) {
        this.proposalService = proposalService;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
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

    private boolean isTeamLeader(User u) {
        return !teamRepository.findByLeader(u.getName()).isEmpty();
    }

    private List<Long> getLedTeamMemberIds(User u) {
        List<Team> ledTeams = teamRepository.findByLeader(u.getName());
        if (ledTeams.isEmpty()) return Collections.emptyList();
        Set<String> memberNames = new HashSet<>();
        for (Team t : ledTeams) {
            if (t.getMembers() != null) {
                for (String name : t.getMembers().split(",")) {
                    memberNames.add(name.trim());
                }
            }
        }
        if (memberNames.isEmpty()) return Collections.emptyList();
        return userRepository.findByNameIn(new ArrayList<>(memberNames))
                .stream().map(User::getId).toList();
    }

    @PostMapping("/api/issues/{id}/change-proposals")
    public ApiResponse<ChangeProposalResponse> submit(@PathVariable Long id,
                                                       Authentication auth,
                                                       @Valid @RequestBody ChangeProposalRequest request) {
        User u = currentUser(auth);
        boolean tl = isTeamLeader(u);
        return ApiResponse.ok(proposalService.submit(id, u.getId(), request, tl, getLedTeamMemberIds(u)));
    }

    @GetMapping("/api/change-proposals/pending")
    public ApiResponse<List<ChangeProposalResponse>> listPending(Authentication auth) {
        User u = currentUser(auth);
        boolean tl = isTeamLeader(u);
        return ApiResponse.ok(proposalService.listPending(u.getId(), isAdmin(u), isIssueAdmin(u),
                tl, getLedTeamMemberIds(u)));
    }

    @PutMapping("/api/change-proposals/{id}/review")
    public ApiResponse<ChangeProposalResponse> review(@PathVariable Long id,
                                                       Authentication auth,
                                                       @Valid @RequestBody ChangeReviewRequest request) {
        User u = currentUser(auth);
        return ApiResponse.ok(proposalService.review(id, u.getId(), request, isAdmin(u), isIssueAdmin(u)));
    }

    @GetMapping("/api/issues/{id}/change-proposals")
    public ApiResponse<List<ChangeProposalResponse>> getByIssueId(@PathVariable Long id) {
        return ApiResponse.ok(proposalService.getByIssueId(id));
    }
}
