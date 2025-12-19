package dev.promise4.GgUd.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토큰 갱신 요청
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "토큰 갱신 요청")
public class TokenRefreshRequest {

    @NotBlank(message = "Refresh Token은 필수입니다")
    @Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;
}
