package dev.promise4.GgUd.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토큰 갱신 응답
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "토큰 갱신 응답")
public class TokenRefreshResponse {

    @Schema(description = "새로운 Access Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "토큰 타입", example = "Bearer")
    private String tokenType;

    @Schema(description = "Access Token 만료 시간 (초)", example = "3600")
    private Long expiresIn;
}
