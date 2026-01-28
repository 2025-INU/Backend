package dev.promise4.GgUd.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter 테스트")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider, tokenBlacklistService);
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("doFilterInternal 테스트")
    class DoFilterInternalTest {

        @Test
        @DisplayName("유효한 토큰이 있으면 SecurityContext에 인증 정보를 설정한다")
        void doFilterInternal_withValidToken_setsAuthentication() throws ServletException, IOException {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer valid-token");
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken("valid-token")).thenReturn(1L);
            when(jwtTokenProvider.getIssuedAtFromToken("valid-token")).thenReturn(new java.util.Date());
            when(tokenBlacklistService.isUserTokensRevoked(anyLong(), any())).thenReturn(false);

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(1L);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("토큰이 없으면 SecurityContext가 비어있다")
        void doFilterInternal_withoutToken_noAuthentication() throws ServletException, IOException {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("유효하지 않은 토큰이면 SecurityContext가 비어있다")
        void doFilterInternal_withInvalidToken_noAuthentication() throws ServletException, IOException {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer invalid-token");
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Bearer 없이 토큰만 있으면 SecurityContext가 비어있다")
        void doFilterInternal_withoutBearerPrefix_noAuthentication() throws ServletException, IOException {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "some-token-without-bearer");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(jwtTokenProvider, never()).validateToken(anyString());
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("extractTokenFromRequest 테스트")
    class ExtractTokenFromRequestTest {

        @Test
        @DisplayName("Bearer 토큰에서 JWT를 추출한다")
        void extractTokenFromRequest_withBearerToken_returnsToken() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer test-jwt-token");

            // when
            String token = jwtAuthenticationFilter.extractTokenFromRequest(request);

            // then
            assertThat(token).isEqualTo("test-jwt-token");
        }

        @Test
        @DisplayName("Authorization 헤더가 없으면 null을 반환한다")
        void extractTokenFromRequest_withoutAuthHeader_returnsNull() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();

            // when
            String token = jwtAuthenticationFilter.extractTokenFromRequest(request);

            // then
            assertThat(token).isNull();
        }

        @Test
        @DisplayName("Bearer로 시작하지 않으면 null을 반환한다")
        void extractTokenFromRequest_withoutBearer_returnsNull() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Basic some-credentials");

            // when
            String token = jwtAuthenticationFilter.extractTokenFromRequest(request);

            // then
            assertThat(token).isNull();
        }

        @Test
        @DisplayName("빈 Authorization 헤더면 null을 반환한다")
        void extractTokenFromRequest_withEmptyHeader_returnsNull() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "");

            // when
            String token = jwtAuthenticationFilter.extractTokenFromRequest(request);

            // then
            assertThat(token).isNull();
        }
    }

    @Nested
    @DisplayName("shouldNotFilter 테스트")
    class ShouldNotFilterTest {

        @Test
        @DisplayName("인증 엔드포인트는 필터링하지 않는다")
        void shouldNotFilter_authEndpoint_returnsTrue() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/auth/login");

            // when
            boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Actuator 엔드포인트는 필터링하지 않는다")
        void shouldNotFilter_actuatorEndpoint_returnsTrue() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/actuator/health");

            // when
            boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Swagger 엔드포인트는 필터링하지 않는다")
        void shouldNotFilter_swaggerEndpoint_returnsTrue() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/swagger-ui/index.html");

            // when
            boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("API 엔드포인트는 필터링한다")
        void shouldNotFilter_apiEndpoint_returnsFalse() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/meetings");

            // when
            boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

            // then
            assertThat(result).isFalse();
        }
    }
}
