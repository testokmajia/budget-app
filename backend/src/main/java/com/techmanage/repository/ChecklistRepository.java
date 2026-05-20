package com.techmanage.repository;

import com.techmanage.entity.Checklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChecklistRepository extends JpaRepository<Checklist, Long> {

    @Query("SELECT c FROM Checklist c WHERE c.userId = ?1 AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
    List<Checklist> findByUserId(Long userId);

    @Query("SELECT c FROM Checklist c WHERE c.userId = ?1 AND c.status = ?2 AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
    List<Checklist> findByUserIdAndStatus(Long userId, String status);

    @Query("SELECT c FROM Checklist c WHERE c.id = ?1 AND c.userId = ?2 AND c.deletedAt IS NULL")
    java.util.Optional<Checklist> findByIdAndUserId(Long id, Long userId);
}
