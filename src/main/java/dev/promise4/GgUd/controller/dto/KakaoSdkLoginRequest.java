package dev.promise4.GgUd.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카카오 SDK 로그인 요청 (모바일용)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "카카오 SDK 로그인 요청")
public class KakaoSdkLoginRequest {

    @NotBlank(message = "카카오 액세스 토큰은 필수입니다")
    @Schema(description = "카카오 SDK에서 발급받은 액세스 토큰", example = "KakaoAK_abc123...")
    private String kakaoAccessToken;
}
