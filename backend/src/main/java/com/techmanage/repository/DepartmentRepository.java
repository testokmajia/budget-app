package com.techmanage.repository;

import com.techmanage.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long>, JpaSpecificationExecutor<Department> {
    List<Department> findAllByOrderByIdAsc();
    boolean existsByName(String name);
    Optional<Department> findByName(String name);
    List<Department> findByEnabledTrue();
}
