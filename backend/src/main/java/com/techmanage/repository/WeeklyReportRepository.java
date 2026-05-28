package com.techmanage.repository;

import com.techmanage.entity.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {

    Optional<WeeklyReport> findByUserIdAndWeekStartDate(Long userId, LocalDate weekStartDate);

    @Query("SELECT r FROM WeeklyReport r WHERE r.userId = ?1 AND r.weekStartDate = ?2 ORDER BY r.version DESC LIMIT 1")
    Optional<WeeklyReport> findLatestByUserIdAndWeekStartDate(Long userId, LocalDate weekStartDate);

    @Query("SELECT r FROM WeeklyReport r WHERE r.userId = ?1 AND r.weekStartDate = ?2 ORDER BY r.version DESC")
    List<WeeklyReport> findAllVersionsByUserIdAndWeekStartDate(Long userId, LocalDate weekStartDate);

    @Query("SELECT r FROM WeeklyReport r WHERE r.userId = ?1 ORDER BY r.weekStartDate DESC")
    List<WeeklyReport> findByUserId(Long userId);

    @Query("SELECT r FROM WeeklyReport r WHERE r.userId IN ?1 AND r.status = 'SUBMITTED' ORDER BY r.submittedAt ASC")
    List<WeeklyReport> findSubmittedByUserIds(List<Long> userIds);

    @Query("SELECT r FROM WeeklyReport r WHERE r.userId IN ?1 AND r.weekStartDate = ?2 AND r.status = 'SUBMITTED'")
    List<WeeklyReport> findSubmittedByUserIdsAndWeekStartDate(List<Long> userIds, LocalDate weekStartDate);

    List<WeeklyReport> findByWeekStartDate(LocalDate weekStartDate);

    @Query("SELECT r FROM WeeklyReport r WHERE r.weekStartDate = ?1 AND r.status = 'APPROVED' AND r.merged = false ORDER BY r.userId")
    List<WeeklyReport> findApprovedByWeek(LocalDate weekStartDate);

    @Query("SELECT r FROM WeeklyReport r WHERE r.userId = ?1 " +
           "AND (?2 IS NULL OR r.weekStartDate >= ?2) " +
           "AND (?3 IS NULL OR r.weekEndDate <= ?3) ORDER BY r.weekStartDate DESC")
    List<WeeklyReport> findByUserIdAndDateRange(Long userId, LocalDate from, LocalDate to);

    @Query("SELECT r FROM WeeklyReport r WHERE " +
           "(?1 IS NULL OR r.weekStartDate >= ?1) " +
           "AND (?2 IS NULL OR r.weekEndDate <= ?2) ORDER BY r.weekStartDate DESC, r.userId")
    List<WeeklyReport> findByDateRange(LocalDate from, LocalDate to);

    @Query("SELECT COUNT(r) FROM WeeklyReport r WHERE r.userId IN ?1 AND r.status = 'SUBMITTED'")
    long countSubmittedByUserIds(List<Long> userIds);
}
