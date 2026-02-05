package dev.promise4.GgUd.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.promise4.GgUd.controller.dto.*;
import dev.promise4.GgUd.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController 테스트")
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("POST /api/v1/auth/kakao/login")
    class KakaoSdkLoginTest {

        @Test
        @DisplayName("카카오 SDK 토큰으로 로그인을 성공적으로 처리한다")
        void kakaoSdkLogin_success() throws Exception {
            // given
            KakaoSdkLoginRequest request = new KakaoSdkLoginRequest("kakao-access-token-123");

            LoginResponse response = LoginResponse.builder()
                    .accessToken("jwt-access-token")
                    .refreshToken("jwt-refresh-token")
                    .tokenType("Bearer")
                    .expiresIn(3600L)
                    .userId(1L)
                    .nickname("홍길동")
                    .build();

            when(authService.processKakaoLoginWithToken("kakao-access-token-123")).thenReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/auth/kakao/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("jwt-access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("jwt-refresh-token"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(3600))
                    .andExpect(jsonPath("$.userId").value(1))
                    .andExpect(jsonPath("$.nickname").value("홍길동"));

            verify(authService).processKakaoLoginWithToken("kakao-access-token-123");
        }

        @Test
        @DisplayName("빈 카카오 토큰으로 요청하면 실패한다")
        void kakaoSdkLogin_withEmptyToken_returnsBadRequest() throws Exception {
            // given
            KakaoSdkLoginRequest request = new KakaoSdkLoginRequest("");

            // when & then
            mockMvc.perform(post("/api/v1/auth/kakao/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).processKakaoLoginWithToken(any());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshTokenTest {

        @Test
        @DisplayName("토큰을 성공적으로 갱신한다")
        void refreshToken_success() throws Exception {
            // given
            TokenRefreshRequest request = new TokenRefreshRequest("refresh-token-123");

            TokenRefreshResponse response = TokenRefreshResponse.builder()
                    .accessToken("new-access-token")
                    .tokenType("Bearer")
                    .expiresIn(3600L)
                    .build();

            when(authService.refreshToken("refresh-token-123")).thenReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(3600));

            verify(authService).refreshToken("refresh-token-123");
        }

        @Test
        @DisplayName("빈 Refresh Token이면 400 에러를 반환한다")
        void refreshToken_withEmptyToken_returnsBadRequest() throws Exception {
            // given
            TokenRefreshRequest request = new TokenRefreshRequest("");

            // when & then
            mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).refreshToken(any());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class LogoutTest {

        @Test
        @DisplayName("인증되지 않은 사용자는 401을 반환한다")
        void logout_unauthenticatedUser_returnsUnauthorized() throws Exception {
            // when & then - @AuthenticationPrincipal이 null이면 401
            mockMvc.perform(post("/api/v1/auth/logout"))
                    .andExpect(status().isUnauthorized());

            verify(authService, never()).logout(any());
        }
    }
}
