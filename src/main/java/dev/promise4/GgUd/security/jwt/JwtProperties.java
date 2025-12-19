package dev.promise4.GgUd.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 설정 프로퍼티
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT 서명에 사용할 비밀 키 (최소 256비트 권장)
     */
    private String secret;

    /**
     * Access Token 만료 시간 (밀리초, 기본 1시간)
     */
    private long accessTokenExpiration = 3600000L; // 1 hour

    /**
     * Refresh Token 만료 시간 (밀리초, 기본 7일)
     */
    private long refreshTokenExpiration = 604800000L; // 7 days
}
