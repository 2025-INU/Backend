package dev.promise4.GgUd.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

/**
 * Redis 기반 JWT 토큰 블랙리스트 서비스
 * 로그아웃 시 토큰을 무효화하고 검증 시 블랙리스트 확인
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";
    private static final String USER_TOKENS_KEY_PREFIX = "user:tokens:";

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 토큰을 블랙리스트에 추가
     *
     * @param tokenId    토큰 ID (JTI)
     * @param userId     사용자 ID
     * @param expiration 토큰 만료 시간
     */
    public void addToBlacklist(String tokenId, Long userId, Date expiration) {
        String key = BLACKLIST_KEY_PREFIX + tokenId;

        // 토큰 만료 시간까지만 블랙리스트에 유지
        long ttlMillis = expiration.getTime() - System.currentTimeMillis();
        if (ttlMillis > 0) {
            redisTemplate.opsForValue().set(key, userId.toString(), Duration.ofMillis(ttlMillis));
            log.debug("Token added to blacklist: tokenId={}, userId={}, ttl={}ms", tokenId, userId, ttlMillis);
        }
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인
     *
     * @param tokenId 토큰 ID (JTI)
     * @return 블랙리스트에 있으면 true
     */
    public boolean isBlacklisted(String tokenId) {
        String key = BLACKLIST_KEY_PREFIX + tokenId;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 사용자의 모든 토큰을 블랙리스트에 추가 (로그아웃 시)
     *
     * @param userId               사용자 ID
     * @param refreshTokenExpiration 리프레시 토큰 만료 기간 (밀리초)
     */
    public void revokeAllUserTokens(Long userId, long refreshTokenExpiration) {
        String key = USER_TOKENS_KEY_PREFIX + userId;

        // 사용자의 모든 세션 무효화 마킹
        redisTemplate.opsForValue().set(key, System.currentTimeMillis(),
                Duration.ofMillis(refreshTokenExpiration));
        log.info("All tokens revoked for userId: {}", userId);
    }

    /**
     * 사용자 토큰이 전체 무효화 시점 이전에 발급되었는지 확인
     *
     * @param userId    사용자 ID
     * @param issuedAt  토큰 발급 시간
     * @return 무효화되었으면 true
     */
    public boolean isUserTokensRevoked(Long userId, Date issuedAt) {
        String key = USER_TOKENS_KEY_PREFIX + userId;
        Object revokedAt = redisTemplate.opsForValue().get(key);

        if (revokedAt != null) {
            long revokedTimestamp = Long.parseLong(revokedAt.toString());
            return issuedAt.getTime() < revokedTimestamp;
        }
        return false;
    }
}
