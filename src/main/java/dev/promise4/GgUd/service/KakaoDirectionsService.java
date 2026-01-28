package dev.promise4.GgUd.service;

import dev.promise4.GgUd.client.KakaoApiClient;
import dev.promise4.GgUd.controller.dto.Coordinate;
import dev.promise4.GgUd.controller.dto.DirectionsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 카카오 길찾기 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoDirectionsService {

    private static final String DIRECTIONS_CACHE_PREFIX = "directions:";
    private static final Duration DIRECTIONS_CACHE_TTL = Duration.ofHours(1);

    private final KakaoApiClient kakaoApiClient;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 대중교통 경로 조회 (Redis 캐시 적용)
     *
     * @param origin      출발지 좌표
     * @param destination 도착지 좌표
     */
    public Mono<DirectionsResponse> getDirections(Coordinate origin, Coordinate destination) {
        String cacheKey = generateCacheKey(origin, destination);

        // 캐시에서 먼저 조회
        DirectionsResponse cached = getCachedDirections(cacheKey);
        if (cached != null) {
            log.debug("Directions cache hit: {}", cacheKey);
            return Mono.just(cached);
        }

        String uri = String.format("/v1/directions?origin=%f,%f&destination=%f,%f&priority=RECOMMEND",
                origin.getLongitude(), origin.getLatitude(),
                destination.getLongitude(), destination.getLatitude());

        return kakaoApiClient.getDirections(uri, Map.class)
                .map(this::parseDirectionsResponse)
                .doOnNext(response -> cacheDirections(cacheKey, response))
                .onErrorResume(e -> {
                    log.error("Failed to get directions: {}", e.getMessage());
                    return Mono.just(DirectionsResponse.builder()
                            .totalDuration(0)
                            .totalDistance(0)
                            .routes(List.of())
                            .build());
                });
    }

    /**
     * 캐시 키 생성 (출발지-도착지 좌표 기반)
     */
    private String generateCacheKey(Coordinate origin, Coordinate destination) {
        return String.format("%s%.4f:%.4f:%.4f:%.4f",
                DIRECTIONS_CACHE_PREFIX,
                origin.getLatitude(), origin.getLongitude(),
                destination.getLatitude(), destination.getLongitude());
    }

    /**
     * 캐시에서 경로 정보 조회
     */
    private DirectionsResponse getCachedDirections(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof DirectionsResponse) {
                return (DirectionsResponse) cached;
            }
        } catch (Exception e) {
            log.warn("Failed to get cached directions: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 경로 정보 캐시에 저장
     */
    private void cacheDirections(String cacheKey, DirectionsResponse response) {
        try {
            if (response.getTotalDuration() > 0) {  // 유효한 응답만 캐싱
                redisTemplate.opsForValue().set(cacheKey, response, DIRECTIONS_CACHE_TTL);
                log.debug("Directions cached: {}", cacheKey);
            }
        } catch (Exception e) {
            log.warn("Failed to cache directions: {}", e.getMessage());
        }
    }

    /**
     * 카카오 API 응답 파싱
     */
    @SuppressWarnings("unchecked")
    private DirectionsResponse parseDirectionsResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");

            if (routes == null || routes.isEmpty()) {
                return DirectionsResponse.builder()
                        .totalDuration(0)
                        .totalDistance(0)
                        .routes(List.of())
                        .build();
            }

            Map<String, Object> route = routes.get(0);
            Map<String, Object> summary = (Map<String, Object>) route.get("summary");

            int totalDuration = ((Number) summary.getOrDefault("duration", 0)).intValue() / 60; // 초 -> 분
            int totalDistance = ((Number) summary.getOrDefault("distance", 0)).intValue();

            List<Map<String, Object>> sections = (List<Map<String, Object>>) route.get("sections");
            List<DirectionsResponse.RouteStep> routeSteps = new ArrayList<>();

            if (sections != null) {
                for (Map<String, Object> section : sections) {
                    int duration = ((Number) section.getOrDefault("duration", 0)).intValue() / 60;
                    int distance = ((Number) section.getOrDefault("distance", 0)).intValue();

                    routeSteps.add(DirectionsResponse.RouteStep.builder()
                            .type(DirectionsResponse.TransportType.WALK)
                            .instruction("이동")
                            .duration(duration)
                            .distance(distance)
                            .build());
                }
            }

            return DirectionsResponse.builder()
                    .totalDuration(totalDuration)
                    .totalDistance(totalDistance)
                    .routes(routeSteps)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse directions response: {}", e.getMessage());
            return DirectionsResponse.builder()
                    .totalDuration(0)
                    .totalDistance(0)
                    .routes(List.of())
                    .build();
        }
    }
}
