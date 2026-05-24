package com.techmanage.repository;

import com.techmanage.entity.RewardPunishment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RewardPunishmentRepository extends JpaRepository<RewardPunishment, Long> {

    @Query("SELECT r FROM RewardPunishment r WHERE r.department = ?1 AND r.deletedAt IS NULL ORDER BY r.decisionDate DESC")
    List<RewardPunishment> findByDepartment(String department);

    @Query("SELECT r FROM RewardPunishment r WHERE r.deletedAt IS NULL ORDER BY r.decisionDate DESC")
    List<RewardPunishment> findAllActive();

    @Query("SELECT r FROM RewardPunishment r WHERE r.type = ?1 AND r.deletedAt IS NULL ORDER BY r.decisionDate DESC")
    List<RewardPunishment> findByType(String type);

    @Query("SELECT r FROM RewardPunishment r WHERE r.type = ?1 AND r.department = ?2 AND r.deletedAt IS NULL ORDER BY r.decisionDate DESC")
    List<RewardPunishment> findByTypeAndDepartment(String type, String department);

    List<RewardPunishment> findByInvolvedPerson(String involvedPerson);
}
