package com.techmanage.repository;

import com.techmanage.entity.RequirementComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequirementCommentRepository extends JpaRepository<RequirementComment, Long> {

    /** 按需求ID查询所有评论，按时间正序 */
    List<RequirementComment> findByRequirementIdOrderByCreatedAtAsc(Long requirementId);
}
