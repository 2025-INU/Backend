package dev.promise4.GgUd.service;

import dev.promise4.GgUd.client.AiPlaceRecommendationClient;
import dev.promise4.GgUd.controller.dto.PlaceRecommendationRequest;
import dev.promise4.GgUd.controller.dto.PlaceRecommendationResponse;
import dev.promise4.GgUd.controller.dto.PlaceRecommendationItem;
import dev.promise4.GgUd.controller.dto.PlaceRecommendationTab;
import dev.promise4.GgUd.entity.AiPlaceRecommendationsEntity;
import dev.promise4.GgUd.entity.Promise;
import dev.promise4.GgUd.repository.ParticipantRepository;
import dev.promise4.GgUd.repository.PromiseRepository;
import dev.promise4.GgUd.repository.AiPlaceRecommendationsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

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

        PlaceRecommendationTab tab = request.getTab() != null ? request.getTab() : PlaceRecommendationTab.ALL;
        boolean hasQuery = StringUtils.hasText(request.getQuery());

        // 1. forceRefresh가 아니면 캐싱된 추천이 있으면 그대로 반환
        // query 기반 추천은 탭 필터 결과가 달라지므로 캐시를 우회한다.
        if (!Boolean.TRUE.equals(request.getForceRefresh()) && !hasQuery && tab == PlaceRecommendationTab.ALL) {
            var cached = aiPlaceRecommendationsRepository.findByPromiseIdOrderByRankingAsc(promiseId);
            if (!cached.isEmpty()) {
                log.debug("Found {} cached AI place recommendations for promiseId={}", cached.size(), promiseId);
                var items = cached.stream()
                        .map(this::toDto)
                        .sorted(Comparator.comparing(
                                PlaceRecommendationItem::getDistanceFromMidpoint,
                                Comparator.nullsLast(Double::compareTo)
                        ))
                        .toList();
                return new PlaceRecommendationResponse(promiseId, items);
            }
        } else {
            log.debug("cache bypassed for promiseId={}, hasQuery={}, tab={}, forceRefresh={}",
                    promiseId, hasQuery, tab, request.getForceRefresh());
        }

        int limit = request.getLimit() != null ? request.getLimit() : 10;
        // 기본 추천에서는 쿼리를 강제하지 않는다.
        // 사용자가 입력했을 때만 쿼리를 전달한다.
        String query = (request.getQuery() != null && !request.getQuery().isBlank())
                ? request.getQuery()
                : "";

        // 중간지점 좌표 전달 (근처 장소 추천에 활용)
        Double latitude = promise.getMidpointLatitude();
        Double longitude = promise.getMidpointLongitude();

        Mono<PlaceRecommendationResponse> mono = aiPlaceRecommendationClient.recommendPlaces(
                query,
                promiseId,
                latitude,
                longitude,
                limit,
                tab
        );

        PlaceRecommendationResponse response = mono.block();
        if (response == null) {
            log.warn("AI 서버로부터 null 응답 수신, 빈 추천 결과 반환");
            return new PlaceRecommendationResponse(promiseId, java.util.List.of());
        }

        response.setPromiseId(promiseId);
        List<PlaceRecommendationItem> filtered = sortAndLimit(response.getRecommendations(), limit);
        response.setRecommendations(filtered);

        // 2. 기본 탭 + 기본 추천(쿼리 미입력)일 때만 캐싱
        if (!hasQuery && tab == PlaceRecommendationTab.ALL) {
            aiPlaceRecommendationsRepository.deleteByPromiseId(promiseId);
            if (!response.getRecommendations().isEmpty()) {
                var entities = java.util.stream.IntStream.range(0, response.getRecommendations().size())
                        .mapToObj(i -> {
                            PlaceRecommendationItem item = response.getRecommendations().get(i);
                            return toEntity(promiseId, i + 1, item);
                        })
                        .toList();
                aiPlaceRecommendationsRepository.saveAll(entities);
                log.debug("Saved {} AI place recommendations for promiseId={}", entities.size(), promiseId);
            }
        }

        return response;
    }

    private List<PlaceRecommendationItem> sortAndLimit(
            List<PlaceRecommendationItem> items,
            int limit
    ) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .sorted(Comparator.comparing(
                        PlaceRecommendationItem::getDistanceFromMidpoint,
                        Comparator.nullsLast(Double::compareTo)
                ))
                .limit(limit)
                .toList();
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
