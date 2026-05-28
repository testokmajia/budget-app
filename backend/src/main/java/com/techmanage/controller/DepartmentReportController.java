package com.techmanage.controller;

import com.techmanage.common.ApiResponse;
import com.techmanage.dto.DepartmentReportRequest;
import com.techmanage.dto.DepartmentReportResponse;
import com.techmanage.service.DepartmentReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/department-reports")
public class DepartmentReportController {

    private final DepartmentReportService deptReportService;

    public DepartmentReportController(DepartmentReportService deptReportService) {
        this.deptReportService = deptReportService;
    }

    @PostMapping("/merge")
    @PreAuthorize("hasAnyRole('ROLE_CLERK', 'ROLE_ADMIN')")
    public ApiResponse<DepartmentReportResponse> mergeAi(Authentication auth,
                                                         @Valid @RequestBody DepartmentReportRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(deptReportService.mergeAi(userId, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_CLERK', 'ROLE_ADMIN')")
    public ApiResponse<List<DepartmentReportResponse>> list() {
        return ApiResponse.ok(deptReportService.listDepartmentReports());
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ROLE_CLERK', 'ROLE_ADMIN')")
    public ApiResponse<DepartmentReportResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(deptReportService.getById(id));
    }

    @PutMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ROLE_CLERK', 'ROLE_ADMIN')")
    public ApiResponse<DepartmentReportResponse> update(@PathVariable Long id,
                                                        Authentication auth,
                                                        @RequestBody Map<String, String> body) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(deptReportService.update(id, userId, body.get("editedContent")));
    }

    @PostMapping("/{id:\\d+}/submit")
    @PreAuthorize("hasAnyRole('ROLE_CLERK', 'ROLE_ADMIN')")
    public ApiResponse<DepartmentReportResponse> submit(@PathVariable Long id,
                                                        Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(deptReportService.submit(id, userId));
    }

    @PostMapping("/{id:\\d+}/finalize")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<DepartmentReportResponse> finalize(@PathVariable Long id,
                                                          Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(deptReportService.finalize(id, userId));
    }

    @GetMapping("/{id:\\d+}/export/word")
    @PreAuthorize("hasAnyRole('ROLE_CLERK', 'ROLE_ADMIN')")
    public ResponseEntity<byte[]> exportWord(@PathVariable Long id) {
        byte[] content = deptReportService.exportWord(id);
        String filename = URLEncoder.encode("部门周报.doc", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(content);
    }

    @GetMapping("/{id:\\d+}/export/html")
    @PreAuthorize("hasAnyRole('ROLE_CLERK', 'ROLE_ADMIN')")
    public ResponseEntity<String> exportHtml(@PathVariable Long id) {
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(deptReportService.exportHtml(id));
    }
}
