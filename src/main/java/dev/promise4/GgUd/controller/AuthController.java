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
         * 카카오 로그인 URL 조회
         */
        @GetMapping("/kakao/login-url")
        @Operation(summary = "카카오 로그인 URL 조회", description = "카카오 OAuth2 인증 페이지로 리다이렉트하기 위한 URL을 반환합니다. " +
                        "클라이언트는 이 URL로 사용자를 리다이렉트해야 합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "로그인 URL 조회 성공", content = @Content(schema = @Schema(implementation = KakaoLoginUrlResponse.class)))
        })
        public ResponseEntity<KakaoLoginUrlResponse> getKakaoLoginUrl() {
                log.debug("GET /api/v1/auth/kakao/login-url");
                KakaoLoginUrlResponse response = authService.getKakaoLoginUrl();
                return ResponseEntity.ok(response);
        }

        /**
         * 카카오 로그인 콜백 처리 (브라우저 리다이렉트용 - GET)
         */
        @GetMapping("/kakao/callback")
        @Operation(summary = "카카오 로그인 콜백 (GET)", description = "카카오 OAuth2 인증 후 브라우저 리다이렉트를 처리합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
                        @ApiResponse(responseCode = "400", description = "잘못된 인가 코드")
        })
        public ResponseEntity<LoginResponse> kakaoCallbackGet(
                        @RequestParam String code,
                        @RequestParam(required = false) String state) {
                log.debug("GET /api/v1/auth/kakao/callback - code: {}...",
                                code.substring(0, Math.min(10, code.length())));

                LoginResponse response = authService.processKakaoLogin(code);
                return ResponseEntity.ok(response);
        }

        /**
         * 카카오 로그인 콜백 처리 (Swagger 테스트용 - POST)
         */
        @PostMapping("/kakao/callback")
        @Operation(summary = "카카오 로그인 콜백 (POST)", description = "카카오 OAuth2 인증 후 콜백을 처리합니다. " +
                        "인가 코드를 받아 토큰을 교환하고, 사용자 정보를 조회한 뒤 JWT를 발급합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
                        @ApiResponse(responseCode = "400", description = "잘못된 인가 코드"),
                        @ApiResponse(responseCode = "500", description = "카카오 API 호출 실패")
        })
        public ResponseEntity<LoginResponse> kakaoCallbackPost(
                        @Valid @RequestBody KakaoCallbackRequest request) {
                log.debug("POST /api/v1/auth/kakao/callback - code: {}...",
                                request.getCode().substring(0, Math.min(10, request.getCode().length())));

                LoginResponse response = authService.processKakaoLogin(request.getCode());
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
