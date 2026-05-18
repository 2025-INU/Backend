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

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceRecommendationService {

    private static final int DEFAULT_RECOMMENDATION_LIMIT = 10;

    private final PromiseRepository promiseRepository;
    private final ParticipantRepository participantRepository;
    private final AiPlaceRecommendationClient aiPlaceRecommendationClient;
    private final AiPlaceRecommendationsRepository aiPlaceRecommendationsRepository;

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

        boolean isHost = promise.getHost().getId().equals(userId);
        PlaceRecommendationTab tab = request.getTab() != null ? request.getTab() : PlaceRecommendationTab.ALL;
        boolean hasQuery = StringUtils.hasText(request.getQuery());

        if (!hasQuery && tab == PlaceRecommendationTab.ALL) {
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
                return new PlaceRecommendationResponse(promiseId, items, isHost);
            }
        } else {
            log.debug("cache bypassed for promiseId={}, hasQuery={}, tab={}", promiseId, hasQuery, tab);
        }

        int limit = DEFAULT_RECOMMENDATION_LIMIT;
        String query = (request.getQuery() != null && !request.getQuery().isBlank())
                ? request.getQuery()
                : "";

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
            return new PlaceRecommendationResponse(promiseId, java.util.List.of(), isHost);
        }

        response.setPromiseId(promiseId);
        response.setHost(isHost);
        List<PlaceRecommendationItem> filtered = sortAndLimit(response.getRecommendations(), limit);
        response.setRecommendations(filtered);

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
        item.setImageUrl(entity.getImageUrl());
        item.setAiSummary(entity.getAiSummary());
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
                .imageUrl(item.getImageUrl())
                .aiSummary(item.getAiSummary())
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
