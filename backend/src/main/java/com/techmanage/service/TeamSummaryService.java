package com.techmanage.service;

import com.techmanage.dto.TeamInfo;
import com.techmanage.dto.TeamSummaryRequest;
import com.techmanage.dto.TeamSummaryResponse;

import java.util.List;

public interface TeamSummaryService {
    TeamSummaryResponse mergeAi(Long leaderId, TeamSummaryRequest request);
    TeamSummaryResponse update(Long id, Long leaderId, String editedContent);
    TeamSummaryResponse submit(Long id, Long leaderId);
    TeamSummaryResponse getById(Long id);
    List<TeamSummaryResponse> listByLeader(Long leaderId);
    List<TeamSummaryResponse> listByWeek(java.time.LocalDate weekStartDate);
    List<TeamInfo> getMyTeams(Long leaderId);
}
