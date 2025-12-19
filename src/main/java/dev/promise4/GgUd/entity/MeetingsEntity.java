package dev.promise4.GgUd.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 약속 엔티티
 * 약속 생성, 중간 지점 계산, 장소 선택 관리
 */
@Entity
@Table(name = "meetings", indexes = {
        @Index(name = "idx_meetings_creator_id", columnList = "creator_id"),
        @Index(name = "idx_meetings_status", columnList = "status"),
        @Index(name = "idx_meetings_datetime", columnList = "meeting_datetime")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meeting_id")
    private Long meetingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(name = "meeting_name", nullable = false, length = 100)
    private String meetingName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "meeting_datetime", nullable = false)
    private LocalDateTime meetingDatetime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MeetingStatus status = MeetingStatus.PLANNING;

    @Column(name = "midpoint_latitude", precision = 10, scale = 8)
    private BigDecimal midpointLatitude;

    @Column(name = "midpoint_longitude", precision = 11, scale = 8)
    private BigDecimal midpointLongitude;

    @Column(name = "selected_place_id", length = 100)
    private String selectedPlaceId;

    @Column(name = "selected_place_name", length = 100)
    private String selectedPlaceName;

    @Column(name = "selected_place_address", columnDefinition = "TEXT")
    private String selectedPlaceAddress;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MeetingParticipantsEntity> participants = new ArrayList<>();
}
