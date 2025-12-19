package dev.promise4.GgUd.service;

import dev.promise4.GgUd.controller.dto.Coordinate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("MidpointCalculationService 테스트")
class MidpointCalculationServiceTest {

    private final MidpointCalculationService service = new MidpointCalculationService();

    @Nested
    @DisplayName("calculateMidpoint 테스트")
    class CalculateMidpointTest {

        @Test
        @DisplayName("2명의 출발지 중간지점을 계산한다")
        void calculateMidpoint_twoParticipants() {
            // given - 강남역(37.4979, 127.0276)과 홍대입구(37.5573, 126.9250)
            List<Coordinate> departures = List.of(
                    Coordinate.of(37.4979, 127.0276),
                    Coordinate.of(37.5573, 126.9250));

            // when
            Coordinate midpoint = service.calculateMidpoint(departures);

            // then - 중간은 대략 (37.5276, 126.9763)
            assertThat(midpoint.getLatitude()).isCloseTo(37.5276, within(0.001));
            assertThat(midpoint.getLongitude()).isCloseTo(126.9763, within(0.001));
        }

        @Test
        @DisplayName("3명의 출발지 중간지점을 계산한다")
        void calculateMidpoint_threeParticipants() {
            // given - 강남, 홍대, 잠실
            List<Coordinate> departures = List.of(
                    Coordinate.of(37.4979, 127.0276),
                    Coordinate.of(37.5573, 126.9250),
                    Coordinate.of(37.5133, 127.1001));

            // when
            Coordinate midpoint = service.calculateMidpoint(departures);

            // then
            double expectedLat = (37.4979 + 37.5573 + 37.5133) / 3;
            double expectedLon = (127.0276 + 126.9250 + 127.1001) / 3;
            assertThat(midpoint.getLatitude()).isCloseTo(expectedLat, within(0.0001));
            assertThat(midpoint.getLongitude()).isCloseTo(expectedLon, within(0.0001));
        }

        @Test
        @DisplayName("10명의 출발지 중간지점을 계산한다")
        void calculateMidpoint_tenParticipants() {
            // given - 서울 전역 10개 지점
            List<Coordinate> departures = List.of(
                    Coordinate.of(37.4979, 127.0276), // 강남
                    Coordinate.of(37.5573, 126.9250), // 홍대
                    Coordinate.of(37.5133, 127.1001), // 잠실
                    Coordinate.of(37.5547, 126.9707), // 서울역
                    Coordinate.of(37.5048, 127.0046), // 고속터미널
                    Coordinate.of(37.5215, 126.9242), // 여의도
                    Coordinate.of(37.5403, 127.0698), // 건대입구
                    Coordinate.of(37.5825, 127.0018), // 혜화
                    Coordinate.of(37.4765, 126.9817), // 사당
                    Coordinate.of(37.6555, 127.0612) // 노원
            );

            // when
            Coordinate midpoint = service.calculateMidpoint(departures);

            // then
            assertThat(midpoint.getLatitude()).isBetween(37.4, 37.7);
            assertThat(midpoint.getLongitude()).isBetween(126.9, 127.1);
        }
    }

    @Nested
    @DisplayName("Haversine 거리 계산 테스트")
    class DistanceCalculationTest {

        @Test
        @DisplayName("서울역에서 강남역까지 거리를 계산한다")
        void calculateDistance_seoulToGangnam() {
            // given
            Coordinate seoul = Coordinate.of(37.5547, 126.9707);
            Coordinate gangnam = Coordinate.of(37.4979, 127.0276);

            // when
            double distance = seoul.distanceTo(gangnam);

            // then - 약 8km
            assertThat(distance).isBetween(7.0, 9.0);
        }

        @Test
        @DisplayName("같은 위치의 거리는 0이다")
        void calculateDistance_sameLocation() {
            // given
            Coordinate location = Coordinate.of(37.5665, 126.9780);

            // when
            double distance = location.distanceTo(location);

            // then
            assertThat(distance).isEqualTo(0.0);
        }
    }
}
