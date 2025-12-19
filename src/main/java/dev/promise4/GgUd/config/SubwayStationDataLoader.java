package dev.promise4.GgUd.config;

import dev.promise4.GgUd.entity.SubwayStation;
import dev.promise4.GgUd.repository.SubwayStationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * 서버 시작 시 지하철역 CSV 데이터 로딩
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubwayStationDataLoader implements ApplicationRunner {

    private final SubwayStationRepository subwayStationRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        // 이미 데이터가 있으면 스킵
        if (subwayStationRepository.count() > 0) {
            log.info("Subway station data already loaded. Skipping...");
            return;
        }

        log.info("Loading subway station data from CSV...");

        ClassPathResource resource = new ClassPathResource("data/seoul_subway_stations.csv");

        List<SubwayStation> stations = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), Charset.forName("EUC-KR")))) {

            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] fields = line.split(",");
                if (fields.length >= 6) {
                    try {
                        // CSV 컬럼: 순번,호선,역번호외부코드,역명,위도,경도,작성일자
                        String stationName = fields[3].trim();
                        String lineName = fields[1].trim() + "호선";
                        double latitude = Double.parseDouble(fields[4].trim());
                        double longitude = Double.parseDouble(fields[5].trim());

                        SubwayStation station = SubwayStation.builder()
                                .stationName(stationName)
                                .lineName(lineName)
                                .latitude(latitude)
                                .longitude(longitude)
                                .build();

                        stations.add(station);
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse line: {}", line);
                    }
                }
            }
        }

        subwayStationRepository.saveAll(stations);
        log.info("Loaded {} subway stations", stations.size());
    }
}
