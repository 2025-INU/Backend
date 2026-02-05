package dev.promise4.GgUd.service;

import dev.promise4.GgUd.controller.dto.KakaoLoginUrlResponse;
import dev.promise4.GgUd.controller.dto.LoginResponse;
import dev.promise4.GgUd.controller.dto.TokenRefreshResponse;
import dev.promise4.GgUd.entity.RefreshToken;
import dev.promise4.GgUd.entity.User;
import dev.promise4.GgUd.entity.UserRole;
import dev.promise4.GgUd.repository.RefreshTokenRepository;
import dev.promise4.GgUd.security.jwt.JwtTokenProvider;
import dev.promise4.GgUd.security.jwt.TokenBlacklistService;
import dev.promise4.GgUd.security.oauth.KakaoOAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @Mock
    private KakaoOAuthService kakaoOAuthService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("getKakaoLoginUrl 테스트")
    class GetKakaoLoginUrlTest {

        @Test
        @DisplayName("카카오 로그인 URL을 반환한다")
        void getKakaoLoginUrl_success() {
            // given
            when(kakaoOAuthService.getKakaoLoginUrl())
                    .thenReturn(new KakaoOAuthService.KakaoLoginUrlResponse(
                            "https://kauth.kakao.com/oauth/authorize?...",
                            "test-state"));

            // when
            KakaoLoginUrlResponse response = authService.getKakaoLoginUrl();

            // then
            assertThat(response.getLoginUrl()).contains("kauth.kakao.com");
            assertThat(response.getState()).isEqualTo("test-state");
        }
    }

    @Nested
    @DisplayName("processKakaoLogin 테스트")
    class ProcessKakaoLoginTest {

        private User mockUser;

        @BeforeEach
        void setUp() {
            mockUser = User.builder()
                    .kakaoId("12345")
                    .nickname("테스트유저")
                    .email("test@kakao.com")
                    .role(UserRole.USER)
                    .build();
            // Simulate ID set
            try {
                var field = User.class.getDeclaredField("id");
                field.setAccessible(true);
                field.set(mockUser, 1L);
            } catch (Exception ignored) {
            }
        }

        @Test
        @DisplayName("카카오 로그인을 성공적으로 처리한다")
        void processKakaoLogin_success() {
            // given
            when(kakaoOAuthService.processKakaoLogin("test-code")).thenReturn(mockUser);
            when(jwtTokenProvider.createAccessToken(1L)).thenReturn("access-token");
            when(jwtTokenProvider.createRefreshToken(1L)).thenReturn("refresh-token");
            when(jwtTokenProvider.getTokenIdFromToken("refresh-token")).thenReturn("token-id");
            when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);
            when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(604800000L);
            when(refreshTokenRepository.revokeAllByUserId(1L)).thenReturn(0);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            // when
            LoginResponse response = authService.processKakaoLogin("test-code");

            // then
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getExpiresIn()).isEqualTo(3600L);
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getNickname()).isEqualTo("테스트유저");

            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }
    }

    @Nested
    @DisplayName("processKakaoLoginWithToken 테스트")
    class ProcessKakaoLoginWithTokenTest {

        private User mockUser;

        @BeforeEach
        void setUp() {
            mockUser = User.builder()
                    .kakaoId("12345")
                    .nickname("테스트유저")
                    .email("test@kakao.com")
                    .role(UserRole.USER)
                    .build();
            try {
                var field = User.class.getDeclaredField("id");
                field.setAccessible(true);
                field.set(mockUser, 1L);
            } catch (Exception ignored) {
            }
        }

        @Test
        @DisplayName("카카오 SDK 토큰으로 로그인을 성공적으로 처리한다")
        void processKakaoLoginWithToken_success() {
            // given
            when(kakaoOAuthService.processKakaoLoginWithToken("kakao-token")).thenReturn(mockUser);
            when(jwtTokenProvider.createAccessToken(1L)).thenReturn("access-token");
            when(jwtTokenProvider.createRefreshToken(1L)).thenReturn("refresh-token");
            when(jwtTokenProvider.getTokenIdFromToken("refresh-token")).thenReturn("token-id");
            when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);
            when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(604800000L);
            when(refreshTokenRepository.revokeAllByUserId(1L)).thenReturn(0);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            // when
            LoginResponse response = authService.processKakaoLoginWithToken("kakao-token");

            // then
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getExpiresIn()).isEqualTo(3600L);
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getNickname()).isEqualTo("테스트유저");

            verify(kakaoOAuthService).processKakaoLoginWithToken("kakao-token");
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }
    }

    @Nested
    @DisplayName("refreshToken 테스트")
    class RefreshTokenTest {

        @Test
        @DisplayName("유효한 Refresh Token으로 새 Access Token을 발급한다")
        void refreshToken_withValidToken_success() {
            // given
            String refreshToken = "valid-refresh-token";
            RefreshToken storedToken = RefreshToken.builder()
                    .userId(1L)
                    .tokenId("token-id")
                    .expiryDate(LocalDateTime.now().plusDays(7))
                    .build();

            when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn(1L);
            when(jwtTokenProvider.getTokenIdFromToken(refreshToken)).thenReturn("token-id");
            when(refreshTokenRepository.findByTokenId("token-id")).thenReturn(Optional.of(storedToken));
            when(jwtTokenProvider.createAccessToken(1L)).thenReturn("new-access-token");
            when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);

            // when
            TokenRefreshResponse response = authService.refreshToken(refreshToken);

            // then
            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getExpiresIn()).isEqualTo(3600L);
        }

        @Test
        @DisplayName("유효하지 않은 토큰이면 예외를 발생시킨다")
        void refreshToken_withInvalidToken_throwsException() {
            // given
            when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.refreshToken("invalid-token"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("유효하지 않은 Refresh Token입니다");
        }

        @Test
        @DisplayName("DB에 없는 토큰이면 예외를 발생시킨다")
        void refreshToken_withNonExistentToken_throwsException() {
            // given
            when(jwtTokenProvider.validateToken("token")).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken("token")).thenReturn(1L);
            when(jwtTokenProvider.getTokenIdFromToken("token")).thenReturn("non-existent-id");
            when(refreshTokenRepository.findByTokenId("non-existent-id")).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.refreshToken("token"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 Refresh Token입니다");
        }

        @Test
        @DisplayName("무효화된 토큰이면 예외를 발생시킨다")
        void refreshToken_withRevokedToken_throwsException() {
            // given
            RefreshToken revokedToken = RefreshToken.builder()
                    .userId(1L)
                    .tokenId("token-id")
                    .expiryDate(LocalDateTime.now().plusDays(7))
                    .build();
            revokedToken.revoke();

            when(jwtTokenProvider.validateToken("token")).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken("token")).thenReturn(1L);
            when(jwtTokenProvider.getTokenIdFromToken("token")).thenReturn("token-id");
            when(refreshTokenRepository.findByTokenId("token-id")).thenReturn(Optional.of(revokedToken));

            // when & then
            assertThatThrownBy(() -> authService.refreshToken("token"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("만료되었거나 무효화된 Refresh Token입니다");
        }
    }

    @Nested
    @DisplayName("logout 테스트")
    class LogoutTest {

        @Test
        @DisplayName("사용자의 모든 Refresh Token을 무효화한다")
        void logout_success() {
            // given
            when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(604800000L);
            when(refreshTokenRepository.revokeAllByUserId(1L)).thenReturn(2);
            doNothing().when(tokenBlacklistService).revokeAllUserTokens(anyLong(), anyLong());

            // when
            authService.logout(1L);

            // then
            verify(tokenBlacklistService).revokeAllUserTokens(1L, 604800000L);
            verify(refreshTokenRepository).revokeAllByUserId(1L);
        }
    }
}
