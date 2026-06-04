package com.techmanage.repository;

import com.techmanage.entity.TestReportComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestReportCommentRepository extends JpaRepository<TestReportComment, Long> {

    /** 按测试报告ID查询所有审核记录，按时间正序 */
    List<TestReportComment> findByReportIdOrderByCreatedAtAsc(Long reportId);
}
