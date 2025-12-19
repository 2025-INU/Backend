package dev.promise4.GgUd.repository;

import dev.promise4.GgUd.entity.SubwayStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 지하철역 Repository
 */
@Repository
public interface SubwayStationRepository extends JpaRepository<SubwayStation, Long> {

    /**
     * 역 이름으로 검색
     */
    List<SubwayStation> findByStationNameContaining(String stationName);

    /**
     * 노선명으로 검색
     */
    List<SubwayStation> findByLineName(String lineName);
}
