package dev.promise4.GgUd.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_place_preference_profiles")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPlacePreferenceProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "top_category", length = 50)
    private String topCategory;

    @Column(name = "top_category_score", precision = 6, scale = 4)
    private BigDecimal topCategoryScore;

    @Column(name = "top_region", length = 100)
    private String topRegion;

    @Column(name = "top_region_score", precision = 6, scale = 4)
    private BigDecimal topRegionScore;

    @Column(name = "preferred_time_slot", length = 20)
    private String preferredTimeSlot;

    @Column(name = "time_slot_score", precision = 6, scale = 4)
    private BigDecimal timeSlotScore;

    @Column(name = "selection_count", nullable = false)
    @Builder.Default
    private Long selectionCount = 0L;

    @Column(name = "last_selected_place_id", length = 100)
    private String lastSelectedPlaceId;

    @Column(name = "last_selected_place_name", length = 100)
    private String lastSelectedPlaceName;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
