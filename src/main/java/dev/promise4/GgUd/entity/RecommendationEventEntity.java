package dev.promise4.GgUd.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_events", indexes = {
        @Index(name = "idx_recommendation_events_user", columnList = "user_id, event_type, created_at"),
        @Index(name = "idx_recommendation_events_promise", columnList = "promise_id, created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "promise_id", nullable = false)
    private Long promiseId;

    @Column(name = "place_id", length = 100)
    private String placeId;

    @Column(name = "place_name", length = 100)
    private String placeName;

    @Column(length = 50)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private RecommendationEventType eventType;

    @Column(name = "query_text", length = 200)
    private String queryText;

    @Column(name = "tab_name", length = 20)
    private String tabName;

    @Column(name = "rank_position")
    private Integer rankPosition;

    @Column(name = "time_slot", length = 20)
    private String timeSlot;

    @Column(name = "ai_score", precision = 5, scale = 2)
    private BigDecimal aiScore;

    @Column(name = "similarity_score", precision = 8, scale = 4)
    private BigDecimal similarityScore;

    @Column(name = "distance_from_midpoint", precision = 10, scale = 2)
    private BigDecimal distanceFromMidpoint;

    @Column(name = "final_score", precision = 6, scale = 4)
    private BigDecimal finalScore;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
