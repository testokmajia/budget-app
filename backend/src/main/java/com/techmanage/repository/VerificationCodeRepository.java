package com.techmanage.repository;

import com.techmanage.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 密码重置验证码 Repository
 */
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    /** 查找某个邮箱最新且未使用的验证码 */
    Optional<VerificationCode> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);

    /** 通过邮箱和重置令牌查找验证码 */
    Optional<VerificationCode> findTopByEmailAndResetTokenOrderByCreatedAtDesc(
        @Param("email") String email, @Param("resetToken") String resetToken);

    /** 统计某个IP在指定时间范围内的记录数（用于频率限制） */
    @Query("SELECT COUNT(v) FROM VerificationCode v WHERE v.createdAt >= :since")
    long countByCreatedAtAfter(@Param("since") LocalDateTime since);

    /** 删除过期验证码（定时清理） */
    @Modifying
    @Query("DELETE FROM VerificationCode v WHERE v.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
