package dev.promise4.GgUd.entity;

import dev.promise4.GgUd.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Refresh Token 엔티티
 * PostgreSQL에 저장되는 리프레시 토큰 정보
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
        @Index(name = "idx_refresh_tokens_token_id", columnList = "token_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RefreshToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * JWT의 jti claim과 매칭되는 토큰 ID (UUID)
     */
    @Column(name = "token_id", nullable = false, unique = true, length = 36)
    private String tokenId;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    /**
     * 토큰 무효화 여부
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    /**
     * 토큰 무효화
     */
    public void revoke() {
        this.revoked = true;
    }

    /**
     * 토큰 만료 여부 확인
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    /**
     * 토큰 유효성 확인 (무효화되지 않고 만료되지 않음)
     */
    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
