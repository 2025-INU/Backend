package dev.promise4.GgUd.security.oauth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 카카오 OAuth2 설정 프로퍼티
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kakao")
public class KakaoOAuthProperties {

    private String clientId;
    private String clientSecret;
    private String redirectUri;

    // 카카오 API URLs
    private String authorizationUri = "https://kauth.kakao.com/oauth/authorize";
    private String tokenUri = "https://kauth.kakao.com/oauth/token";
    private String userInfoUri = "https://kapi.kakao.com/v2/user/me";

    /**
     * 카카오 로그인 URL 생성
     */
    public String buildAuthorizationUrl(String state) {
        return String.format("%s?client_id=%s&redirect_uri=%s&response_type=code&state=%s",
                authorizationUri, clientId, redirectUri, state);
    }
}
