package com.techmanage.repository;

import com.techmanage.entity.IssueCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueCategoryRepository extends JpaRepository<IssueCategory, Long> {
    List<IssueCategory> findAllByOrderBySortOrderAsc();
    boolean existsByName(String name);
}
