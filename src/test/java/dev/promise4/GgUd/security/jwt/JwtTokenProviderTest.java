package dev.promise4.GgUd.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtTokenProvider 테스트")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-for-jwt-testing-must-be-at-least-256-bits!!");
        jwtProperties.setAccessTokenExpiration(3600000L); // 1 hour
        jwtProperties.setRefreshTokenExpiration(604800000L); // 7 days

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    @Nested
    @DisplayName("createAccessToken 테스트")
    class CreateAccessTokenTest {

        @Test
        @DisplayName("유효한 Access Token을 생성한다")
        void createAccessToken_withValidUserId_returnsToken() {
            // given
            Long userId = 1L;

            // when
            String token = jwtTokenProvider.createAccessToken(userId);

            // then
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
        }

        @Test
        @DisplayName("생성된 Access Token에서 userId를 추출할 수 있다")
        void createAccessToken_extractUserIdFromToken() {
            // given
            Long userId = 123L;

            // when
            String token = jwtTokenProvider.createAccessToken(userId);
            Long extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

            // then
            assertThat(extractedUserId).isEqualTo(userId);
        }

        @Test
        @DisplayName("Access Token의 만료 시간이 1시간 이내이다")
        void createAccessToken_hasCorrectExpiration() {
            // given
            Long userId = 1L;

            // when
            String token = jwtTokenProvider.createAccessToken(userId);
            Date expiration = jwtTokenProvider.getExpirationFromToken(token);

            // then
            long expirationTime = expiration.getTime() - System.currentTimeMillis();
            assertThat(expirationTime).isLessThanOrEqualTo(3600000L); // <= 1 hour
            assertThat(expirationTime).isGreaterThan(3500000L); // > 58 minutes (allow some delta)
        }
    }

    @Nested
    @DisplayName("createRefreshToken 테스트")
    class CreateRefreshTokenTest {

        @Test
        @DisplayName("유효한 Refresh Token을 생성한다")
        void createRefreshToken_withValidUserId_returnsToken() {
            // given
            Long userId = 1L;

            // when
            String token = jwtTokenProvider.createRefreshToken(userId);

            // then
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
        }

        @Test
        @DisplayName("Refresh Token에는 JTI(토큰 ID)가 포함된다")
        void createRefreshToken_containsJti() {
            // given
            Long userId = 1L;

            // when
            String token = jwtTokenProvider.createRefreshToken(userId);
            String tokenId = jwtTokenProvider.getTokenIdFromToken(token);

            // then
            assertThat(tokenId).isNotNull();
            assertThat(tokenId).hasSize(36); // UUID format
        }

        @Test
        @DisplayName("Refresh Token의 만료 시간이 7일 이내이다")
        void createRefreshToken_hasCorrectExpiration() {
            // given
            Long userId = 1L;

            // when
            String token = jwtTokenProvider.createRefreshToken(userId);
            Date expiration = jwtTokenProvider.getExpirationFromToken(token);

            // then
            long expirationTime = expiration.getTime() - System.currentTimeMillis();
            assertThat(expirationTime).isLessThanOrEqualTo(604800000L); // <= 7 days
            assertThat(expirationTime).isGreaterThan(604700000L); // > 6.99 days
        }

        @Test
        @DisplayName("매번 다른 JTI를 생성한다")
        void createRefreshToken_generatesUniqueJti() {
            // given
            Long userId = 1L;

            // when
            String token1 = jwtTokenProvider.createRefreshToken(userId);
            String token2 = jwtTokenProvider.createRefreshToken(userId);

            String jti1 = jwtTokenProvider.getTokenIdFromToken(token1);
            String jti2 = jwtTokenProvider.getTokenIdFromToken(token2);

            // then
            assertThat(jti1).isNotEqualTo(jti2);
        }
    }

    @Nested
    @DisplayName("validateToken 테스트")
    class ValidateTokenTest {

        @Test
        @DisplayName("유효한 토큰은 true를 반환한다")
        void validateToken_withValidToken_returnsTrue() {
            // given
            String token = jwtTokenProvider.createAccessToken(1L);

            // when
            boolean isValid = jwtTokenProvider.validateToken(token);

            // then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("만료된 토큰은 false를 반환한다")
        void validateToken_withExpiredToken_returnsFalse() {
            // given - 만료 시간을 0으로 설정
            JwtProperties shortExpiryProperties = new JwtProperties();
            shortExpiryProperties.setSecret("test-secret-key-for-jwt-testing-must-be-at-least-256-bits!!");
            shortExpiryProperties.setAccessTokenExpiration(0L); // 즉시 만료

            JwtTokenProvider shortExpiryProvider = new JwtTokenProvider(shortExpiryProperties);
            String token = shortExpiryProvider.createAccessToken(1L);

            // when
            boolean isValid = shortExpiryProvider.validateToken(token);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("잘못된 형식의 토큰은 false를 반환한다")
        void validateToken_withMalformedToken_returnsFalse() {
            // given
            String malformedToken = "not.a.valid.jwt.token";

            // when
            boolean isValid = jwtTokenProvider.validateToken(malformedToken);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("빈 토큰은 false를 반환한다")
        void validateToken_withEmptyToken_returnsFalse() {
            // when
            boolean isValid = jwtTokenProvider.validateToken("");

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("null 토큰은 false를 반환한다")
        void validateToken_withNullToken_returnsFalse() {
            // when
            boolean isValid = jwtTokenProvider.validateToken(null);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("다른 비밀 키로 서명된 토큰은 false를 반환한다")
        void validateToken_withDifferentSignature_returnsFalse() {
            // given - 다른 비밀 키 사용
            JwtProperties otherProperties = new JwtProperties();
            otherProperties.setSecret("another-secret-key-for-different-signature-testing!!");
            otherProperties.setAccessTokenExpiration(3600000L);

            JwtTokenProvider otherProvider = new JwtTokenProvider(otherProperties);
            String token = otherProvider.createAccessToken(1L);

            // when - 원래 provider로 검증
            boolean isValid = jwtTokenProvider.validateToken(token);

            // then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("getUserIdFromToken 테스트")
    class GetUserIdFromTokenTest {

        @Test
        @DisplayName("토큰에서 userId를 정확하게 추출한다")
        void getUserIdFromToken_extractsCorrectUserId() {
            // given
            Long expectedUserId = 42L;
            String token = jwtTokenProvider.createAccessToken(expectedUserId);

            // when
            Long actualUserId = jwtTokenProvider.getUserIdFromToken(token);

            // then
            assertThat(actualUserId).isEqualTo(expectedUserId);
        }

        @Test
        @DisplayName("Refresh Token에서도 userId를 추출할 수 있다")
        void getUserIdFromToken_worksWithRefreshToken() {
            // given
            Long expectedUserId = 99L;
            String token = jwtTokenProvider.createRefreshToken(expectedUserId);

            // when
            Long actualUserId = jwtTokenProvider.getUserIdFromToken(token);

            // then
            assertThat(actualUserId).isEqualTo(expectedUserId);
        }
    }

    @Nested
    @DisplayName("getTokenIdFromToken 테스트")
    class GetTokenIdFromTokenTest {

        @Test
        @DisplayName("Refresh Token에서 JTI를 추출한다")
        void getTokenIdFromToken_extractsJti() {
            // given
            String token = jwtTokenProvider.createRefreshToken(1L);

            // when
            String tokenId = jwtTokenProvider.getTokenIdFromToken(token);

            // then
            assertThat(tokenId).isNotNull();
            assertThat(tokenId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }

        @Test
        @DisplayName("Access Token은 JTI가 null이다")
        void getTokenIdFromToken_accessTokenHasNullJti() {
            // given
            String token = jwtTokenProvider.createAccessToken(1L);

            // when
            String tokenId = jwtTokenProvider.getTokenIdFromToken(token);

            // then
            assertThat(tokenId).isNull();
        }
    }

    @Nested
    @DisplayName("Expiration 관련 메서드 테스트")
    class ExpirationMethodsTest {

        @Test
        @DisplayName("getAccessTokenExpiration이 설정된 값을 반환한다")
        void getAccessTokenExpiration_returnsConfiguredValue() {
            // when
            long expiration = jwtTokenProvider.getAccessTokenExpiration();

            // then
            assertThat(expiration).isEqualTo(3600000L);
        }

        @Test
        @DisplayName("getRefreshTokenExpiration이 설정된 값을 반환한다")
        void getRefreshTokenExpiration_returnsConfiguredValue() {
            // when
            long expiration = jwtTokenProvider.getRefreshTokenExpiration();

            // then
            assertThat(expiration).isEqualTo(604800000L);
        }
    }
}
