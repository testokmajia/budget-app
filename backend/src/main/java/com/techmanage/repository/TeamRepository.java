package com.techmanage.repository;

import com.techmanage.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findAllByOrderByIdAsc();
    boolean existsByName(String name);
    Optional<Team> findByName(String name);
    List<Team> findByLeader(String leader);
}
