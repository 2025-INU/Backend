package dev.promise4.GgUd.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Participant 엔티티 테스트")
class ParticipantTest {

    private User user;
    private Promise promise;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .kakaoId("12345")
                .nickname("테스트유저")
                .role(UserRole.USER)
                .build();

        User host = User.builder()
                .kakaoId("99999")
                .nickname("호스트")
                .role(UserRole.USER)
                .build();

        promise = Promise.builder()
                .title("테스트 약속")
                .promiseDateTime(LocalDateTime.now().plusDays(1))
                .host(host)
                .build();
    }

    @Nested
    @DisplayName("생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("참여자를 생성할 수 있다")
        void createParticipant() {
            // when
            Participant participant = Participant.builder()
                    .promise(promise)
                    .user(user)
                    .isHost(false)
                    .build();

            // then
            assertThat(participant.getPromise()).isEqualTo(promise);
            assertThat(participant.getUser()).isEqualTo(user);
            assertThat(participant.isHost()).isFalse();
            assertThat(participant.isLocationSubmitted()).isFalse();
        }

        @Test
        @DisplayName("호스트로 참여자를 생성할 수 있다")
        void createParticipant_asHost() {
            // when
            Participant participant = Participant.builder()
                    .promise(promise)
                    .user(user)
                    .isHost(true)
                    .build();

            // then
            assertThat(participant.isHost()).isTrue();
        }

        @Test
        @DisplayName("참여 시간이 자동으로 설정된다")
        void createParticipant_withJoinedAt() {
            // given
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);

            // when
            Participant participant = Participant.builder()
                    .promise(promise)
                    .user(user)
                    .isHost(false)
                    .build();

            // then
            assertThat(participant.getJoinedAt()).isAfter(before);
            assertThat(participant.getJoinedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
        }
    }

    @Nested
    @DisplayName("출발지 입력 테스트")
    class DepartureLocationTest {

        @Test
        @DisplayName("출발지를 입력할 수 있다")
        void submitDepartureLocation() {
            // given
            Participant participant = Participant.builder()
                    .promise(promise)
                    .user(user)
                    .isHost(false)
                    .build();

            // when
            participant.submitDepartureLocation(37.5665, 126.9780, "서울시청");

            // then
            assertThat(participant.getDepartureLatitude()).isEqualTo(37.5665);
            assertThat(participant.getDepartureLongitude()).isEqualTo(126.9780);
            assertThat(participant.getDepartureAddress()).isEqualTo("서울시청");
            assertThat(participant.isLocationSubmitted()).isTrue();
        }

        @Test
        @DisplayName("출발지를 수정할 수 있다")
        void updateDepartureLocation() {
            // given
            Participant participant = Participant.builder()
                    .promise(promise)
                    .user(user)
                    .isHost(false)
                    .build();
            participant.submitDepartureLocation(37.5665, 126.9780, "서울시청");

            // when
            participant.submitDepartureLocation(37.5512, 126.9882, "명동역");

            // then
            assertThat(participant.getDepartureLatitude()).isEqualTo(37.5512);
            assertThat(participant.getDepartureLongitude()).isEqualTo(126.9882);
            assertThat(participant.getDepartureAddress()).isEqualTo("명동역");
            assertThat(participant.isLocationSubmitted()).isTrue();
        }

        @Test
        @DisplayName("출발지 입력 전 isLocationSubmitted는 false이다")
        void isLocationSubmitted_beforeSubmit() {
            // given
            Participant participant = Participant.builder()
                    .promise(promise)
                    .user(user)
                    .isHost(false)
                    .build();

            // then
            assertThat(participant.isLocationSubmitted()).isFalse();
        }
    }
}
