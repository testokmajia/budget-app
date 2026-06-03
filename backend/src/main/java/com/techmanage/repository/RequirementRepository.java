package com.techmanage.repository;

import com.techmanage.entity.Requirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RequirementRepository extends JpaRepository<Requirement, Long>,
        JpaSpecificationExecutor<Requirement> {

    Optional<Requirement> findByRequirementCode(String code);

    /** 查询最大的序号，用于生成新编号 */
    @Query("SELECT MAX(r.requirementCode) FROM Requirement r WHERE r.requirementCode LIKE :prefix%")
    Optional<String> findMaxCodeByPrefix(@Param("prefix") String prefix);

    /** 按提出人统计 */
    long countBySubmitterId(Long submitterId);

    /** 按状态统计 */
    long countByStatus(String status);

    /** 按状态分页查询 */
    Page<Requirement> findByStatus(String status, Pageable pageable);
}
