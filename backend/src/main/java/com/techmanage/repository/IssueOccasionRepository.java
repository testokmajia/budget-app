package com.techmanage.repository;

import com.techmanage.entity.IssueOccasion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueOccasionRepository extends JpaRepository<IssueOccasion, Long> {
    List<IssueOccasion> findAllByOrderByIdAsc();
    boolean existsByName(String name);
}
