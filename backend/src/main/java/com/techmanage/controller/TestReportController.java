package com.techmanage.controller;

import com.techmanage.common.ApiResponse;
import com.techmanage.dto.PageResponse;
import com.techmanage.dto.TestReportRequest;
import com.techmanage.entity.User;
import com.techmanage.repository.UserRepository;
import com.techmanage.service.TestReportService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test-reports")
public class TestReportController {

    private final TestReportService reportService;
    private final UserRepository userRepo;

    public TestReportController(TestReportService reportService, UserRepository userRepo) {
        this.reportService = reportService;
        this.userRepo = userRepo;
    }

    private User currentUser(Authentication auth) {
        return userRepo.findById((Long) auth.getPrincipal()).orElseThrow();
    }

    @GetMapping
    public ApiResponse<PageResponse<Map<String, Object>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long submitterId,
            @RequestParam(required = false) String systemName,
            @RequestParam(required = false) String requirementTitle,
            @RequestParam(required = false) LocalDate createdAtFrom,
            @RequestParam(required = false) LocalDate createdAtTo,
            @RequestParam(required = false) LocalDate confirmedAtFrom,
            @RequestParam(required = false) LocalDate confirmedAtTo) {
        List<String> statuses = null;
        if (org.springframework.util.StringUtils.hasText(status)) {
            statuses = Arrays.asList(status.split(","));
        }
        var result = reportService.list(keyword, statuses, submitterId, systemName,
                requirementTitle, createdAtFrom, createdAtTo, confirmedAtFrom, confirmedAtTo, page, size);
        return ApiResponse.ok(PageResponse.of(result.getContent(), result.getTotalElements(), page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(reportService.getDetail(id));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Long>> stats() {
        return ApiResponse.ok(reportService.stats());
    }

    @GetMapping("/by-requirement/{reqId}")
    public ApiResponse<List<Map<String, Object>>> byRequirement(@PathVariable Long reqId) {
        return ApiResponse.ok(reportService.getByRequirement(reqId));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody TestReportRequest request,
                                                    Authentication auth) {
        return ApiResponse.ok(reportService.create(request, currentUser(auth)));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<Map<String, Object>> confirm(@PathVariable Long id,
                                                     @RequestBody(required = false) TestReportRequest request,
                                                     Authentication auth) {
        String comment = request != null ? request.getComment() : null;
        LocalDate confirmedDate = null;
        if (request != null && org.springframework.util.StringUtils.hasText(request.getConfirmedDate())) {
            confirmedDate = LocalDate.parse(request.getConfirmedDate());
        }
        return ApiResponse.ok(reportService.confirm(id, currentUser(auth), comment, confirmedDate));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Map<String, Object>> reject(@PathVariable Long id,
                                                    @RequestBody(required = false) TestReportRequest request,
                                                    Authentication auth) {
        String comment = request != null ? request.getComment() : null;
        return ApiResponse.ok(reportService.reject(id, currentUser(auth), comment));
    }

    /** 根据选中需求自动生成默认确认人员 */
    @PostMapping("/default-reviewers")
    public ApiResponse<List<Map<String, Object>>> defaultReviewers(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(reportService.getDefaultReviewers(body.get("requirementIds")));
    }
}
