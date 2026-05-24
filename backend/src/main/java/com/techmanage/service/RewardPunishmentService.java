package com.techmanage.service;

import com.techmanage.dto.RewardPunishmentRequest;
import com.techmanage.dto.RewardPunishmentResponse;

import java.time.LocalDate;
import java.util.List;

public interface RewardPunishmentService {
    List<RewardPunishmentResponse> list(Long userId, boolean canViewAll,
                                        String type, String department,
                                        String keyword, LocalDate dateFrom,
                                        LocalDate dateTo, Integer scoreMin, Integer scoreMax);
    RewardPunishmentResponse getById(Long id);
    List<RewardPunishmentResponse> create(Long userId, RewardPunishmentRequest request);
    RewardPunishmentResponse update(Long id, Long userId, boolean canEditAll, RewardPunishmentRequest request);
    void delete(Long id, Long userId, boolean canEditAll);
}
