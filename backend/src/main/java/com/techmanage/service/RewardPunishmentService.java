package com.techmanage.service;

import com.techmanage.dto.RewardPunishmentRequest;
import com.techmanage.dto.RewardPunishmentResponse;

import java.util.List;

public interface RewardPunishmentService {
    List<RewardPunishmentResponse> list(Long userId, boolean isAdmin);
    RewardPunishmentResponse getById(Long id);
    RewardPunishmentResponse create(Long userId, RewardPunishmentRequest request);
    RewardPunishmentResponse update(Long id, Long userId, RewardPunishmentRequest request);
    void delete(Long id, Long userId);
}
