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

@Slf4j
@Component
@RequiredArgsConstructor
public class AiPlaceRecommendationClient {

    @Qualifier("aiServerWebClient")
    private final WebClient aiServerWebClient;

    public Mono<PlaceRecommendationResponse> recommendPlaces(
            String query,
            Long promiseId,
            Double latitude,
            Double longitude,
            int limit,
            PlaceRecommendationTab tab) {

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
                    return Mono.just(new PlaceRecommendationResponse(promiseId, java.util.List.of(), false, null));
                });
    }
}
