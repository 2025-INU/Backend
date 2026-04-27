package dev.promise4.GgUd.client;

import dev.promise4.GgUd.controller.dto.PlaceRecommendationResponse;
import dev.promise4.GgUd.controller.dto.PlaceRecommendationTab;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.List;

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
            int limit,
            PlaceRecommendationTab tab,
            String contextSummary,
            String timeSlot,
            List<String> preferredCategories,
            List<String> preferredRegions,
            Integer participantCount) {

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
        if (tab != null) {
            body.put("tab", tab.name());
        }
        if (contextSummary != null && !contextSummary.isBlank()) {
            body.put("context_summary", contextSummary);
        }
        if (timeSlot != null && !timeSlot.isBlank()) {
            body.put("time_slot", timeSlot);
        }
        if (preferredCategories != null && !preferredCategories.isEmpty()) {
            body.put("preferred_categories", preferredCategories);
        }
        if (preferredRegions != null && !preferredRegions.isEmpty()) {
            body.put("preferred_regions", preferredRegions);
        }
        if (participantCount != null) {
            body.put("participant_count", participantCount);
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
