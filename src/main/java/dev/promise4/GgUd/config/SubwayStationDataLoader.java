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

    // isIncheon=true면 호선명 그대로 사용(IN_1, IN_2), false면 "N호선" 형식으로 변환
    private List<SubwayStation> loadFromCsv(String path, boolean isIncheon) throws Exception {
        ClassPathResource resource = new ClassPathResource(path);
        List<SubwayStation> stations = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {

            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) { isHeader = false; continue; }

                String[] fields = line.split(",");
                if (fields.length < 6) continue;

                try {
                    String stationName = fields[3].trim();
                    String rawLine = fields[1].trim();
                    String lineName = isIncheon ? rawLine : rawLine + "호선";
                    double latitude = Double.parseDouble(fields[4].trim());
                    double longitude = Double.parseDouble(fields[5].trim());

                    stations.add(SubwayStation.builder()
                            .stationName(stationName)
                            .lineName(lineName)
                            .latitude(latitude)
                            .longitude(longitude)
                            .build());
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse line in {}: {}", path, line);
                }
            }
        }

        log.info("Loaded {} stations from {}", stations.size(), path);
        return stations;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        // 이미 데이터가 있으면 스킵
        if (subwayStationRepository.count() > 0) {
            log.info("Subway station data already loaded. Skipping...");
            return;
        }

        log.info("Loading subway station data from CSV...");

        List<SubwayStation> stations = new ArrayList<>();
        stations.addAll(loadFromCsv("data/seoul_subway_stations.csv", false));
        stations.addAll(loadFromCsv("data/incheon_subway_stations.csv", true));

        subwayStationRepository.saveAll(stations);
        log.info("Loaded {} subway stations", stations.size());
    }
}
