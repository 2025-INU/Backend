package dev.promise4.GgUd.security.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카카오 토큰 응답 DTO
 * 카카오 OAuth2 토큰 엔드포인트 응답을 매핑
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KakaoTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("expires_in")
    private Long expiresIn;

    @JsonProperty("refresh_token_expires_in")
    private Long refreshTokenExpiresIn;

    private String scope;
}
