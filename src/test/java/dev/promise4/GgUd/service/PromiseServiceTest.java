package dev.promise4.GgUd.service;

import dev.promise4.GgUd.controller.dto.*;
import dev.promise4.GgUd.entity.*;
import dev.promise4.GgUd.exception.*;
import dev.promise4.GgUd.repository.ParticipantRepository;
import dev.promise4.GgUd.repository.PromiseRepository;
import dev.promise4.GgUd.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromiseService 테스트")
class PromiseServiceTest {

    @Mock
    private PromiseRepository promiseRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PromiseService promiseService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .kakaoId("12345")
                .nickname("테스트유저")
                .role(UserRole.USER)
                .build();
        setUserId(testUser, 1L);
    }

    @Nested
    @DisplayName("createPromise 테스트")
    class CreatePromiseTest {

        @Test
        @DisplayName("약속을 생성하고 호스트를 참여자로 등록한다")
        void createPromise_success() {
            // given
            CreatePromiseRequest request = new CreatePromiseRequest(
                    "강남역 모임",
                    "친구들과 저녁",
                    LocalDateTime.now().plusDays(7));

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(promiseRepository.save(any(Promise.class))).thenAnswer(i -> {
                Promise p = i.getArgument(0);
                setPromiseId(p, 1L);
                return p;
            });
            when(participantRepository.save(any(Participant.class))).thenAnswer(i -> i.getArgument(0));

            // when
            PromiseResponse response = promiseService.createPromise(1L, request);

            // then
            assertThat(response.getTitle()).isEqualTo("강남역 모임");
            assertThat(response.getHostId()).isEqualTo(1L);
            verify(participantRepository).save(any(Participant.class));
        }
    }

    @Nested
    @DisplayName("joinPromise 테스트")
    class JoinPromiseTest {

        private Promise testPromise;

        @BeforeEach
        void setUp() {
            User host = User.builder()
                    .kakaoId("99999")
                    .nickname("호스트")
                    .role(UserRole.USER)
                    .build();
            setUserId(host, 99L);

            testPromise = Promise.builder()
                    .title("테스트 약속")
                    .promiseDateTime(LocalDateTime.now().plusDays(1))
                    .host(host)
                    .build();
            setPromiseId(testPromise, 1L);
        }

        @Test
        @DisplayName("유효한 초대 코드로 약속에 참여한다")
        void joinPromise_success() {
            // given - uses findByInviteCodeWithLock for pessimistic locking
            when(promiseRepository.findByInviteCodeWithLock(testPromise.getInviteCode()))
                    .thenReturn(Optional.of(testPromise));
            when(participantRepository.existsByPromiseIdAndUserId(1L, 1L)).thenReturn(false);
            when(participantRepository.countByPromiseId(1L)).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(participantRepository.save(any(Participant.class))).thenAnswer(i -> i.getArgument(0));

            // when
            PromiseResponse response = promiseService.joinPromise(1L, testPromise.getInviteCode());

            // then
            assertThat(response).isNotNull();
            verify(participantRepository).save(any(Participant.class));
        }

        @Test
        @DisplayName("유효하지 않은 초대 코드면 예외 발생")
        void joinPromise_invalidCode_throwsException() {
            // given - uses findByInviteCodeWithLock for pessimistic locking
            when(promiseRepository.findByInviteCodeWithLock("invalid-code")).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> promiseService.joinPromise(1L, "invalid-code"))
                    .isInstanceOf(InvalidInviteCodeException.class);
        }

        @Test
        @DisplayName("이미 참여한 약속이면 예외 발생")
        void joinPromise_alreadyJoined_throwsException() {
            // given - uses findByInviteCodeWithLock for pessimistic locking
            when(promiseRepository.findByInviteCodeWithLock(testPromise.getInviteCode()))
                    .thenReturn(Optional.of(testPromise));
            when(participantRepository.existsByPromiseIdAndUserId(1L, 1L)).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> promiseService.joinPromise(1L, testPromise.getInviteCode()))
                    .isInstanceOf(AlreadyJoinedException.class);
        }

        @Test
        @DisplayName("최대 참여자 수 초과하면 예외 발생")
        void joinPromise_maxExceeded_throwsException() {
            // given - uses findByInviteCodeWithLock for pessimistic locking
            when(promiseRepository.findByInviteCodeWithLock(testPromise.getInviteCode()))
                    .thenReturn(Optional.of(testPromise));
            when(participantRepository.existsByPromiseIdAndUserId(1L, 1L)).thenReturn(false);
            when(participantRepository.countByPromiseId(1L)).thenReturn(10L); // max

            // when & then
            assertThatThrownBy(() -> promiseService.joinPromise(1L, testPromise.getInviteCode()))
                    .isInstanceOf(MaxParticipantsExceededException.class);
        }
    }

    @Nested
    @DisplayName("submitDepartureLocation 테스트")
    class SubmitDepartureLocationTest {

        @Test
        @DisplayName("출발지를 성공적으로 입력한다")
        void submitDepartureLocation_success() {
            // given
            User host = User.builder().kakaoId("99999").nickname("호스트").role(UserRole.USER).build();
            setUserId(host, 99L);
            Promise promise = Promise.builder()
                    .title("테스트")
                    .promiseDateTime(LocalDateTime.now().plusDays(1))
                    .host(host)
                    .build();
            setPromiseId(promise, 1L);

            Participant participant = Participant.builder()
                    .promise(promise)
                    .user(testUser)
                    .isHost(false)
                    .build();

            UpdateDepartureRequest request = new UpdateDepartureRequest(37.5665, 126.9780, "서울시청");

            when(participantRepository.findByPromiseIdAndUserId(1L, 1L))
                    .thenReturn(Optional.of(participant));
            when(promiseRepository.findById(1L)).thenReturn(Optional.of(promise));

            // when
            ParticipantResponse response = promiseService.submitDepartureLocation(1L, 1L, request);

            // then
            assertThat(response.getDepartureLatitude()).isEqualTo(37.5665);
            assertThat(response.getDepartureLongitude()).isEqualTo(126.9780);
            assertThat(response.isLocationSubmitted()).isTrue();
        }
    }

    // Helper methods
    private void setUserId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception ignored) {
        }
    }

    private void setPromiseId(Promise promise, Long id) {
        try {
            var field = Promise.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(promise, id);
        } catch (Exception ignored) {
        }
    }
}
