package com.techmanage.repository;

import com.techmanage.entity.IssueFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IssueFeedbackRepository extends JpaRepository<IssueFeedback, Long> {

    List<IssueFeedback> findBySubmitterIdOrderByCreatedAtDesc(Long submitterId);

    List<IssueFeedback> findByAssigneeIdOrderByCreatedAtDesc(Long assigneeId);

    List<IssueFeedback> findByDepartmentOrderByCreatedAtDesc(String department);

    @Query("SELECT i FROM IssueFeedback i ORDER BY i.createdAt DESC")
    List<IssueFeedback> findAllOrdered();
}
