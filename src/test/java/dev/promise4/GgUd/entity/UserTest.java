package dev.promise4.GgUd.entity;

import dev.promise4.GgUd.config.TestJpaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(TestJpaConfig.class)
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
@DisplayName("User 엔티티 테스트")
class UserTest {

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        entityManager.clear();
    }

    @Nested
    @DisplayName("User 생성 테스트")
    class CreateUserTest {

        @Test
        @DisplayName("필수 필드로 User를 생성할 수 있다")
        void createUser_withRequiredFields_success() {
            // given
            User user = User.builder()
                    .kakaoId("123456789")
                    .nickname("홍길동")
                    .role(UserRole.USER)
                    .build();

            // when
            User savedUser = entityManager.persistAndFlush(user);

            // then
            assertThat(savedUser.getId()).isNotNull();
            assertThat(savedUser.getKakaoId()).isEqualTo("123456789");
            assertThat(savedUser.getNickname()).isEqualTo("홍길동");
            assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
        }

        @Test
        @DisplayName("모든 필드로 User를 생성할 수 있다")
        void createUser_withAllFields_success() {
            // given
            User user = User.builder()
                    .kakaoId("123456789")
                    .nickname("홍길동")
                    .email("hong@kakao.com")
                    .profileImageUrl("https://k.kakaocdn.net/profile.jpg")
                    .role(UserRole.USER)
                    .build();

            // when
            User savedUser = entityManager.persistAndFlush(user);

            // then
            assertThat(savedUser.getId()).isNotNull();
            assertThat(savedUser.getKakaoId()).isEqualTo("123456789");
            assertThat(savedUser.getNickname()).isEqualTo("홍길동");
            assertThat(savedUser.getEmail()).isEqualTo("hong@kakao.com");
            assertThat(savedUser.getProfileImageUrl()).isEqualTo("https://k.kakaocdn.net/profile.jpg");
            assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
        }

        @Test
        @DisplayName("ADMIN 역할로 User를 생성할 수 있다")
        void createUser_withAdminRole_success() {
            // given
            User user = User.builder()
                    .kakaoId("admin123")
                    .nickname("관리자")
                    .role(UserRole.ADMIN)
                    .build();

            // when
            User savedUser = entityManager.persistAndFlush(user);

            // then
            assertThat(savedUser.getRole()).isEqualTo(UserRole.ADMIN);
        }
    }

    @Nested
    @DisplayName("User 제약조건 테스트")
    class ConstraintTest {

        @Test
        @DisplayName("kakaoId는 unique 해야 한다")
        void kakaoId_shouldBeUnique() {
            // given
            User user1 = User.builder()
                    .kakaoId("same_kakao_id")
                    .nickname("사용자1")
                    .role(UserRole.USER)
                    .build();
            entityManager.persistAndFlush(user1);
            entityManager.clear();

            User user2 = User.builder()
                    .kakaoId("same_kakao_id")
                    .nickname("사용자2")
                    .role(UserRole.USER)
                    .build();

            // when & then
            assertThatThrownBy(() -> entityManager.persistAndFlush(user2))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("BaseTimeEntity 상속 테스트")
    class BaseTimeEntityTest {

        @Test
        @DisplayName("User 생성 시 createdAt이 자동으로 설정된다")
        void createUser_setsCreatedAt() {
            // given
            User user = User.builder()
                    .kakaoId("123456789")
                    .nickname("홍길동")
                    .role(UserRole.USER)
                    .build();

            // when
            User savedUser = entityManager.persistAndFlush(user);

            // then
            assertThat(savedUser.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("User 생성 시 updatedAt이 자동으로 설정된다")
        void createUser_setsUpdatedAt() {
            // given
            User user = User.builder()
                    .kakaoId("123456789")
                    .nickname("홍길동")
                    .role(UserRole.USER)
                    .build();

            // when
            User savedUser = entityManager.persistAndFlush(user);

            // then
            assertThat(savedUser.getUpdatedAt()).isNotNull();
        }
    }
}
