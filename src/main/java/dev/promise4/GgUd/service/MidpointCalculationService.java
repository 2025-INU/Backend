package dev.promise4.GgUd.service;

import dev.promise4.GgUd.controller.dto.Coordinate;
import dev.promise4.GgUd.controller.dto.StationDistance;
import dev.promise4.GgUd.entity.SubwayStation;
import dev.promise4.GgUd.repository.SubwayStationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 중간지점 계산 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MidpointCalculationService {

    private final SubwayStationRepository subwayStationRepository;

    // 기본 생성자 (테스트용)
    public MidpointCalculationService() {
        this.subwayStationRepository = null;
    }

    /**
     * 여러 출발지의 중간지점 계산 (평균 좌표)
     */
    public Coordinate calculateMidpoint(List<Coordinate> departures) {
        if (departures == null || departures.isEmpty()) {
            throw new IllegalArgumentException("출발지 목록이 비어있습니다");
        }

        double sumLat = 0;
        double sumLon = 0;

        for (Coordinate coord : departures) {
            sumLat += coord.getLatitude();
            sumLon += coord.getLongitude();
        }

        double avgLat = sumLat / departures.size();
        double avgLon = sumLon / departures.size();

        log.debug("Calculated midpoint: ({}, {}) from {} departures", avgLat, avgLon, departures.size());

        return Coordinate.of(avgLat, avgLon);
    }

    /**
     * 중간지점에서 가장 가까운 지하철역 찾기
     */
    public List<StationDistance> findNearestStations(Coordinate midpoint, int count) {
        if (subwayStationRepository == null) {
            throw new IllegalStateException("SubwayStationRepository is not initialized");
        }

        List<SubwayStation> allStations = subwayStationRepository.findAll();

        return allStations.stream()
                .map(station -> {
                    Coordinate stationCoord = Coordinate.of(station.getLatitude(), station.getLongitude());
                    double distance = midpoint.distanceTo(stationCoord);
                    return new StationDistance(station, distance);
                })
                .sorted(Comparator.comparingDouble(StationDistance::getDistanceKm))
                .limit(count)
                .toList();
    }

    /**
     * 참여자들로부터 각 역까지의 평균 거리 계산
     */
    public double calculateAverageDistance(SubwayStation station, List<Coordinate> departures) {
        Coordinate stationCoord = Coordinate.of(station.getLatitude(), station.getLongitude());

        return departures.stream()
                .mapToDouble(dep -> dep.distanceTo(stationCoord))
                .average()
                .orElse(0.0);
    }
}
