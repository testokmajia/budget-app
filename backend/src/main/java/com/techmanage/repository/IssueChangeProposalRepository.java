package com.techmanage.repository;

import com.techmanage.entity.IssueChangeProposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueChangeProposalRepository extends JpaRepository<IssueChangeProposal, Long> {
    List<IssueChangeProposal> findByIssueIdOrderByCreatedAtDesc(Long issueId);
    List<IssueChangeProposal> findByStatusOrderByCreatedAtDesc(String status);
}
