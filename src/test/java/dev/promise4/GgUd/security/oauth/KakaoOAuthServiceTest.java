package dev.promise4.GgUd.security.oauth;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import dev.promise4.GgUd.entity.User;
import dev.promise4.GgUd.entity.UserRole;
import dev.promise4.GgUd.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KakaoOAuthService 테스트")
class KakaoOAuthServiceTest {

    private static WireMockServer wireMockServer;

    @Mock
    private UserRepository userRepository;

    private KakaoOAuthService kakaoOAuthService;
    private KakaoOAuthProperties kakaoProperties;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(0); // Random port
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();

        // Setup properties with WireMock URLs
        kakaoProperties = new KakaoOAuthProperties();
        kakaoProperties.setClientId("test-client-id");
        kakaoProperties.setClientSecret("test-client-secret");
        kakaoProperties.setRedirectUri("http://localhost:8080/callback");
        kakaoProperties.setTokenUri(wireMockServer.baseUrl() + "/oauth/token");
        kakaoProperties.setUserInfoUri(wireMockServer.baseUrl() + "/v2/user/me");
        kakaoProperties.setAuthorizationUri(wireMockServer.baseUrl() + "/oauth/authorize");

        // Create RestClient for tests
        RestClient restClient = RestClient.builder().build();

        kakaoOAuthService = new KakaoOAuthService(kakaoProperties, userRepository, restClient);
    }

    @Nested
    @DisplayName("getKakaoLoginUrl 테스트")
    class GetKakaoLoginUrlTest {

        @Test
        @DisplayName("카카오 로그인 URL을 생성할 수 있다")
        void getKakaoLoginUrl_returnsValidUrl() {
            // when
            KakaoOAuthService.KakaoLoginUrlResponse response = kakaoOAuthService.getKakaoLoginUrl();

            // then
            assertThat(response.loginUrl()).isNotNull();
            assertThat(response.loginUrl()).contains("client_id=test-client-id");
            assertThat(response.loginUrl()).contains("redirect_uri=http://localhost:8080/callback");
            assertThat(response.loginUrl()).contains("response_type=code");
            assertThat(response.state()).isNotNull();
            assertThat(response.state()).hasSize(36); // UUID format
        }

        @Test
        @DisplayName("매번 다른 state 값을 생성한다")
        void getKakaoLoginUrl_generatesUniqueState() {
            // when
            KakaoOAuthService.KakaoLoginUrlResponse response1 = kakaoOAuthService.getKakaoLoginUrl();
            KakaoOAuthService.KakaoLoginUrlResponse response2 = kakaoOAuthService.getKakaoLoginUrl();

            // then
            assertThat(response1.state()).isNotEqualTo(response2.state());
        }
    }

    @Nested
    @DisplayName("processKakaoLogin 테스트")
    class ProcessKakaoLoginTest {

        private static final String TOKEN_RESPONSE = """
                {
                    "access_token": "test-access-token",
                    "token_type": "bearer",
                    "refresh_token": "test-refresh-token",
                    "expires_in": 21599,
                    "refresh_token_expires_in": 5183999,
                    "scope": "profile_nickname account_email"
                }
                """;

        private static final String USER_INFO_RESPONSE = """
                {
                    "id": 12345678,
                    "connected_at": "2024-01-01T00:00:00Z",
                    "properties": {
                        "nickname": "홍길동",
                        "profile_image": "https://k.kakaocdn.net/profile.jpg"
                    },
                    "kakao_account": {
                        "profile_nickname_needs_agreement": false,
                        "profile": {
                            "nickname": "홍길동",
                            "profile_image_url": "https://k.kakaocdn.net/profile.jpg",
                            "is_default_image": false
                        },
                        "has_email": true,
                        "email_needs_agreement": false,
                        "is_email_valid": true,
                        "is_email_verified": true,
                        "email": "hong@kakao.com"
                    }
                }
                """;

        @BeforeEach
        void setupStubs() {
            // Token endpoint stub
            stubFor(post(urlPathEqualTo("/oauth/token"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody(TOKEN_RESPONSE)));

            // User info endpoint stub
            stubFor(get(urlPathEqualTo("/v2/user/me"))
                    .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody(USER_INFO_RESPONSE)));
        }

        @Test
        @DisplayName("신규 사용자가 로그인하면 회원가입 후 User를 반환한다")
        void processKakaoLogin_newUser_createsAndReturnsUser() {
            // given
            when(userRepository.findByKakaoId("12345678")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                // Simulate ID assignment
                return User.builder()
                        .kakaoId(user.getKakaoId())
                        .nickname(user.getNickname())
                        .email(user.getEmail())
                        .profileImageUrl(user.getProfileImageUrl())
                        .role(user.getRole())
                        .build();
            });

            // when
            User user = kakaoOAuthService.processKakaoLogin("test-authorization-code");

            // then
            assertThat(user).isNotNull();
            assertThat(user.getKakaoId()).isEqualTo("12345678");
            assertThat(user.getNickname()).isEqualTo("홍길동");
            assertThat(user.getEmail()).isEqualTo("hong@kakao.com");
            assertThat(user.getProfileImageUrl()).isEqualTo("https://k.kakaocdn.net/profile.jpg");
            assertThat(user.getRole()).isEqualTo(UserRole.USER);

            // Verify save was called
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("기존 사용자가 로그인하면 프로필을 업데이트하고 User를 반환한다")
        void processKakaoLogin_existingUser_updatesAndReturnsUser() {
            // given - 기존 사용자 생성
            User existingUser = User.builder()
                    .kakaoId("12345678")
                    .nickname("기존닉네임")
                    .email("old@email.com")
                    .role(UserRole.USER)
                    .build();
            when(userRepository.findByKakaoId("12345678")).thenReturn(Optional.of(existingUser));

            // when
            User user = kakaoOAuthService.processKakaoLogin("test-authorization-code");

            // then
            assertThat(user).isNotNull();
            assertThat(user.getKakaoId()).isEqualTo("12345678");
            assertThat(user.getNickname()).isEqualTo("홍길동"); // Updated
            assertThat(user.getEmail()).isEqualTo("hong@kakao.com"); // Updated

            // Verify save was NOT called (existing user updated in-place)
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("카카오 API 호출 시 올바른 파라미터를 전송한다")
        void processKakaoLogin_sendsCorrectParameters() {
            // given
            when(userRepository.findByKakaoId(anyString())).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            kakaoOAuthService.processKakaoLogin("test-code-123");

            // then - Verify token request parameters
            verify(postRequestedFor(urlPathEqualTo("/oauth/token"))
                    .withRequestBody(containing("grant_type=authorization_code"))
                    .withRequestBody(containing("client_id=test-client-id"))
                    .withRequestBody(containing("client_secret=test-client-secret"))
                    .withRequestBody(containing("redirect_uri="))
                    .withRequestBody(containing("code=test-code-123")));

            // Verify user info request header
            verify(getRequestedFor(urlPathEqualTo("/v2/user/me"))
                    .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer test-access-token")));
        }
    }

    @Nested
    @DisplayName("processKakaoLoginWithToken 테스트")
    class ProcessKakaoLoginWithTokenTest {

        private static final String USER_INFO_RESPONSE = """
                {
                    "id": 12345678,
                    "connected_at": "2024-01-01T00:00:00Z",
                    "properties": {
                        "nickname": "홍길동",
                        "profile_image": "https://k.kakaocdn.net/profile.jpg"
                    },
                    "kakao_account": {
                        "profile_nickname_needs_agreement": false,
                        "profile": {
                            "nickname": "홍길동",
                            "profile_image_url": "https://k.kakaocdn.net/profile.jpg",
                            "is_default_image": false
                        },
                        "has_email": true,
                        "email_needs_agreement": false,
                        "is_email_valid": true,
                        "is_email_verified": true,
                        "email": "hong@kakao.com"
                    }
                }
                """;

        @BeforeEach
        void setupStubs() {
            // User info endpoint stub
            stubFor(get(urlPathEqualTo("/v2/user/me"))
                    .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody(USER_INFO_RESPONSE)));
        }

        @Test
        @DisplayName("카카오 SDK 토큰으로 신규 사용자 로그인을 처리한다")
        void processKakaoLoginWithToken_newUser_success() {
            // given
            when(userRepository.findByKakaoId("12345678")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            User user = kakaoOAuthService.processKakaoLoginWithToken("sdk-access-token");

            // then
            assertThat(user).isNotNull();
            assertThat(user.getKakaoId()).isEqualTo("12345678");
            assertThat(user.getNickname()).isEqualTo("홍길동");
            assertThat(user.getEmail()).isEqualTo("hong@kakao.com");

            // 토큰 교환 없이 바로 사용자 정보 조회
            verify(getRequestedFor(urlPathEqualTo("/v2/user/me"))
                    .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer sdk-access-token")));

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("카카오 SDK 토큰으로 기존 사용자 로그인을 처리한다")
        void processKakaoLoginWithToken_existingUser_success() {
            // given
            User existingUser = User.builder()
                    .kakaoId("12345678")
                    .nickname("기존닉네임")
                    .email("old@email.com")
                    .role(UserRole.USER)
                    .build();
            when(userRepository.findByKakaoId("12345678")).thenReturn(Optional.of(existingUser));

            // when
            User user = kakaoOAuthService.processKakaoLoginWithToken("sdk-access-token");

            // then
            assertThat(user.getNickname()).isEqualTo("홍길동");
            assertThat(user.getEmail()).isEqualTo("hong@kakao.com");
            verify(userRepository, never()).save(any(User.class));
        }
    }
}
