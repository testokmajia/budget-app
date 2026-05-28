package com.techmanage.service;

import com.techmanage.dto.DepartmentReportRequest;
import com.techmanage.dto.DepartmentReportResponse;

import java.util.List;

public interface DepartmentReportService {
    DepartmentReportResponse mergeAi(Long clerkUserId, DepartmentReportRequest request);
    DepartmentReportResponse getById(Long id);
    DepartmentReportResponse update(Long id, Long clerkUserId, String editedContent);
    DepartmentReportResponse submit(Long id, Long clerkUserId);
    DepartmentReportResponse finalize(Long id, Long headUserId);
    List<DepartmentReportResponse> listDepartmentReports();
    byte[] exportWord(Long id);
    String exportHtml(Long id);
}
