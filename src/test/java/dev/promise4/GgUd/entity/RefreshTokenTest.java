package dev.promise4.GgUd.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RefreshToken 엔티티 테스트")
class RefreshTokenTest {

    @Nested
    @DisplayName("생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("RefreshToken을 생성할 수 있다")
        void createRefreshToken() {
            // given
            LocalDateTime expiryDate = LocalDateTime.now().plusDays(7);

            // when
            RefreshToken refreshToken = RefreshToken.builder()
                    .userId(1L)
                    .tokenId("test-token-id")
                    .expiryDate(expiryDate)
                    .build();

            // then
            assertThat(refreshToken.getUserId()).isEqualTo(1L);
            assertThat(refreshToken.getTokenId()).isEqualTo("test-token-id");
            assertThat(refreshToken.getExpiryDate()).isEqualTo(expiryDate);
            assertThat(refreshToken.isRevoked()).isFalse();
        }
    }

    @Nested
    @DisplayName("revoke 테스트")
    class RevokeTest {

        @Test
        @DisplayName("토큰을 무효화할 수 있다")
        void revoke_setsRevokedToTrue() {
            // given
            RefreshToken refreshToken = RefreshToken.builder()
                    .userId(1L)
                    .tokenId("test-token-id")
                    .expiryDate(LocalDateTime.now().plusDays(7))
                    .build();

            assertThat(refreshToken.isRevoked()).isFalse();

            // when
            refreshToken.revoke();

            // then
            assertThat(refreshToken.isRevoked()).isTrue();
        }
    }

    @Nested
    @DisplayName("isExpired 테스트")
    class IsExpiredTest {

        @Test
        @DisplayName("만료되지 않은 토큰은 false를 반환한다")
        void isExpired_withFutureDate_returnsFalse() {
            // given
            RefreshToken refreshToken = RefreshToken.builder()
                    .userId(1L)
                    .tokenId("test-token-id")
                    .expiryDate(LocalDateTime.now().plusDays(7))
                    .build();

            // when
            boolean isExpired = refreshToken.isExpired();

            // then
            assertThat(isExpired).isFalse();
        }

        @Test
        @DisplayName("만료된 토큰은 true를 반환한다")
        void isExpired_withPastDate_returnsTrue() {
            // given
            RefreshToken refreshToken = RefreshToken.builder()
                    .userId(1L)
                    .tokenId("test-token-id")
                    .expiryDate(LocalDateTime.now().minusDays(1))
                    .build();

            // when
            boolean isExpired = refreshToken.isExpired();

            // then
            assertThat(isExpired).isTrue();
        }
    }

    @Nested
    @DisplayName("isValid 테스트")
    class IsValidTest {

        @Test
        @DisplayName("무효화되지 않고 만료되지 않은 토큰은 유효하다")
        void isValid_withNonRevokedAndNonExpired_returnsTrue() {
            // given
            RefreshToken refreshToken = RefreshToken.builder()
                    .userId(1L)
                    .tokenId("test-token-id")
                    .expiryDate(LocalDateTime.now().plusDays(7))
                    .build();

            // when
            boolean isValid = refreshToken.isValid();

            // then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("무효화된 토큰은 유효하지 않다")
        void isValid_withRevoked_returnsFalse() {
            // given
            RefreshToken refreshToken = RefreshToken.builder()
                    .userId(1L)
                    .tokenId("test-token-id")
                    .expiryDate(LocalDateTime.now().plusDays(7))
                    .build();
            refreshToken.revoke();

            // when
            boolean isValid = refreshToken.isValid();

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("만료된 토큰은 유효하지 않다")
        void isValid_withExpired_returnsFalse() {
            // given
            RefreshToken refreshToken = RefreshToken.builder()
                    .userId(1L)
                    .tokenId("test-token-id")
                    .expiryDate(LocalDateTime.now().minusDays(1))
                    .build();

            // when
            boolean isValid = refreshToken.isValid();

            // then
            assertThat(isValid).isFalse();
        }
    }
}
