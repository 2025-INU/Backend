package dev.promise4.GgUd.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 토큰 생성 및 검증 Provider
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        // 비밀 키가 충분히 길지 않으면 패딩 추가
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.length() < 32) {
            secret = "default-secret-key-for-development-only-change-in-production!!";
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Access Token 생성
     * 
     * @param userId 사용자 ID
     * @return 생성된 Access Token
     */
    public String createAccessToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("type", "access")
                .signWith(secretKey)
                .compact();
    }

    /**
     * Refresh Token 생성
     * 
     * @param userId 사용자 ID
     * @return 생성된 Refresh Token
     */
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString()) // jti claim for token identification
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("type", "refresh")
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰 유효성 검증
     * 
     * @param token JWT 토큰
     * @return 유효 여부
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT token is malformed: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT token is empty or null: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 토큰에서 사용자 ID 추출
     * 
     * @param token JWT 토큰
     * @return 사용자 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    /**
     * 토큰에서 JTI (토큰 ID) 추출 (Refresh Token용)
     * 
     * @param token JWT 토큰
     * @return JTI
     */
    public String getTokenIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getId();
    }

    /**
     * 토큰 만료 시간 조회
     * 
     * @param token JWT 토큰
     * @return 만료 시간
     */
    public Date getExpirationFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getExpiration();
    }

    /**
     * Access Token 만료 시간 반환 (밀리초)
     */
    public long getAccessTokenExpiration() {
        return jwtProperties.getAccessTokenExpiration();
    }

    /**
     * Refresh Token 만료 시간 반환 (밀리초)
     */
    public long getRefreshTokenExpiration() {
        return jwtProperties.getRefreshTokenExpiration();
    }
}
