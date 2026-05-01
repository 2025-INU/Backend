package dev.promise4.GgUd.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_place_history", indexes = {
        @Index(name = "idx_user_place_history_user_id", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserPlaceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "place_id", nullable = false, length = 100)
    private String placeId;

    @Column(name = "query_id")
    private Long queryId;

    @CreatedDate
    @Column(name = "selected_at", nullable = false, updatable = false)
    private LocalDateTime selectedAt;
}
