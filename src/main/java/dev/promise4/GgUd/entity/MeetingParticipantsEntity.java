package dev.promise4.GgUd.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 약속 참여자 엔티티
 * 약속 초대, 수락/거절, 출발지 정보 관리
 */
@Entity
@Table(name = "meeting_participants",
    uniqueConstraints = {
        @UniqueConstraint(name = "unique_meeting_user", columnNames = {"meeting_id", "user_id"})
    },
    indexes = {
        @Index(name = "idx_participants_meeting_id", columnList = "meeting_id"),
        @Index(name = "idx_participants_user_id", columnList = "user_id"),
        @Index(name = "idx_participants_status", columnList = "invitation_status")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingParticipantsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participant_id")
    private Long participantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private MeetingsEntity meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UsersEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "invitation_status", nullable = false, length = 20)
    @Builder.Default
    private InvitationStatus invitationStatus = InvitationStatus.PENDING;

    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "departure_address", columnDefinition = "TEXT")
    private String departureAddress;

    @Column(name = "departure_latitude", precision = 10, scale = 8)
    private BigDecimal departureLatitude;

    @Column(name = "departure_longitude", precision = 11, scale = 8)
    private BigDecimal departureLongitude;

    @Column(name = "location_submitted_at")
    private LocalDateTime locationSubmittedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
