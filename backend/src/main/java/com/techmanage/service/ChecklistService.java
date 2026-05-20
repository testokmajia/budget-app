package com.techmanage.service;

import com.techmanage.dto.ChecklistRequest;
import com.techmanage.dto.ChecklistResponse;

import java.time.LocalDate;
import java.util.List;

public interface ChecklistService {
    List<ChecklistResponse> list(Long userId, String status, String keyword, String responsiblePerson,
                                  LocalDate startDate, LocalDate endDate);
    ChecklistResponse getById(Long id, Long userId);
    ChecklistResponse create(Long userId, ChecklistRequest request);
    ChecklistResponse update(Long id, Long userId, ChecklistRequest request);
    ChecklistResponse complete(Long id, Long userId);
    void delete(Long id, Long userId);
}
