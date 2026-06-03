package com.techmanage.repository;

import com.techmanage.entity.TestReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TestReportRepository extends JpaRepository<TestReport, Long>, JpaSpecificationExecutor<TestReport> {

    long countByStatus(String status);

    /** 查询包含指定需求ID的测试报告（requirementIds字段是JSON数组） */
    @Query("SELECT t FROM TestReport t WHERE t.requirementIds LIKE %:reqId%")
    List<TestReport> findByRequirementIdContaining(@Param("reqId") String reqId);
}
