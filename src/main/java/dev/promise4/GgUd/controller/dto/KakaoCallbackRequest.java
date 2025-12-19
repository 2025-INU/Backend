package dev.promise4.GgUd.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카카오 로그인 콜백 요청
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "카카오 로그인 콜백 요청")
public class KakaoCallbackRequest {

    @NotBlank(message = "인가 코드는 필수입니다")
    @Schema(description = "카카오에서 발급한 인가 코드", example = "abc123...")
    private String code;

    @Schema(description = "CSRF 방지를 위한 state 값 (선택)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String state;
}
