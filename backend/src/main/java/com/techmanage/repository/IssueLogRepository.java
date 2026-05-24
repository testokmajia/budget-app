package com.techmanage.repository;

import com.techmanage.entity.IssueLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueLogRepository extends JpaRepository<IssueLog, Long> {
    List<IssueLog> findByIssueIdOrderByCreatedAtAsc(Long issueId);
    List<IssueLog> findByIssueIdInOrderByCreatedAtAsc(List<Long> issueIds);
}
