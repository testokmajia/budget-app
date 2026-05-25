package com.techmanage.repository;

import com.techmanage.entity.SystemInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SystemInfoRepository extends JpaRepository<SystemInfo, Long> {
    List<SystemInfo> findAllByOrderByIdAsc();
    boolean existsByName(String name);
    Optional<SystemInfo> findByName(String name);
}
