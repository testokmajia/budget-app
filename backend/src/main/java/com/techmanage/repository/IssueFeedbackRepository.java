package com.techmanage.repository;

import com.techmanage.entity.IssueFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface IssueFeedbackRepository extends JpaRepository<IssueFeedback, Long>,
        JpaSpecificationExecutor<IssueFeedback> {

    List<IssueFeedback> findBySubmitterIdOrderByCreatedAtDesc(Long submitterId);
    IssueFeedback findTopByIssueCodeStartingWithOrderByIssueCodeDesc(String prefix);
}
