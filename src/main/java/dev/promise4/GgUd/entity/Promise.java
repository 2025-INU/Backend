package dev.promise4.GgUd.entity;

import dev.promise4.GgUd.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * 약속 엔티티
 */
@Entity
@Table(name = "promises", indexes = {
        @Index(name = "idx_promises_invite_code", columnList = "invite_code"),
        @Index(name = "idx_promises_host_id", columnList = "host_id"),
        @Index(name = "idx_promises_status", columnList = "status"),
        // 복합 인덱스: 호스트별 상태 조회 최적화
        @Index(name = "idx_promises_host_status", columnList = "host_id, status"),
        // 복합 인덱스: 상태별 최신순 조회 최적화
        @Index(name = "idx_promises_status_created", columnList = "status, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Promise extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(length = 200)
    private String description;

    @Column(name = "promise_date_time", nullable = false)
    private LocalDateTime promiseDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PromiseStatus status = PromiseStatus.CREATED;

    @Column(name = "invite_code", nullable = false, unique = true, length = 6)
    private String inviteCode;

    @Column(name = "invite_expired_at", nullable = false)
    private LocalDateTime inviteExpiredAt;

    @Column(name = "max_participants")
    @Builder.Default
    private int maxParticipants = 10;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    // 중간지점 (선택된 지하철역)
    @Column(name = "midpoint_latitude")
    private Double midpointLatitude;

    @Column(name = "midpoint_longitude")
    private Double midpointLongitude;

    @Column(name = "midpoint_station_name", length = 100)
    private String midpointStationName;

    // 최종 약속 장소 (AI 추천 중 호스트가 확정한 장소)
    @Column(name = "confirmed_latitude")
    private Double confirmedLatitude;

    @Column(name = "confirmed_longitude")
    private Double confirmedLongitude;

    @Column(name = "confirmed_place_name", length = 100)
    private String confirmedPlaceName;

    @Column(name = "settlement_completed_at")
    private LocalDateTime settlementCompletedAt;

    /**
     * Builder 커스텀: inviteCode와 inviteExpiredAt 자동 생성
     */
    public static class PromiseBuilder {
        private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        private static final SecureRandom RANDOM = new SecureRandom();

        private String inviteCode = generateInviteCode();
        private LocalDateTime inviteExpiredAt = LocalDateTime.now().plusHours(24);

        private static String generateInviteCode() {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
            }
            return sb.toString();
        }
    }

    /**
     * 모집 시작 (CREATED → RECRUITING)
     */
    public void startRecruiting() {
        validateStatusTransition(PromiseStatus.CREATED, PromiseStatus.RECRUITING);
        this.status = PromiseStatus.RECRUITING;
    }

    /**
     * 중간 지점 선택 시작 (RECRUITING → SELECTING_MIDPOINT)
     * 호스트가 모든 참여자의 위치 입력 완료를 확인 후 수동으로 호출
     */
    public void startSelectingMidpoint() {
        validateStatusTransition(PromiseStatus.RECRUITING, PromiseStatus.SELECTING_MIDPOINT);
        this.status = PromiseStatus.SELECTING_MIDPOINT;
    }

    /**
     * 중간지점 확정 (SELECTING_MIDPOINT → MIDPOINT_CONFIRMED)
     */
    public void confirmMidpointStation(Double latitude, Double longitude, String stationName) {
        validateStatusTransition(PromiseStatus.SELECTING_MIDPOINT, PromiseStatus.MIDPOINT_CONFIRMED);
        this.midpointLatitude = latitude;
        this.midpointLongitude = longitude;
        this.midpointStationName = stationName;
        this.status = PromiseStatus.MIDPOINT_CONFIRMED;
    }

    /**
     * 최종 약속 장소 확정 (MIDPOINT_CONFIRMED 또는 PLACE_CONFIRMED → PLACE_CONFIRMED)
     * 호스트가 AI 추천 장소 중 하나를 선택
     */
    public void confirmFinalPlace(Double latitude, Double longitude, String placeName) {
        if (this.status != PromiseStatus.MIDPOINT_CONFIRMED && this.status != PromiseStatus.PLACE_CONFIRMED) {
            throw new IllegalStateException(
                    String.format("중간지점 확정 후에만 약속 장소를 확정할 수 있습니다. 현재 상태: %s", this.status));
        }
        this.confirmedLatitude = latitude;
        this.confirmedLongitude = longitude;
        this.confirmedPlaceName = placeName;
        this.status = PromiseStatus.PLACE_CONFIRMED;
    }

    /**
     * 중간지점 초기화 (MIDPOINT_CONFIRMED 또는 PLACE_CONFIRMED → SELECTING_MIDPOINT)
     * 호스트가 뒤로가기로 중간지점을 다시 선택할 때 사용. IN_PROGRESS 이전까지만 가능.
     */
    public void resetMidpoint() {
        if (this.status != PromiseStatus.MIDPOINT_CONFIRMED && this.status != PromiseStatus.PLACE_CONFIRMED) {
            throw new IllegalStateException(
                    String.format("중간지점 확정 또는 장소 확정 상태에서만 되돌릴 수 있습니다. 현재 상태: %s", this.status));
        }
        this.midpointLatitude = null;
        this.midpointLongitude = null;
        this.midpointStationName = null;
        this.confirmedLatitude = null;
        this.confirmedLongitude = null;
        this.confirmedPlaceName = null;
        this.status = PromiseStatus.SELECTING_MIDPOINT;
    }

    /**
     * 약속 진행 시작 (PLACE_CONFIRMED → IN_PROGRESS)
     */
    public void startProgress() {
        validateStatusTransition(PromiseStatus.PLACE_CONFIRMED, PromiseStatus.IN_PROGRESS);
        this.status = PromiseStatus.IN_PROGRESS;
    }

    /**
     * 약속 완료 (IN_PROGRESS → COMPLETED)
     */
    public void complete() {
        validateStatusTransition(PromiseStatus.IN_PROGRESS, PromiseStatus.COMPLETED);
        this.status = PromiseStatus.COMPLETED;
    }

    /**
     * 약속 취소
     */
    public void cancel() {
        if (this.status == PromiseStatus.COMPLETED || this.status == PromiseStatus.CANCELLED) {
            throw new IllegalStateException("이미 완료되었거나 취소된 약속입니다");
        }
        this.status = PromiseStatus.CANCELLED;
    }

    /**
     * 정산 완료 여부
     */
    public boolean isSettlementCompleted() {
        return this.settlementCompletedAt != null;
    }

    /**
     * 정산 완료 처리
     */
    public void completeSettlement() {
        this.settlementCompletedAt = LocalDateTime.now();
    }

    /**
     * 초대 코드 유효성 검증
     */
    public boolean isInviteValid() {
        return LocalDateTime.now().isBefore(inviteExpiredAt);
    }

    /**
     * 상태 전환 유효성 검증
     */
    private void validateStatusTransition(PromiseStatus expected, PromiseStatus next) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    String.format("잘못된 상태 전환입니다. 현재: %s, 기대: %s, 다음: %s",
                            this.status, expected, next));
        }
    }
}
