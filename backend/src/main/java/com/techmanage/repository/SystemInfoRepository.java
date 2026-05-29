package com.techmanage.repository;

import com.techmanage.entity.SystemInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SystemInfoRepository extends JpaRepository<SystemInfo, Long> {
    List<SystemInfo> findAllByOrderByIdAsc();
    boolean existsByName(String name);
    boolean existsByCode(String code);
    Optional<SystemInfo> findByName(String name);
    Optional<SystemInfo> findByCode(String code);
}
