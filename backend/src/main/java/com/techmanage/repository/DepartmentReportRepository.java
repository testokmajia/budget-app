package com.techmanage.repository;

import com.techmanage.entity.DepartmentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentReportRepository extends JpaRepository<DepartmentReport, Long> {

    Optional<DepartmentReport> findByDepartmentAndWeekStartDate(String department, LocalDate weekStartDate);

    @Query("SELECT r FROM DepartmentReport r WHERE r.department = ?1 ORDER BY r.weekStartDate DESC")
    List<DepartmentReport> findByDepartment(String department);
}
