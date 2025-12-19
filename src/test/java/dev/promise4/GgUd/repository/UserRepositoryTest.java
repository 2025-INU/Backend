package dev.promise4.GgUd.repository;

import dev.promise4.GgUd.config.TestJpaConfig;
import dev.promise4.GgUd.entity.User;
import dev.promise4.GgUd.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestJpaConfig.class)
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
@DisplayName("UserRepository 테스트")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        entityManager.clear();

        testUser = User.builder()
                .kakaoId("123456789")
                .nickname("테스트유저")
                .email("test@kakao.com")
                .profileImageUrl("https://k.kakaocdn.net/profile.jpg")
                .role(UserRole.USER)
                .build();
    }

    @Nested
    @DisplayName("findByKakaoId 테스트")
    class FindByKakaoIdTest {

        @Test
        @DisplayName("존재하는 kakaoId로 사용자를 찾을 수 있다")
        void findByKakaoId_existingId_returnsUser() {
            // given
            User savedUser = entityManager.persistAndFlush(testUser);
            entityManager.clear();

            // when
            Optional<User> foundUser = userRepository.findByKakaoId("123456789");

            // then
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getId()).isEqualTo(savedUser.getId());
            assertThat(foundUser.get().getKakaoId()).isEqualTo("123456789");
            assertThat(foundUser.get().getNickname()).isEqualTo("테스트유저");
        }

        @Test
        @DisplayName("존재하지 않는 kakaoId로 조회하면 빈 Optional을 반환한다")
        void findByKakaoId_nonExistingId_returnsEmpty() {
            // given
            entityManager.persistAndFlush(testUser);
            entityManager.clear();

            // when
            Optional<User> foundUser = userRepository.findByKakaoId("non_existing_id");

            // then
            assertThat(foundUser).isEmpty();
        }
    }

    @Nested
    @DisplayName("기본 CRUD 테스트")
    class CrudTest {

        @Test
        @DisplayName("User를 저장할 수 있다")
        void save_user_success() {
            // when
            User savedUser = userRepository.save(testUser);

            // then
            assertThat(savedUser.getId()).isNotNull();
            assertThat(savedUser.getKakaoId()).isEqualTo("123456789");
        }

        @Test
        @DisplayName("User를 ID로 조회할 수 있다")
        void findById_existingId_success() {
            // given
            User savedUser = userRepository.save(testUser);
            entityManager.clear();

            // when
            Optional<User> foundUser = userRepository.findById(savedUser.getId());

            // then
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getKakaoId()).isEqualTo("123456789");
        }

        @Test
        @DisplayName("User 정보를 업데이트할 수 있다")
        void update_user_success() {
            // given
            User savedUser = userRepository.save(testUser);
            entityManager.clear();

            // when
            User foundUser = userRepository.findById(savedUser.getId()).orElseThrow();
            foundUser.updateProfile("새닉네임", "new@kakao.com", "https://new-image.jpg");
            userRepository.flush();
            entityManager.clear();

            // then
            User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
            assertThat(updatedUser.getNickname()).isEqualTo("새닉네임");
            assertThat(updatedUser.getEmail()).isEqualTo("new@kakao.com");
            assertThat(updatedUser.getProfileImageUrl()).isEqualTo("https://new-image.jpg");
        }

        @Test
        @DisplayName("User를 삭제할 수 있다")
        void delete_user_success() {
            // given
            User savedUser = userRepository.save(testUser);
            Long userId = savedUser.getId();
            entityManager.clear();

            // when
            userRepository.deleteById(userId);
            entityManager.flush();

            // then
            Optional<User> foundUser = userRepository.findById(userId);
            assertThat(foundUser).isEmpty();
        }
    }
}
