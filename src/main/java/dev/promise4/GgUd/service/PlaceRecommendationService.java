package dev.promise4.GgUd.service;

import dev.promise4.GgUd.client.AiPlaceRecommendationClient;
import dev.promise4.GgUd.controller.dto.PlaceRecommendationRequest;
import dev.promise4.GgUd.controller.dto.PlaceRecommendationResponse;
import dev.promise4.GgUd.controller.dto.PlaceRecommendationItem;
import dev.promise4.GgUd.entity.AiPlaceRecommendationsEntity;
import dev.promise4.GgUd.entity.Promise;
import dev.promise4.GgUd.repository.ParticipantRepository;
import dev.promise4.GgUd.repository.PromiseRepository;
import dev.promise4.GgUd.repository.AiPlaceRecommendationsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * AI 기반 장소 추천 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceRecommendationService {

    private final PromiseRepository promiseRepository;
    private final ParticipantRepository participantRepository;
    private final AiPlaceRecommendationClient aiPlaceRecommendationClient;
    private final AiPlaceRecommendationsRepository aiPlaceRecommendationsRepository;

    /**
     * 사용자 맞춤 장소 추천 (AI 서버 호출)
     *
     * @param promiseId  약속 ID
     * @param userId     요청 사용자 ID (참여자 여부 검증용)
     * @param request    query, limit
     * @return AI 추천 장소 목록
     */
    @Transactional
    public PlaceRecommendationResponse getPlaceRecommendations(
            Long promiseId,
            Long userId,
            PlaceRecommendationRequest request) {

        Promise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new IllegalArgumentException("약속을 찾을 수 없습니다"));

        if (!participantRepository.existsByPromiseIdAndUserId(promiseId, userId)) {
            throw new IllegalStateException("해당 약속의 참여자만 장소 추천을 요청할 수 있습니다");
        }

        // 1. forceRefresh가 아니면 캐싱된 추천이 있으면 그대로 반환
        if (!Boolean.TRUE.equals(request.getForceRefresh())) {
            var cached = aiPlaceRecommendationsRepository.findByPromiseIdOrderByRankingAsc(promiseId);
            if (!cached.isEmpty()) {
                log.debug("Found {} cached AI place recommendations for promiseId={}", cached.size(), promiseId);
                var items = cached.stream()
                        .map(this::toDto)
                        .toList();
                return new PlaceRecommendationResponse(promiseId, items);
            }
        } else {
            log.debug("forceRefresh=true, skipping cache for promiseId={}", promiseId);
        }

        int limit = request.getLimit() != null ? request.getLimit() : 10;

        // 중간지점이 확정된 경우 좌표 전달 (근처 장소 추천에 활용)
        Double latitude = promise.getConfirmedLatitude();
        Double longitude = promise.getConfirmedLongitude();

        Mono<PlaceRecommendationResponse> mono = aiPlaceRecommendationClient.recommendPlaces(
                request.getQuery(),
                promiseId,
                latitude,
                longitude,
                limit
        );

        PlaceRecommendationResponse response = mono.block();
        if (response == null) {
            log.warn("AI 서버로부터 null 응답 수신, 빈 추천 결과 반환");
            return new PlaceRecommendationResponse(promiseId, java.util.List.of());
        }

        response.setPromiseId(promiseId);

        // 2. 새로 받은 추천 결과를 캐싱
        aiPlaceRecommendationsRepository.deleteByPromiseId(promiseId);
        if (response.getRecommendations() != null && !response.getRecommendations().isEmpty()) {
            var entities = java.util.stream.IntStream.range(0, response.getRecommendations().size())
                    .mapToObj(i -> {
                        PlaceRecommendationItem item = response.getRecommendations().get(i);
                        return toEntity(promiseId, i + 1, item);
                    })
                    .toList();
            aiPlaceRecommendationsRepository.saveAll(entities);
            log.debug("Saved {} AI place recommendations for promiseId={}", entities.size(), promiseId);
        }

        return response;
    }

    private PlaceRecommendationItem toDto(AiPlaceRecommendationsEntity entity) {
        PlaceRecommendationItem item = new PlaceRecommendationItem();
        item.setPlaceId(entity.getPlaceId());
        item.setPlaceName(entity.getPlaceName());
        item.setCategory(entity.getCategory());
        item.setAddress(entity.getAddress());
        item.setLatitude(entity.getLatitude() != null ? entity.getLatitude().doubleValue() : null);
        item.setLongitude(entity.getLongitude() != null ? entity.getLongitude().doubleValue() : null);
        item.setAiScore(entity.getAiScore() != null ? entity.getAiScore().doubleValue() : null);
        item.setDistanceFromMidpoint(
                entity.getDistanceFromMidpoint() != null ? entity.getDistanceFromMidpoint().doubleValue() : null
        );
        return item;
    }

    private AiPlaceRecommendationsEntity toEntity(Long promiseId, int ranking, PlaceRecommendationItem item) {
        return AiPlaceRecommendationsEntity.builder()
                .promiseId(promiseId)
                .placeId(item.getPlaceId())
                .placeName(item.getPlaceName())
                .category(item.getCategory())
                .address(item.getAddress())
                .latitude(item.getLatitude() != null ? java.math.BigDecimal.valueOf(item.getLatitude()) : null)
                .longitude(item.getLongitude() != null ? java.math.BigDecimal.valueOf(item.getLongitude()) : null)
                .ranking(ranking)
                .aiScore(item.getAiScore() != null ? java.math.BigDecimal.valueOf(item.getAiScore()) : null)
                .distanceFromMidpoint(
                        item.getDistanceFromMidpoint() != null
                                ? java.math.BigDecimal.valueOf(item.getDistanceFromMidpoint())
                                : null
                )
                .build();
    }
}
