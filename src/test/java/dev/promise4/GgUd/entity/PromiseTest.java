package dev.promise4.GgUd.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Promise 엔티티 테스트")
class PromiseTest {

    private User host;

    @BeforeEach
    void setUp() {
        host = User.builder()
                .kakaoId("12345")
                .nickname("호스트")
                .role(UserRole.USER)
                .build();
    }

    @Nested
    @DisplayName("생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("약속을 생성할 수 있다")
        void createPromise() {
            // given
            LocalDateTime promiseDateTime = LocalDateTime.now().plusDays(7);

            // when
            Promise promise = Promise.builder()
                    .title("강남역 모임")
                    .description("친구들과 저녁 식사")
                    .promiseDateTime(promiseDateTime)
                    .host(host)
                    .build();

            // then
            assertThat(promise.getTitle()).isEqualTo("강남역 모임");
            assertThat(promise.getDescription()).isEqualTo("친구들과 저녁 식사");
            assertThat(promise.getPromiseDateTime()).isEqualTo(promiseDateTime);
            assertThat(promise.getHost()).isEqualTo(host);
            assertThat(promise.getStatus()).isEqualTo(PromiseStatus.CREATED);
            assertThat(promise.getMaxParticipants()).isEqualTo(10);
        }

        @Test
        @DisplayName("약속 생성 시 초대 코드가 자동 생성된다")
        void createPromise_withInviteCode() {
            // when
            Promise promise = Promise.builder()
                    .title("테스트 약속")
                    .promiseDateTime(LocalDateTime.now().plusDays(1))
                    .host(host)
                    .build();

            // then
            assertThat(promise.getInviteCode()).isNotNull();
            assertThat(promise.getInviteCode()).hasSize(36); // UUID format
        }

        @Test
        @DisplayName("약속 생성 시 초대 만료 시간이 24시간 후로 설정된다")
        void createPromise_withInviteExpiredAt() {
            // given
            LocalDateTime before = LocalDateTime.now().plusHours(23);
            LocalDateTime after = LocalDateTime.now().plusHours(25);

            // when
            Promise promise = Promise.builder()
                    .title("테스트 약속")
                    .promiseDateTime(LocalDateTime.now().plusDays(1))
                    .host(host)
                    .build();

            // then
            assertThat(promise.getInviteExpiredAt()).isAfter(before);
            assertThat(promise.getInviteExpiredAt()).isBefore(after);
        }

        @Test
        @DisplayName("description 없이 약속을 생성할 수 있다")
        void createPromise_withoutDescription() {
            // when
            Promise promise = Promise.builder()
                    .title("테스트 약속")
                    .promiseDateTime(LocalDateTime.now().plusDays(1))
                    .host(host)
                    .build();

            // then
            assertThat(promise.getDescription()).isNull();
        }
    }

    @Nested
    @DisplayName("상태 변경 테스트")
    class StatusChangeTest {

        @Test
        @DisplayName("CREATED에서 RECRUITING으로 변경할 수 있다")
        void changeStatus_createdToRecruiting() {
            // given
            Promise promise = createPromise();

            // when
            promise.startRecruiting();

            // then
            assertThat(promise.getStatus()).isEqualTo(PromiseStatus.RECRUITING);
        }

        @Test
        @DisplayName("RECRUITING에서 WAITING_LOCATIONS으로 변경할 수 있다")
        void changeStatus_recruitingToWaitingLocations() {
            // given
            Promise promise = createPromise();
            promise.startRecruiting();

            // when
            promise.closeRecruiting();

            // then
            assertThat(promise.getStatus()).isEqualTo(PromiseStatus.WAITING_LOCATIONS);
        }

        @Test
        @DisplayName("WAITING_LOCATIONS에서 SELECTING_MIDPOINT로 변경할 수 있다")
        void changeStatus_waitingToSelecting() {
            // given
            Promise promise = createPromise();
            promise.startRecruiting();
            promise.closeRecruiting();

            // when
            promise.startSelectingMidpoint();

            // then
            assertThat(promise.getStatus()).isEqualTo(PromiseStatus.SELECTING_MIDPOINT);
        }

        @Test
        @DisplayName("장소를 확정할 수 있다")
        void confirmLocation() {
            // given
            Promise promise = createPromise();
            promise.startRecruiting();
            promise.closeRecruiting();
            promise.startSelectingMidpoint();

            // when
            promise.confirmLocation(37.5665, 126.9780, "서울시청");

            // then
            assertThat(promise.getStatus()).isEqualTo(PromiseStatus.CONFIRMED);
            assertThat(promise.getConfirmedLatitude()).isEqualTo(37.5665);
            assertThat(promise.getConfirmedLongitude()).isEqualTo(126.9780);
            assertThat(promise.getConfirmedPlaceName()).isEqualTo("서울시청");
        }

        @Test
        @DisplayName("약속을 취소할 수 있다")
        void cancelPromise() {
            // given
            Promise promise = createPromise();

            // when
            promise.cancel();

            // then
            assertThat(promise.getStatus()).isEqualTo(PromiseStatus.CANCELLED);
        }

        @Test
        @DisplayName("잘못된 상태에서 상태 변경 시 예외가 발생한다")
        void changeStatus_invalidTransition_throwsException() {
            // given
            Promise promise = createPromise();

            // when & then - CREATED에서 바로 CONFIRMED로 변경 시도
            assertThatThrownBy(() -> promise.confirmLocation(37.5665, 126.9780, "서울시청"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("초대 코드 검증 테스트")
    class InviteCodeTest {

        @Test
        @DisplayName("초대 코드가 만료되지 않았으면 true를 반환한다")
        void isInviteValid_notExpired_returnsTrue() {
            // given
            Promise promise = createPromise();

            // when
            boolean isValid = promise.isInviteValid();

            // then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("초대 코드가 만료되었으면 false를 반환한다")
        void isInviteValid_expired_returnsFalse() {
            // given
            Promise promise = Promise.builder()
                    .title("테스트 약속")
                    .promiseDateTime(LocalDateTime.now().plusDays(1))
                    .host(host)
                    .inviteExpiredAt(LocalDateTime.now().minusHours(1))
                    .build();

            // when
            boolean isValid = promise.isInviteValid();

            // then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("참여자 수 검증 테스트")
    class ParticipantsLimitTest {

        @Test
        @DisplayName("기본 최대 참여자 수는 10명이다")
        void defaultMaxParticipants() {
            // when
            Promise promise = createPromise();

            // then
            assertThat(promise.getMaxParticipants()).isEqualTo(10);
        }

        @Test
        @DisplayName("최대 참여자 수를 커스텀 설정할 수 있다")
        void customMaxParticipants() {
            // when
            Promise promise = Promise.builder()
                    .title("소규모 모임")
                    .promiseDateTime(LocalDateTime.now().plusDays(1))
                    .host(host)
                    .maxParticipants(5)
                    .build();

            // then
            assertThat(promise.getMaxParticipants()).isEqualTo(5);
        }
    }

    private Promise createPromise() {
        return Promise.builder()
                .title("테스트 약속")
                .promiseDateTime(LocalDateTime.now().plusDays(1))
                .host(host)
                .build();
    }
}
