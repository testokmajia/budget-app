package com.techmanage.repository;

import com.techmanage.entity.IssueFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IssueFeedbackRepository extends JpaRepository<IssueFeedback, Long>,
        JpaSpecificationExecutor<IssueFeedback> {

    List<IssueFeedback> findBySubmitterIdOrderByCreatedAtDesc(Long submitterId);
    IssueFeedback findTopByIssueCodeStartingWithOrderByIssueCodeDesc(String prefix);

    @Modifying
    @Query("UPDATE IssueFeedback i SET i.submitterDepartment = :newName WHERE i.submitterDepartment = :oldName")
    int updateSubmitterDepartment(String oldName, String newName);

    @Modifying
    @Query("UPDATE IssueFeedback i SET i.issueType = :newName WHERE i.issueType = :oldName")
    int updateIssueType(String oldName, String newName);
}
