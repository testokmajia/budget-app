package com.techmanage.service;

import com.techmanage.dto.ReviewRequest;
import com.techmanage.dto.SmartDraftResponse;
import com.techmanage.dto.WeeklyReportRequest;
import com.techmanage.dto.WeeklyReportResponse;

import java.time.LocalDate;
import java.util.List;

public interface WeeklyReportService {
    WeeklyReportResponse getCurrentWeek(Long userId);
    WeeklyReportResponse save(Long userId, WeeklyReportRequest request);
    WeeklyReportResponse submit(Long userId, WeeklyReportRequest request);
    SmartDraftResponse getSmartDraft(Long userId);
    List<WeeklyReportResponse> listMyReports(Long userId, LocalDate from, LocalDate to);
    List<WeeklyReportResponse> listPendingForLeader(Long leaderUserId);
    long countPendingForLeader(Long leaderUserId);
    WeeklyReportResponse approve(Long reportId, Long leaderUserId, ReviewRequest request);
    WeeklyReportResponse reject(Long reportId, Long leaderUserId, ReviewRequest request);
    List<WeeklyReportResponse> listTeamHistory(Long leaderUserId, LocalDate from, LocalDate to);
    List<WeeklyReportResponse> listAllHistory(LocalDate from, LocalDate to);
}
