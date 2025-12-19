package dev.promise4.GgUd.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카카오 로그인 URL 응답
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "카카오 로그인 URL 응답")
public class KakaoLoginUrlResponse {

    @Schema(description = "카카오 로그인 페이지 URL", example = "https://kauth.kakao.com/oauth/authorize?...")
    private String loginUrl;

    @Schema(description = "CSRF 방지를 위한 state 값", example = "550e8400-e29b-41d4-a716-446655440000")
    private String state;
}
