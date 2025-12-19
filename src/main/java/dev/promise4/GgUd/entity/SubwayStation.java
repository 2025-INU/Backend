package dev.promise4.GgUd.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 지하철역 엔티티
 */
@Entity
@Table(name = "subway_stations", indexes = {
        @Index(name = "idx_subway_station_name", columnList = "station_name")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SubwayStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "station_name", nullable = false, length = 50)
    private String stationName;

    @Column(name = "line_name", nullable = false, length = 30)
    private String lineName;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;
}
