package com.techmanage.repository;

import com.techmanage.entity.QrLoginSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public interface QrLoginSessionRepository extends JpaRepository<QrLoginSession, Long> {
    Optional<QrLoginSession> findBySessionId(String sessionId);
    Optional<QrLoginSession> findByShortCode(String shortCode);
    void deleteByExpiresAtBefore(LocalDateTime time);
}
