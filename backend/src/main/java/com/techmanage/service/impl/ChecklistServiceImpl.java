package com.techmanage.service.impl;

import com.techmanage.common.BusinessException;
import com.techmanage.dto.ChecklistRequest;
import com.techmanage.dto.ChecklistResponse;
import com.techmanage.entity.Checklist;
import com.techmanage.repository.ChecklistRepository;
import com.techmanage.repository.UserRepository;
import com.techmanage.service.ChecklistService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChecklistServiceImpl implements ChecklistService {

    private final ChecklistRepository checklistRepository;
    private final UserRepository userRepository;

    public ChecklistServiceImpl(ChecklistRepository checklistRepository,
                               UserRepository userRepository) {
        this.checklistRepository = checklistRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<ChecklistResponse> list(Long userId, List<String> status, String keyword,
                                         String responsiblePerson, LocalDate startDate, LocalDate endDate) {
        return checklistRepository.findByUserId(userId).stream()
            .filter(c -> status == null || status.isEmpty() || status.contains(c.getStatus()))
            .filter(c -> keyword == null || keyword.isEmpty() || c.getDescription().contains(keyword))
            .filter(c -> responsiblePerson == null || responsiblePerson.isEmpty() ||
                         (c.getResponsiblePerson() != null && c.getResponsiblePerson().contains(responsiblePerson)))
            .filter(c -> {
                if (startDate == null && endDate == null) return true;
                LocalDate ref = c.getActualDate() != null ? c.getActualDate() : c.getCreatedAt().toLocalDate();
                if (startDate != null && ref.isBefore(startDate)) return false;
                if (endDate != null && ref.isAfter(endDate)) return false;
                return true;
            })
            .map(this::toResponse)
            .toList();
    }

    @Override
    public ChecklistResponse getById(Long id, Long userId) {
        var checklist = checklistRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new BusinessException("清单不存在"));
        return toResponse(checklist);
    }

    @Override
    public ChecklistResponse create(Long userId, ChecklistRequest request) {
        var checklist = new Checklist();
        checklist.setUserId(userId);
        applyRequest(checklist, request);
        checklistRepository.save(checklist);
        return toResponse(checklist);
    }

    @Override
    public ChecklistResponse update(Long id, Long userId, ChecklistRequest request) {
        var checklist = checklistRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new BusinessException("清单不存在"));
        applyRequest(checklist, request);
        checklistRepository.save(checklist);
        return toResponse(checklist);
    }

    @Override
    public ChecklistResponse complete(Long id, Long userId, LocalDate actualDate) {
        var checklist = checklistRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new BusinessException("清单不存在"));
        checklist.setStatus("已完成");
        checklist.setActualDate(actualDate != null ? actualDate : LocalDate.now());
        checklistRepository.save(checklist);
        return toResponse(checklist);
    }

    @Override
    public void delete(Long id, Long userId) {
        var checklist = checklistRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new BusinessException("清单不存在"));
        checklist.setDeletedAt(LocalDateTime.now());
        checklistRepository.save(checklist);
    }

    private void applyRequest(Checklist checklist, ChecklistRequest request) {
        checklist.setSource(request.source());
        checklist.setSourceDetail(request.sourceDetail());
        checklist.setDescription(request.description());
        checklist.setStatus(request.status());
        checklist.setResponsiblePerson(request.responsiblePerson());
        checklist.setPlannedDate(request.plannedDate());
        checklist.setActualDate(request.actualDate());
        checklist.setRemark(request.remark());
    }

    private ChecklistResponse toResponse(Checklist c) {
        String creatorName = userRepository.findById(c.getUserId())
            .map(u -> u.getName())
            .orElse("");
        return new ChecklistResponse(
            c.getId(), c.getSource(), c.getSourceDetail(), c.getDescription(), c.getStatus(),
            c.getResponsiblePerson(), c.getPlannedDate(), c.getActualDate(),
            c.getRemark(), creatorName, c.getCreatedAt(), c.getUpdatedAt()
        );
    }
}
