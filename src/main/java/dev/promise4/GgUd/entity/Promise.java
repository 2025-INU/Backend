package dev.promise4.GgUd.entity;

import dev.promise4.GgUd.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

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

    @Column(name = "invite_code", nullable = false, unique = true, length = 36)
    private String inviteCode;

    @Column(name = "invite_expired_at", nullable = false)
    private LocalDateTime inviteExpiredAt;

    @Column(name = "max_participants")
    @Builder.Default
    private int maxParticipants = 10;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

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
        private String inviteCode = UUID.randomUUID().toString();
        private LocalDateTime inviteExpiredAt = LocalDateTime.now().plusHours(24);
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
     * 장소 확정 (SELECTING_MIDPOINT → CONFIRMED)
     */
    public void confirmLocation(Double latitude, Double longitude, String placeName) {
        validateStatusTransition(PromiseStatus.SELECTING_MIDPOINT, PromiseStatus.CONFIRMED);
        this.confirmedLatitude = latitude;
        this.confirmedLongitude = longitude;
        this.confirmedPlaceName = placeName;
        this.status = PromiseStatus.CONFIRMED;
    }

    /**
     * 약속 진행 시작 (CONFIRMED → IN_PROGRESS)
     */
    public void startProgress() {
        validateStatusTransition(PromiseStatus.CONFIRMED, PromiseStatus.IN_PROGRESS);
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
