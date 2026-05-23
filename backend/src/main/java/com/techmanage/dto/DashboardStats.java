package com.techmanage.dto;

import java.util.List;

public record DashboardStats(
    List<StatusCount> statusCounts,
    List<TeamCount> teamDistribution,
    OverdueInfo overdue,
    List<TeamPersonnel> personnelDistribution
) {
    public record StatusCount(String status, long count) {}
    public record TeamCount(String team, long count) {}
    public record OverdueInfo(long temporaryOverdue, long permanentOverdue) {}
    public record TeamPersonnel(String teamName, long memberCount) {}
}
