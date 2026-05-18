package dev.promise4.GgUd.service;

import dev.promise4.GgUd.client.AiPlaceRecommendationClient;
import dev.promise4.GgUd.controller.dto.PlaceRecommendationItem;
import dev.promise4.GgUd.controller.dto.PlaceRecommendationRequest;
import dev.promise4.GgUd.controller.dto.PlaceRecommendationResponse;
import dev.promise4.GgUd.entity.Promise;
import dev.promise4.GgUd.entity.PromiseStatus;
import dev.promise4.GgUd.entity.User;
import dev.promise4.GgUd.entity.UserRole;
import dev.promise4.GgUd.repository.AiPlaceRecommendationsRepository;
import dev.promise4.GgUd.repository.ParticipantRepository;
import dev.promise4.GgUd.repository.PromiseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlaceRecommendationService 테스트")
class PlaceRecommendationServiceTest {

    @Mock private PromiseRepository promiseRepository;
    @Mock private ParticipantRepository participantRepository;
    @Mock private AiPlaceRecommendationClient aiPlaceRecommendationClient;
    @Mock private AiPlaceRecommendationsRepository aiPlaceRecommendationsRepository;
    @Mock private UserHistoryService userHistoryService;

    @InjectMocks
    private PlaceRecommendationService placeRecommendationService;

    private User host;
    private Promise promise;

    @BeforeEach
    void setUp() {
        host = User.builder()
                .kakaoId("99999")
                .nickname("호스트")
                .role(UserRole.USER)
                .build();
        setUserId(host, 1L);

        promise = Promise.builder()
                .title("테스트 약속")
                .promiseDateTime(LocalDateTime.now().plusDays(1))
                .host(host)
                .build();
        setPromiseId(promise, 1L);
        promise.startRecruiting();
        promise.startSelectingMidpoint();
        promise.confirmMidpointStation(37.5665, 126.9780, "강남역");
    }

    @Nested
    @DisplayName("applyDistanceScoreFallback 테스트")
    class ApplyDistanceScoreFallbackTest {

        @BeforeEach
        void setUpMocks() {
            when(promiseRepository.findById(1L)).thenReturn(Optional.of(promise));
            when(participantRepository.existsByPromiseIdAndUserId(1L, 1L)).thenReturn(true);
            when(aiPlaceRecommendationsRepository.findByPromiseIdOrderByRankingAsc(1L)).thenReturn(List.of());
            when(userHistoryService.getRecentQueries(1L)).thenReturn(List.of());
            when(userHistoryService.getRecentPlaceIds(1L)).thenReturn(List.of());
        }

        @Test
        @DisplayName("ai_score가 0이면 거리 기반 점수로 대체된다")
        void aiScore_zero_replacedWithDistanceScore() {
            PlaceRecommendationItem item = new PlaceRecommendationItem();
            item.setPlaceName("스타벅스");
            item.setAiScore(0.0);
            item.setDistanceFromMidpoint(2.0);

            when(aiPlaceRecommendationClient.recommendPlaces(any(), any(), any(), any(), anyInt(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(Mono.just(new PlaceRecommendationResponse(1L, List.of(item), false)));
            doNothing().when(aiPlaceRecommendationsRepository).deleteByPromiseId(1L);

            PlaceRecommendationResponse response = placeRecommendationService
                    .getPlaceRecommendations(1L, 1L, new PlaceRecommendationRequest());

            // score = max(1.0, 100.0 - 2.0 * 10) = 80.0
            assertThat(response.getRecommendations().get(0).getAiScore()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("ai_score가 null이면 거리 기반 점수로 대체된다")
        void aiScore_null_replacedWithDistanceScore() {
            PlaceRecommendationItem item = new PlaceRecommendationItem();
            item.setPlaceName("카페베네");
            item.setAiScore(null);
            item.setDistanceFromMidpoint(5.0);

            when(aiPlaceRecommendationClient.recommendPlaces(any(), any(), any(), any(), anyInt(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(Mono.just(new PlaceRecommendationResponse(1L, List.of(item), false)));
            doNothing().when(aiPlaceRecommendationsRepository).deleteByPromiseId(1L);

            PlaceRecommendationResponse response = placeRecommendationService
                    .getPlaceRecommendations(1L, 1L, new PlaceRecommendationRequest());

            // score = max(1.0, 100.0 - 5.0 * 10) = 50.0
            assertThat(response.getRecommendations().get(0).getAiScore()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("ai_score가 0이고 거리가 10km 이상이면 최솟값 1.0으로 대체된다")
        void aiScore_zero_distanceFar_returnsMinScore() {
            PlaceRecommendationItem item = new PlaceRecommendationItem();
            item.setPlaceName("먼 카페");
            item.setAiScore(0.0);
            item.setDistanceFromMidpoint(15.0);

            when(aiPlaceRecommendationClient.recommendPlaces(any(), any(), any(), any(), anyInt(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(Mono.just(new PlaceRecommendationResponse(1L, List.of(item), false)));
            doNothing().when(aiPlaceRecommendationsRepository).deleteByPromiseId(1L);

            PlaceRecommendationResponse response = placeRecommendationService
                    .getPlaceRecommendations(1L, 1L, new PlaceRecommendationRequest());

            // score = max(1.0, 100.0 - 15.0 * 10) = max(1.0, -50.0) = 1.0
            assertThat(response.getRecommendations().get(0).getAiScore()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("ai_score가 0이고 거리가 0이면 100.0으로 대체된다")
        void aiScore_zero_distanceZero_returnsMaxScore() {
            PlaceRecommendationItem item = new PlaceRecommendationItem();
            item.setPlaceName("근처 카페");
            item.setAiScore(0.0);
            item.setDistanceFromMidpoint(0.0);

            when(aiPlaceRecommendationClient.recommendPlaces(any(), any(), any(), any(), anyInt(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(Mono.just(new PlaceRecommendationResponse(1L, List.of(item), false)));
            doNothing().when(aiPlaceRecommendationsRepository).deleteByPromiseId(1L);

            PlaceRecommendationResponse response = placeRecommendationService
                    .getPlaceRecommendations(1L, 1L, new PlaceRecommendationRequest());

            // score = max(1.0, 100.0 - 0.0 * 10) = 100.0
            assertThat(response.getRecommendations().get(0).getAiScore()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("ai_score가 이미 있으면 점수를 유지한다")
        void aiScore_present_notReplaced() {
            PlaceRecommendationItem item = new PlaceRecommendationItem();
            item.setPlaceName("AI 추천 카페");
            item.setAiScore(85.0);
            item.setDistanceFromMidpoint(3.0);

            when(aiPlaceRecommendationClient.recommendPlaces(any(), any(), any(), any(), anyInt(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(Mono.just(new PlaceRecommendationResponse(1L, List.of(item), false)));
            doNothing().when(aiPlaceRecommendationsRepository).deleteByPromiseId(1L);

            PlaceRecommendationResponse response = placeRecommendationService
                    .getPlaceRecommendations(1L, 1L, new PlaceRecommendationRequest());

            assertThat(response.getRecommendations().get(0).getAiScore()).isEqualTo(85.0);
        }
    }

    private void setUserId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception ignored) {}
    }

    private void setPromiseId(Promise promise, Long id) {
        try {
            var field = Promise.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(promise, id);
        } catch (Exception ignored) {}
    }
}
