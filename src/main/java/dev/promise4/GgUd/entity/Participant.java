package dev.promise4.GgUd.entity;

import dev.promise4.GgUd.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 참여자 엔티티
 */
@Entity
@Table(name = "participants", uniqueConstraints = {
        @UniqueConstraint(name = "uk_participant_promise_user", columnNames = { "promise_id", "user_id" })
}, indexes = {
        @Index(name = "idx_participants_promise_id", columnList = "promise_id"),
        @Index(name = "idx_participants_user_id", columnList = "user_id"),
        // 복합 인덱스: 약속별 출발지 제출 여부 조회 최적화
        @Index(name = "idx_participants_promise_location", columnList = "promise_id, is_location_submitted")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Participant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promise_id", nullable = false)
    private Promise promise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "departure_latitude")
    private Double departureLatitude;

    @Column(name = "departure_longitude")
    private Double departureLongitude;

    @Column(name = "departure_address", length = 200)
    private String departureAddress;

    @Column(name = "is_location_submitted")
    @Builder.Default
    private boolean isLocationSubmitted = false;

    @Column(name = "is_host")
    @Builder.Default
    private boolean isHost = false;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    /**
     * Builder 커스텀: joinedAt 자동 생성
     */
    public static class ParticipantBuilder {
        private LocalDateTime joinedAt = LocalDateTime.now();
    }

    /**
     * 출발지 입력/수정
     */
    public void submitDepartureLocation(Double latitude, Double longitude, String address) {
        this.departureLatitude = latitude;
        this.departureLongitude = longitude;
        this.departureAddress = address;
        this.isLocationSubmitted = true;
    }
}
