package dev.promise4.GgUd.controller;

import dev.promise4.GgUd.controller.dto.*;
import dev.promise4.GgUd.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 REST API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 API")
public class AuthController {

        private final AuthService authService;

        /**
         * 카카오 SDK 로그인 (모바일용)
         * 모바일 앱에서 카카오 SDK로 받은 액세스 토큰으로 로그인 처리
         */
        @PostMapping("/kakao/login")
        @Operation(summary = "카카오 SDK 로그인 (모바일용)", description = "모바일 앱에서 카카오 SDK로 받은 액세스 토큰을 전달하면, " +
                        "서버가 해당 토큰으로 카카오 사용자 정보를 조회하고 JWT를 발급합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
                        @ApiResponse(responseCode = "400", description = "유효하지 않은 카카오 액세스 토큰"),
                        @ApiResponse(responseCode = "500", description = "카카오 API 호출 실패")
        })
        public ResponseEntity<LoginResponse> kakaoSdkLogin(
                        @Valid @RequestBody KakaoSdkLoginRequest request) {
                log.debug("POST /api/v1/auth/kakao/login - SDK token login");

                LoginResponse response = authService.processKakaoLoginWithToken(request.getKakaoAccessToken());
                return ResponseEntity.ok(response);
        }

        /**
         * 토큰 갱신
         */
        @PostMapping("/refresh")
        @Operation(summary = "토큰 갱신", description = "Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "토큰 갱신 성공", content = @Content(schema = @Schema(implementation = TokenRefreshResponse.class))),
                        @ApiResponse(responseCode = "400", description = "유효하지 않거나 만료된 Refresh Token")
        })
        public ResponseEntity<TokenRefreshResponse> refreshToken(
                        @Valid @RequestBody TokenRefreshRequest request) {
                log.debug("POST /api/v1/auth/refresh");
                TokenRefreshResponse response = authService.refreshToken(request.getRefreshToken());
                return ResponseEntity.ok(response);
        }

        /**
         * 로그아웃
         */
        @PostMapping("/logout")
        @Operation(summary = "로그아웃", description = "현재 사용자의 모든 Refresh Token을 무효화합니다. " +
                        "Authorization 헤더에 Access Token이 필요합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
                        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
        })
        public ResponseEntity<Void> logout(
                        @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
                log.debug("POST /api/v1/auth/logout - userId: {}", userId);

                if (userId == null) {
                        return ResponseEntity.status(401).build();
                }

                authService.logout(userId);
                return ResponseEntity.ok().build();
        }
}
