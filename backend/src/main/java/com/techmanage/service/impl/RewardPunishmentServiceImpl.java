package com.techmanage.service.impl;

import com.techmanage.dto.RewardPunishmentRequest;
import com.techmanage.dto.RewardPunishmentResponse;
import com.techmanage.entity.RewardPunishment;
import com.techmanage.repository.RewardPunishmentRepository;
import com.techmanage.repository.UserRepository;
import com.techmanage.service.RewardPunishmentService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RewardPunishmentServiceImpl implements RewardPunishmentService {

    private final RewardPunishmentRepository repository;
    private final UserRepository userRepository;

    public RewardPunishmentServiceImpl(RewardPunishmentRepository repository,
                                       UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Override
    public List<RewardPunishmentResponse> list(Long userId, boolean canViewAll,
                                                String type, String department,
                                                String keyword, LocalDate dateFrom,
                                                LocalDate dateTo, Integer scoreMin, Integer scoreMax) {
        List<RewardPunishment> source;
        if (canViewAll) {
            if (type != null && !type.isEmpty() && department != null && !department.isEmpty()) {
                source = repository.findByTypeAndDepartment(type, department);
            } else if (type != null && !type.isEmpty()) {
                source = repository.findByType(type);
            } else if (department != null && !department.isEmpty()) {
                source = repository.findByDepartment(department);
            } else {
                source = repository.findAllActive();
            }
        } else {
            String userDept = userRepository.findById(userId)
                .map(u -> u.getDepartment())
                .orElse("");
            if (userDept.isEmpty()) return List.of();
            if (type != null && !type.isEmpty()) {
                source = repository.findByTypeAndDepartment(type, userDept);
            } else {
                source = repository.findByDepartment(userDept);
            }
        }

        return source.stream()
            .filter(r -> keyword == null || keyword.isEmpty() || r.getTitle().contains(keyword) || r.getDescription().contains(keyword))
            .filter(r -> {
                if (dateFrom == null && dateTo == null) return true;
                if (dateFrom != null && r.getDecisionDate().isBefore(dateFrom)) return false;
                if (dateTo != null && r.getDecisionDate().isAfter(dateTo)) return false;
                return true;
            })
            .filter(r -> {
                if (scoreMin == null && scoreMax == null) return true;
                Integer s = r.getScore();
                if (s == null) return false;
                if (scoreMin != null && s < scoreMin) return false;
                if (scoreMax != null && s > scoreMax) return false;
                return true;
            })
            .map(this::toResponse)
            .toList();
    }

    @Override
    public RewardPunishmentResponse getById(Long id) {
        var rp = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("记录不存在"));
        return toResponse(rp);
    }

    @Override
    public List<RewardPunishmentResponse> create(Long userId, RewardPunishmentRequest request) {
        List<RewardPunishmentResponse> results = new ArrayList<>();
        int baseScore = Math.abs(request.score());
        for (String name : request.involvedPersonNames()) {
            if (name == null || name.isBlank()) continue;
            var rp = new RewardPunishment();
            rp.setCreatorId(userId);
            applyRequest(rp, request, baseScore);
            rp.setInvolvedPerson(name.trim());
            repository.save(rp);
            results.add(toResponse(rp));
        }
        return results;
    }

    @Override
    public RewardPunishmentResponse update(Long id, Long userId, boolean canEditAll, RewardPunishmentRequest request) {
        var rp = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("记录不存在"));
        if (!canEditAll && !rp.getCreatorId().equals(userId)) {
            throw new RuntimeException("只能修改自己创建的记录");
        }
        int baseScore = Math.abs(request.score());
        applyRequest(rp, request, baseScore);
        // Update involvedPerson from first name in list if provided
        if (request.involvedPersonNames() != null && !request.involvedPersonNames().isEmpty()) {
            rp.setInvolvedPerson(request.involvedPersonNames().get(0).trim());
        }
        repository.save(rp);
        return toResponse(rp);
    }

    @Override
    public void delete(Long id, Long userId, boolean canEditAll) {
        var rp = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("记录不存在"));
        if (!canEditAll && !rp.getCreatorId().equals(userId)) {
            throw new RuntimeException("只能删除自己创建的记录");
        }
        rp.setDeletedAt(LocalDateTime.now());
        repository.save(rp);
    }

    private void applyRequest(RewardPunishment rp, RewardPunishmentRequest req, int baseScore) {
        rp.setType(req.type());
        rp.setTitle(req.title());
        rp.setDescription(req.description());
        rp.setDepartment(req.department());
        rp.setDecisionDate(req.decisionDate());
        rp.setDocumentNo(req.documentNo());
        rp.setAttachmentUrl(req.attachmentUrl());
        rp.setScore("惩罚".equals(req.type()) ? -baseScore : baseScore);
    }

    private RewardPunishmentResponse toResponse(RewardPunishment rp) {
        String creatorName = userRepository.findById(rp.getCreatorId())
            .map(u -> u.getName())
            .orElse("");
        return new RewardPunishmentResponse(
            rp.getId(), rp.getType(), rp.getTitle(), rp.getDescription(),
            rp.getInvolvedPerson(), rp.getDepartment(), rp.getDecisionDate(),
            rp.getDocumentNo(), rp.getAttachmentUrl(), creatorName,
            rp.getCreatedAt(), rp.getUpdatedAt(), rp.getScore()
        );
    }
}
