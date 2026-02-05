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
        kakaoProperties.setUserInfoUri(wireMockServer.baseUrl() + "/v2/user/me");

        // Create RestClient for tests
        RestClient restClient = RestClient.builder().build();

        kakaoOAuthService = new KakaoOAuthService(kakaoProperties, userRepository, restClient);
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
