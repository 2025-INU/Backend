package dev.promise4.GgUd.service;

import dev.promise4.GgUd.controller.dto.LocationUpdateMessage;
import dev.promise4.GgUd.controller.dto.MapDataResponse;
import dev.promise4.GgUd.controller.dto.ParticipantLocationResponse;
import dev.promise4.GgUd.entity.Participant;
import dev.promise4.GgUd.entity.Promise;
import dev.promise4.GgUd.entity.PromiseStatus;
import dev.promise4.GgUd.repository.ParticipantRepository;
import dev.promise4.GgUd.repository.PromiseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 카카오맵 데이터 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MapDataService {

    private final PromiseRepository promiseRepository;
    private final ParticipantRepository participantRepository;
    private final LocationTrackingService locationTrackingService;

    /**
     * 약속의 지도 데이터 조회
     */
    @Transactional(readOnly = true)
    public MapDataResponse getMapData(Long promiseId) {
        Promise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new IllegalArgumentException("약속을 찾을 수 없습니다"));

        List<Participant> participants = participantRepository.findByPromiseId(promiseId);

        // 확정 장소 (CONFIRMED 이후)
        MapDataResponse.MapMarker destination = null;
        if (promise.getConfirmedLatitude() != null && promise.getConfirmedLongitude() != null) {
            destination = MapDataResponse.MapMarker.builder()
                    .latitude(promise.getConfirmedLatitude())
                    .longitude(promise.getConfirmedLongitude())
                    .name(promise.getConfirmedPlaceName())
                    .type(MapDataResponse.MarkerType.DESTINATION)
                    .build();
        }

        // 참여자 출발지
        List<MapDataResponse.ParticipantMarker> departures = participants.stream()
                .filter(Participant::isLocationSubmitted)
                .map(p -> MapDataResponse.ParticipantMarker.builder()
                        .userId(p.getUser().getId())
                        .nickname(p.getUser().getNickname())
                        .profileImageUrl(p.getUser().getProfileImageUrl())
                        .latitude(p.getDepartureLatitude())
                        .longitude(p.getDepartureLongitude())
                        .host(p.isHost())
                        .build())
                .toList();

        // 실시간 위치 (PLACE_CONFIRMED 또는 IN_PROGRESS - 위치 추적 허용 상태)
        List<MapDataResponse.ParticipantMarker> currentLocations = new ArrayList<>();
        if (promise.getStatus() == PromiseStatus.PLACE_CONFIRMED
                || promise.getStatus() == PromiseStatus.IN_PROGRESS) {
            Map<Long, Participant> participantMap = participants.stream()
                    .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));
            ParticipantLocationResponse locations = locationTrackingService.getParticipantLocations(promiseId);
            currentLocations = locations.getLocations().stream()
                    .map(loc -> {
                        Participant p = participantMap.get(loc.getUserId());
                        return MapDataResponse.ParticipantMarker.builder()
                                .userId(loc.getUserId())
                                .nickname(loc.getNickname())
                                .profileImageUrl(p != null ? p.getUser().getProfileImageUrl() : null)
                                .latitude(loc.getLatitude())
                                .longitude(loc.getLongitude())
                                .host(p != null && p.isHost())
                                .build();
                    })
                    .toList();
        }

        return MapDataResponse.builder()
                .promiseId(promiseId)
                .destination(destination)
                .participantDepartures(departures)
                .recommendedMidpoints(List.of()) // 추천 중간지점은 별도 API에서 제공
                .currentLocations(currentLocations)
                .build();
    }
}
