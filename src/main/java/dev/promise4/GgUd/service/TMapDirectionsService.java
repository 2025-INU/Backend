package dev.promise4.GgUd.service;

import dev.promise4.GgUd.controller.dto.Coordinate;
import dev.promise4.GgUd.controller.dto.DirectionsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * TMap 대중교통 길찾기 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TMapDirectionsService {

    private static final String DIRECTIONS_CACHE_PREFIX = "tmap:directions:";
    private static final Duration DIRECTIONS_CACHE_TTL = Duration.ofHours(1);

    @Qualifier("tMapWebClient")
    private final WebClient tMapWebClient;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 대중교통 경로 조회 (Redis 캐시 적용)
     */
    public Mono<DirectionsResponse> getDirections(Coordinate origin, Coordinate destination) {
        String cacheKey = generateCacheKey(origin, destination);

        DirectionsResponse cached = getCachedDirections(cacheKey);
        if (cached != null) {
            log.debug("Directions cache hit: {}", cacheKey);
            return Mono.just(cached);
        }

        Map<String, Object> requestBody = Map.of(
                "startX", String.valueOf(origin.getLongitude()),
                "startY", String.valueOf(origin.getLatitude()),
                "endX", String.valueOf(destination.getLongitude()),
                "endY", String.valueOf(destination.getLatitude()),
                "count", 3,
                "lang", 0,
                "format", "json"
        );

        return tMapWebClient.post()
                .uri("/transit/routes")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::parseResponse)
                .doOnNext(response -> cacheDirections(cacheKey, response))
                .onErrorResume(e -> {
                    log.error("TMap API call failed: {}", e.getMessage());
                    return Mono.just(DirectionsResponse.builder()
                            .routeOptions(List.of())
                            .build());
                });
    }

    @SuppressWarnings("unchecked")
    private DirectionsResponse parseResponse(Map<String, Object> response) {
        try {
            Map<String, Object> metaData = (Map<String, Object>) response.get("metaData");
            if (metaData == null) return emptyResponse();

            Map<String, Object> plan = (Map<String, Object>) metaData.get("plan");
            if (plan == null) return emptyResponse();

            List<Map<String, Object>> itineraries = (List<Map<String, Object>>) plan.get("itineraries");
            if (itineraries == null || itineraries.isEmpty()) return emptyResponse();

            List<DirectionsResponse.RouteOption> routeOptions = new ArrayList<>();

            for (Map<String, Object> itinerary : itineraries) {
                int totalDuration = ((Number) itinerary.getOrDefault("totalTime", 0)).intValue() / 60;
                int totalDistance = ((Number) itinerary.getOrDefault("totalDistance", 0)).intValue();
                int transferCount = ((Number) itinerary.getOrDefault("transferCount", 0)).intValue();
                int totalFare = parseFare(itinerary);

                List<Map<String, Object>> legs = (List<Map<String, Object>>) itinerary.get("legs");
                List<DirectionsResponse.RouteStep> steps = new ArrayList<>();

                if (legs != null) {
                    for (Map<String, Object> leg : legs) {
                        String mode = (String) leg.getOrDefault("mode", "WALK");
                        int duration = ((Number) leg.getOrDefault("sectionTime", 0)).intValue() / 60;
                        int distance = ((Number) leg.getOrDefault("distance", 0)).intValue();

                        DirectionsResponse.TransportType type = switch (mode) {
                            case "BUS" -> DirectionsResponse.TransportType.BUS;
                            case "SUBWAY" -> DirectionsResponse.TransportType.SUBWAY;
                            case "TRANSFER" -> DirectionsResponse.TransportType.TRANSFER;
                            default -> DirectionsResponse.TransportType.WALK;
                        };

                        // TMap API에서 route 필드는 "지선:1128" 형태의 String
                        String lineName = null;
                        if (type == DirectionsResponse.TransportType.BUS || type == DirectionsResponse.TransportType.SUBWAY) {
                            Object routeObj = leg.get("route");
                            if (routeObj instanceof String) {
                                lineName = (String) routeObj;
                            }
                        }

                        String linestring = extractLinestring(mode, leg);
                        String instruction = buildInstruction(mode, leg);

                        steps.add(DirectionsResponse.RouteStep.builder()
                                .type(type)
                                .instruction(instruction)
                                .duration(duration)
                                .distance(distance)
                                .lineName(lineName)
                                .linestring(linestring)
                                .build());
                    }
                }

                routeOptions.add(DirectionsResponse.RouteOption.builder()
                        .totalDuration(totalDuration)
                        .totalDistance(totalDistance)
                        .totalFare(totalFare)
                        .transferCount(transferCount)
                        .routes(steps)
                        .build());
            }

            routeOptions.sort(Comparator.comparingInt(DirectionsResponse.RouteOption::getTotalDuration));

            return DirectionsResponse.builder()
                    .routeOptions(routeOptions)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse TMap response: {}", e.getMessage());
            return emptyResponse();
        }
    }

    @SuppressWarnings("unchecked")
    private String extractLinestring(String mode, Map<String, Object> leg) {
        try {
            if ("BUS".equals(mode) || "SUBWAY".equals(mode)) {
                // 대중교통: passShape.linestring
                Map<String, Object> passShape = (Map<String, Object>) leg.get("passShape");
                if (passShape != null) {
                    return (String) passShape.get("linestring");
                }
            } else if ("WALK".equals(mode)) {
                // 도보: steps[].linestring 을 이어붙임
                List<Map<String, Object>> walkSteps = (List<Map<String, Object>>) leg.get("steps");
                if (walkSteps != null && !walkSteps.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (Map<String, Object> step : walkSteps) {
                        String ls = (String) step.get("linestring");
                        if (ls != null && !ls.isBlank()) {
                            if (!sb.isEmpty()) sb.append(" ");
                            sb.append(ls.trim());
                        }
                    }
                    return sb.isEmpty() ? null : sb.toString();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract linestring: {}", e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String buildInstruction(String mode, Map<String, Object> leg) {
        return switch (mode) {
            case "BUS" -> {
                Map<String, Object> start = (Map<String, Object>) leg.get("start");
                Map<String, Object> end = (Map<String, Object>) leg.get("end");
                String startName = start != null ? (String) start.getOrDefault("name", "") : "";
                String endName = end != null ? (String) end.getOrDefault("name", "") : "";
                yield startName + " 승차 → " + endName + " 하차";
            }
            case "SUBWAY" -> {
                Map<String, Object> start = (Map<String, Object>) leg.get("start");
                Map<String, Object> end = (Map<String, Object>) leg.get("end");
                String startName = start != null ? (String) start.getOrDefault("name", "") : "";
                String endName = end != null ? (String) end.getOrDefault("name", "") : "";
                yield startName + " 승차 → " + endName + " 하차";
            }
            case "WALK" -> "도보 이동";
            default -> "이동";
        };
    }

    @SuppressWarnings("unchecked")
    private int parseFare(Map<String, Object> itinerary) {
        try {
            Map<String, Object> fare = (Map<String, Object>) itinerary.get("fare");
            if (fare == null) return 0;
            Map<String, Object> regular = (Map<String, Object>) fare.get("regular");
            if (regular == null) return 0;
            return ((Number) regular.getOrDefault("totalFare", 0)).intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private DirectionsResponse emptyResponse() {
        return DirectionsResponse.builder().routeOptions(List.of()).build();
    }

    private String generateCacheKey(Coordinate origin, Coordinate destination) {
        return String.format("%s%.4f:%.4f:%.4f:%.4f",
                DIRECTIONS_CACHE_PREFIX,
                origin.getLatitude(), origin.getLongitude(),
                destination.getLatitude(), destination.getLongitude());
    }

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

    private void cacheDirections(String cacheKey, DirectionsResponse response) {
        try {
            if (response.getRouteOptions() != null && !response.getRouteOptions().isEmpty()) {
                redisTemplate.opsForValue().set(cacheKey, response, DIRECTIONS_CACHE_TTL);
            }
        } catch (Exception e) {
            log.warn("Failed to cache directions: {}", e.getMessage());
        }
    }
}
