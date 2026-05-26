package dev.promise4.GgUd.benchmark;

import dev.promise4.GgUd.controller.dto.Coordinate;
import dev.promise4.GgUd.controller.dto.DirectionsResponse;
import dev.promise4.GgUd.entity.SubwayStation;
import dev.promise4.GgUd.service.MidpointCalculationService;
import dev.promise4.GgUd.service.TMapDirectionsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

@SpringBootTest
@ActiveProfiles("benchmark")
@DisplayName("Cache Benchmark Test")
class CacheBenchmarkTest {

    @Autowired
    private MidpointCalculationService midpointCalculationService;

    @Autowired
    private TMapDirectionsService tMapDirectionsService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 테스트용 좌표: 강남역 → 홍대입구역
    private static final Coordinate GANGNAM = Coordinate.of(37.4979, 127.0276);
    private static final Coordinate HONGDAE = Coordinate.of(37.5575, 126.9246);

    @BeforeEach
    void clearCache() {
        // subwayStations 캐시 초기화
        var cache = cacheManager.getCache("subwayStations");
        if (cache != null) cache.clear();

        // TMap 캐시 키 초기화
        Set<String> keys = redisTemplate.keys("tmap:directions:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("[1] 지하철역 전체 조회 - Redis 캐시 미스 vs 히트")
    void benchmarkSubwayStationsCache() {
        System.out.println("\n========================================");
        System.out.println("[지하철역 캐시 벤치마크]");
        System.out.println("========================================");

        // --- 1차 호출: DB 조회 (cache miss) ---
        long start1 = System.currentTimeMillis();
        List<SubwayStation> stations1 = midpointCalculationService.getAllStations();
        long miss = System.currentTimeMillis() - start1;

        System.out.printf("조회된 역 수       : %d개%n", stations1.size());
        System.out.printf("1차 (캐시 미스, DB): %dms%n", miss);

        // --- 2차 호출: Redis 캐시 히트 ---
        long start2 = System.currentTimeMillis();
        List<SubwayStation> stations2 = midpointCalculationService.getAllStations();
        long hit = System.currentTimeMillis() - start2;

        System.out.printf("2차 (캐시 히트, Redis): %dms%n", hit);
        System.out.printf("개선율 : %.0f배 빠름 (%.1f%% 감소)%n",
                (double) miss / Math.max(hit, 1),
                (1.0 - (double) hit / Math.max(miss, 1)) * 100);
        System.out.println("========================================\n");

        assert !stations1.isEmpty();
        assert stations1.size() == stations2.size();
    }

    @Test
    @DisplayName("[2] TMap 대중교통 API - Redis 캐시 미스 vs 히트")
    void benchmarkTMapDirectionsCache() {
        System.out.println("\n========================================");
        System.out.println("[TMap API 캐시 벤치마크]");
        System.out.println("구간: 강남역(37.4979,127.0276) → 홍대입구역(37.5575,126.9246)");
        System.out.println("========================================");

        // --- 1차 호출: 외부 TMap API (cache miss) ---
        System.out.println("1차 호출 중 (TMap 외부 API)...");
        long start1 = System.currentTimeMillis();
        DirectionsResponse response1 = tMapDirectionsService.getDirections(GANGNAM, HONGDAE).block();
        long miss = System.currentTimeMillis() - start1;

        int routeCount = response1 != null && response1.getRouteOptions() != null
                ? response1.getRouteOptions().size() : 0;
        System.out.printf("응답 경로 수       : %d개%n", routeCount);
        System.out.printf("1차 (캐시 미스, API): %dms%n", miss);

        // --- 2차 호출: Redis 캐시 히트 ---
        long start2 = System.currentTimeMillis();
        DirectionsResponse response2 = tMapDirectionsService.getDirections(GANGNAM, HONGDAE).block();
        long hit = System.currentTimeMillis() - start2;

        System.out.printf("2차 (캐시 히트, Redis): %dms%n", hit);
        System.out.printf("개선율 : %.0f배 빠름 (%.1f%% 감소)%n",
                (double) miss / Math.max(hit, 1),
                (1.0 - (double) hit / Math.max(miss, 1)) * 100);
        System.out.println("========================================\n");
    }

    @Test
    @DisplayName("[3] 병렬 TMap 호출 - 5개 역 × 5명 순차 vs 병렬")
    void benchmarkParallelTMapCalls() {
        System.out.println("\n========================================");
        System.out.println("[병렬 처리 벤치마크]");
        System.out.println("5개 목적지에 대해 5개 출발지의 경로 조회");
        System.out.println("========================================");

        List<Coordinate> origins = List.of(
                Coordinate.of(37.5665, 126.9780), // 서울시청
                Coordinate.of(37.5172, 127.0473), // 강남
                Coordinate.of(37.5796, 126.9770), // 경복궁
                Coordinate.of(37.5326, 127.1001), // 잠실
                Coordinate.of(37.5400, 126.9509)  // 신촌
        );

        List<Coordinate> destinations = List.of(
                Coordinate.of(37.5575, 126.9246), // 홍대
                Coordinate.of(37.5049, 127.0260), // 양재
                Coordinate.of(37.5485, 127.0475), // 건대
                Coordinate.of(37.5195, 127.0567), // 선릉
                Coordinate.of(37.5007, 126.8675)  // 오목교
        );

        // 캐시 초기화 (공정한 비교를 위해)
        Set<String> keys = redisTemplate.keys("tmap:directions:*");
        if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);

        // --- 순차 처리 측정 ---
        System.out.println("순차 처리 측정 중...");
        long seqStart = System.currentTimeMillis();
        for (Coordinate origin : origins) {
            for (Coordinate dest : destinations) {
                tMapDirectionsService.getDirections(origin, dest).block();
            }
        }
        long seqTime = System.currentTimeMillis() - seqStart;
        System.out.printf("순차 처리 (25회): %dms%n", seqTime);

        // 캐시 초기화 후 병렬 측정
        keys = redisTemplate.keys("tmap:directions:*");
        if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);

        // --- 병렬 처리 측정 (Flux.merge per destination) ---
        System.out.println("병렬 처리 측정 중...");
        long parStart = System.currentTimeMillis();
        for (Coordinate dest : destinations) {
            // 각 목적지에 대해 5개 출발지를 Flux.merge로 병렬 호출
            var monos = origins.stream()
                    .map(origin -> tMapDirectionsService.getDirections(origin, dest))
                    .toList();
            reactor.core.publisher.Flux.merge(monos).collectList().block();
        }
        long parTime = System.currentTimeMillis() - parStart;
        System.out.printf("병렬 처리 (5dest × 5origins, Flux.merge): %dms%n", parTime);
        System.out.printf("개선율: %.1f배 빠름%n", (double) seqTime / Math.max(parTime, 1));
        System.out.println("========================================\n");
    }
}
