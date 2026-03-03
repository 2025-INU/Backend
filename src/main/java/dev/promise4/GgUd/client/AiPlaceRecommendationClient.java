package dev.promise4.GgUd.client;

import dev.promise4.GgUd.controller.dto.PlaceRecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * AI 서버 장소 추천 API 클라이언트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiPlaceRecommendationClient {

    @Qualifier("aiServerWebClient")
    private final WebClient aiServerWebClient;

    /**
     * AI 서버에 장소 추천 요청
     *
     * @param query      자연어 요청 (예: 친구들이랑 분위기 좋은 카페)
     * @param promiseId  약속 ID (선택)
     * @param latitude   중간지점 위도 (선택)
     * @param longitude  중간지점 경도 (선택)
     * @param limit      추천 개수 (1~20, 기본 10)
     * @return 추천 장소 목록
     */
    public Mono<PlaceRecommendationResponse> recommendPlaces(
            String query,
            Long promiseId,
            Double latitude,
            Double longitude,
            int limit) {

        Map<String, Object> body = new java.util.HashMap<>(Map.of(
                "query", query,
                "limit", limit
        ));
        if (promiseId != null) {
            body.put("promise_id", promiseId);
        }
        if (latitude != null && longitude != null) {
            body.put("latitude", latitude);
            body.put("longitude", longitude);
        }

        return aiServerWebClient.post()
                .uri("/recommend-places")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(PlaceRecommendationResponse.class)
                .doOnError(WebClientResponseException.class, ex ->
                        log.error("AI 서버 장소 추천 실패: status={}, body={}",
                                ex.getStatusCode(), ex.getResponseBodyAsString()))
                .onErrorResume(e -> {
                    log.warn("AI 서버 호출 실패, 빈 결과 반환: {}", e.getMessage());
                    return Mono.just(new PlaceRecommendationResponse(promiseId, java.util.List.of()));
                });
    }
}
