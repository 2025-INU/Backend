package dev.promise4.GgUd.service;

import dev.promise4.GgUd.client.KakaoApiClient;
import dev.promise4.GgUd.controller.dto.Coordinate;
import dev.promise4.GgUd.controller.dto.DirectionsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KakaoDirectionsServiceTest {

    @Mock
    private KakaoApiClient kakaoApiClient;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private KakaoDirectionsService kakaoDirectionsService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null); // 캐시 미스
        kakaoDirectionsService = new KakaoDirectionsService(kakaoApiClient, redisTemplate);
    }

    @Test
    @DisplayName("여러 경로를 소요 시간 오름차순으로 반환한다")
    void getDirections_returnsMultipleRoutesSortedByDuration() {
        // 카카오 API 응답 mock - 경로 3개 (소요 시간 역순으로 세팅)
        Map<String, Object> mockResponse = Map.of(
                "routes", List.of(
                        buildRoute(60 * 45, 12000), // 45분
                        buildRoute(60 * 30, 8000),  // 30분
                        buildRoute(60 * 50, 15000)  // 50분
                )
        );

        when(kakaoApiClient.getDirections(anyString(), eq(Map.class)))
                .thenReturn(Mono.just(mockResponse));

        Coordinate origin = Coordinate.of(37.5665, 126.9780);
        Coordinate destination = Coordinate.of(37.4979, 127.0276);

        DirectionsResponse response = kakaoDirectionsService.getDirections(origin, destination).block();

        assertThat(response).isNotNull();
        assertThat(response.getRouteOptions()).hasSize(3);

        // 소요 시간 오름차순 정렬 확인
        assertThat(response.getRouteOptions().get(0).getTotalDuration()).isEqualTo(30);
        assertThat(response.getRouteOptions().get(1).getTotalDuration()).isEqualTo(45);
        assertThat(response.getRouteOptions().get(2).getTotalDuration()).isEqualTo(50);
    }

    @Test
    @DisplayName("카카오 API 응답이 비어있으면 빈 리스트를 반환한다")
    void getDirections_returnsEmptyList_whenNoRoutes() {
        Map<String, Object> mockResponse = Map.of("routes", List.of());

        when(kakaoApiClient.getDirections(anyString(), eq(Map.class)))
                .thenReturn(Mono.just(mockResponse));

        Coordinate origin = Coordinate.of(37.5665, 126.9780);
        Coordinate destination = Coordinate.of(37.4979, 127.0276);

        DirectionsResponse response = kakaoDirectionsService.getDirections(origin, destination).block();

        assertThat(response).isNotNull();
        assertThat(response.getRouteOptions()).isEmpty();
    }

    @Test
    @DisplayName("각 경로의 거리와 소요 시간이 올바르게 파싱된다")
    void getDirections_parsesDistanceAndDurationCorrectly() {
        Map<String, Object> mockResponse = Map.of(
                "routes", List.of(buildRoute(60 * 30, 8000))
        );

        when(kakaoApiClient.getDirections(anyString(), eq(Map.class)))
                .thenReturn(Mono.just(mockResponse));

        Coordinate origin = Coordinate.of(37.5665, 126.9780);
        Coordinate destination = Coordinate.of(37.4979, 127.0276);

        DirectionsResponse response = kakaoDirectionsService.getDirections(origin, destination).block();

        DirectionsResponse.RouteOption route = response.getRouteOptions().get(0);
        assertThat(route.getTotalDuration()).isEqualTo(30);   // 초 → 분 변환 확인
        assertThat(route.getTotalDistance()).isEqualTo(8000);
    }

    private Map<String, Object> buildRoute(int durationSeconds, int distanceMeters) {
        return Map.of(
                "summary", Map.of(
                        "duration", durationSeconds,
                        "distance", distanceMeters
                ),
                "sections", List.of(
                        Map.of("duration", durationSeconds, "distance", distanceMeters)
                )
        );
    }
}
