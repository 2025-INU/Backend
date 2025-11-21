package dev.promise4.GgUd.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 추천 장소 엔티티
 * AI 서버로부터 받은 장소 추천 결과를 저장
 */
@Entity
@Table(name = "ai_place_recommendations", indexes = {
    @Index(name = "idx_recommendations_meeting_id", columnList = "meeting_id"),
    @Index(name = "idx_recommendations_ranking", columnList = "meeting_id, ranking"),
    @Index(name = "idx_recommendations_selected", columnList = "is_selected")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiPlaceRecommendationsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_id")
    private Long recommendationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private MeetingsEntity meeting;

    @Column(name = "place_id", nullable = false, length = 100)
    private String placeId;

    @Column(name = "place_name", nullable = false, length = 100)
    private String placeName;

    @Column(length = 50)
    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(nullable = false)
    private Integer ranking;

    @Column(name = "ai_score", precision = 5, scale = 2)
    private BigDecimal aiScore;

    @Column(name = "distance_from_midpoint", precision = 10, scale = 2)
    private BigDecimal distanceFromMidpoint;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Lob
    @Column(name = "review_summary", columnDefinition = "TEXT")
    private String reviewSummary;

    @Column(name = "is_selected", nullable = false)
    @Builder.Default
    private Boolean isSelected = false;

    @Column(name = "selected_at")
    private LocalDateTime selectedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
