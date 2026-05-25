package com.techmanage.repository;

import com.techmanage.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByIssueIdOrderByCreatedAtAsc(Long issueId);
    void deleteByIssueIdAndId(Long issueId, Long id);
}
