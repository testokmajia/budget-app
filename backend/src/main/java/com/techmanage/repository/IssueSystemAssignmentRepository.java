package com.techmanage.repository;

import com.techmanage.entity.IssueSystemAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueSystemAssignmentRepository extends JpaRepository<IssueSystemAssignment, Long> {
    List<IssueSystemAssignment> findByIssueIdOrderByCreatedAtAsc(Long issueId);
    List<IssueSystemAssignment> findBySystemOwnerIdOrderByCreatedAtDesc(Long systemOwnerId);
    List<IssueSystemAssignment> findByIssueIdAndSystemOwnerId(Long issueId, Long systemOwnerId);
    void deleteByIssueId(Long issueId);
}
