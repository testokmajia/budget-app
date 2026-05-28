package com.techmanage.service;

import com.techmanage.dto.WeeklyReportResponse;

import java.util.List;

public interface AiService {
    String mergeReports(List<WeeklyReportResponse> reports);
}
