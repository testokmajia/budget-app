package com.techmanage.service;

import com.techmanage.dto.ChecklistRequest;
import com.techmanage.dto.ChecklistResponse;
import com.techmanage.dto.PageResponse;

import java.time.LocalDate;
import java.util.List;

public interface ChecklistService {
    PageResponse<ChecklistResponse> list(Long userId, List<String> status, String keyword, String responsiblePerson,
                                         LocalDate startDate, LocalDate endDate, int page, int size);
    ChecklistResponse getById(Long id, Long userId);
    ChecklistResponse create(Long userId, ChecklistRequest request);
    ChecklistResponse update(Long id, Long userId, ChecklistRequest request);
    ChecklistResponse complete(Long id, Long userId, LocalDate actualDate);
    void delete(Long id, Long userId);
}
