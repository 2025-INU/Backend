package dev.promise4.GgUd.security.oauth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 카카오 OAuth2 설정 프로퍼티 (모바일 SDK 전용)
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kakao")
public class KakaoOAuthProperties {

    private String clientId;
    private String clientSecret;
    private String redirectUri;

    // 카카오 API URL (사용자 정보 조회용)
    private String userInfoUri = "https://kapi.kakao.com/v2/user/me";
}
