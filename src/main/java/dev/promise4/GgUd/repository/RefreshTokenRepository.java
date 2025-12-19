package dev.promise4.GgUd.repository;

import dev.promise4.GgUd.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Refresh Token Repository
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 토큰 ID로 리프레시 토큰 조회
     */
    Optional<RefreshToken> findByTokenId(String tokenId);

    /**
     * 사용자 ID와 토큰 ID로 리프레시 토큰 조회
     */
    Optional<RefreshToken> findByUserIdAndTokenId(Long userId, String tokenId);

    /**
     * 사용자의 모든 유효한 리프레시 토큰 무효화
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.userId = :userId AND rt.revoked = false")
    int revokeAllByUserId(@Param("userId") Long userId);

    /**
     * 만료되었거나 무효화된 토큰 삭제
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now OR rt.revoked = true")
    int deleteExpiredOrRevokedTokens(@Param("now") LocalDateTime now);

    /**
     * 사용자 ID로 리프레시 토큰 존재 여부 확인
     */
    boolean existsByUserIdAndRevokedFalse(Long userId);
}
