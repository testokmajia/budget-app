package com.techmanage.service.impl;

import com.techmanage.dto.RewardPunishmentRequest;
import com.techmanage.dto.RewardPunishmentResponse;
import com.techmanage.entity.RewardPunishment;
import com.techmanage.repository.RewardPunishmentRepository;
import com.techmanage.repository.UserRepository;
import com.techmanage.service.RewardPunishmentService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    public List<RewardPunishmentResponse> list(Long userId, boolean canViewAll) {
        if (canViewAll) {
            return repository.findAllActive().stream().map(this::toResponse).toList();
        }
        String department = userRepository.findById(userId)
            .map(u -> u.getDepartment())
            .orElse("");
        if (department.isEmpty()) {
            return List.of();
        }
        return repository.findByDepartment(department).stream().map(this::toResponse).toList();
    }

    @Override
    public RewardPunishmentResponse getById(Long id) {
        var rp = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("记录不存在"));
        return toResponse(rp);
    }

    @Override
    public RewardPunishmentResponse create(Long userId, RewardPunishmentRequest request) {
        var rp = new RewardPunishment();
        rp.setCreatorId(userId);
        applyRequest(rp, request);
        repository.save(rp);
        return toResponse(rp);
    }

    @Override
    public RewardPunishmentResponse update(Long id, Long userId, boolean canEditAll, RewardPunishmentRequest request) {
        var rp = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("记录不存在"));
        if (!canEditAll && !rp.getCreatorId().equals(userId)) {
            throw new RuntimeException("只能修改自己创建的记录");
        }
        applyRequest(rp, request);
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

    private void applyRequest(RewardPunishment rp, RewardPunishmentRequest req) {
        rp.setType(req.type());
        rp.setTitle(req.title());
        rp.setDescription(req.description());
        rp.setInvolvedPerson(req.involvedPerson());
        rp.setDepartment(req.department());
        rp.setDecisionDate(req.decisionDate());
        rp.setDocumentNo(req.documentNo());
        rp.setAttachmentUrl(req.attachmentUrl());
    }

    private RewardPunishmentResponse toResponse(RewardPunishment rp) {
        String creatorName = userRepository.findById(rp.getCreatorId())
            .map(u -> u.getName())
            .orElse("");
        return new RewardPunishmentResponse(
            rp.getId(), rp.getType(), rp.getTitle(), rp.getDescription(),
            rp.getInvolvedPerson(), rp.getDepartment(), rp.getDecisionDate(),
            rp.getDocumentNo(), rp.getAttachmentUrl(), creatorName,
            rp.getCreatedAt(), rp.getUpdatedAt()
        );
    }
}
