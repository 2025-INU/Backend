package dev.promise4.GgUd.service;

import dev.promise4.GgUd.controller.dto.*;
import dev.promise4.GgUd.entity.*;
import dev.promise4.GgUd.repository.ParticipantRepository;
import dev.promise4.GgUd.repository.PromiseRepository;
import dev.promise4.GgUd.repository.SubwayStationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 중간지점 추천 및 확정 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MidpointService {

    private final PromiseRepository promiseRepository;
    private final ParticipantRepository participantRepository;
    private final SubwayStationRepository subwayStationRepository;
    private final MidpointCalculationService midpointCalculationService;

    /**
     * 중간지점 추천 조회
     */
    @Transactional(readOnly = true)
    public MidpointRecommendationResponse getRecommendations(Long promiseId) {
        Promise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new IllegalArgumentException("약속을 찾을 수 없습니다"));

        // 상태 확인
        if (promise.getStatus() != PromiseStatus.SELECTING_MIDPOINT) {
            throw new IllegalStateException("중간지점 선택 단계가 아닙니다. 현재 상태: " + promise.getStatus());
        }

        // 참여자 출발지 목록
        List<Participant> participants = participantRepository.findByPromiseId(promiseId);
        List<Coordinate> departures = participants.stream()
                .filter(Participant::isLocationSubmitted)
                .map(p -> Coordinate.of(p.getDepartureLatitude(), p.getDepartureLongitude()))
                .toList();

        if (departures.isEmpty()) {
            throw new IllegalStateException("출발지를 입력한 참여자가 없습니다");
        }

        // 중간지점 계산
        Coordinate midpoint = midpointCalculationService.calculateMidpoint(departures);

        // 가까운 역 5개 찾기
        List<StationDistance> nearestStations = midpointCalculationService.findNearestStations(midpoint, 5);

        // 추천 결과 생성
        List<StationRecommendation> recommendations = nearestStations.stream()
                .map(sd -> StationRecommendation.from(
                        sd.getStation(),
                        sd.getDistanceKm(),
                        midpointCalculationService.calculateAverageDistance(sd.getStation(), departures)))
                .toList();

        log.info("Midpoint recommendations generated: promiseId={}, midpoint=({}, {}), stationCount={}",
                promiseId, midpoint.getLatitude(), midpoint.getLongitude(), recommendations.size());

        return MidpointRecommendationResponse.builder()
                .calculatedMidpoint(midpoint)
                .recommendedStations(recommendations)
                .participantCount(participants.size())
                .build();
    }

    /**
     * 중간지점 확정 (호스트만)
     */
    @Transactional
    public void confirmMidpoint(Long promiseId, Long userId, Long stationId) {
        Promise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new IllegalArgumentException("약속을 찾을 수 없습니다"));

        // 호스트 확인
        if (!promise.getHost().getId().equals(userId)) {
            throw new IllegalStateException("호스트만 확정할 수 있습니다");
        }

        if (promise.getStatus() != PromiseStatus.SELECTING_MIDPOINT) {
            throw new IllegalStateException("확정 가능한 단계가 아닙니다");
        }

        SubwayStation station = subwayStationRepository.findById(stationId)
                .orElseThrow(() -> new IllegalArgumentException("역을 찾을 수 없습니다"));

        // 확정
        promise.confirmLocation(station.getLatitude(), station.getLongitude(), station.getStationName());

        log.info("Midpoint confirmed: promiseId={}, stationId={}, stationName={}",
                promiseId, stationId, station.getStationName());
    }
}
