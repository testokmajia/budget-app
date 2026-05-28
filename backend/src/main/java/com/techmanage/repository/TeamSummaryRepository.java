package com.techmanage.repository;

import com.techmanage.entity.TeamSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamSummaryRepository extends JpaRepository<TeamSummary, Long> {

    Optional<TeamSummary> findByTeamNameAndWeekStartDate(String teamName, LocalDate weekStartDate);

    List<TeamSummary> findByLeaderIdOrderByWeekStartDateDesc(Long leaderId);

    List<TeamSummary> findByWeekStartDate(LocalDate weekStartDate);
}
