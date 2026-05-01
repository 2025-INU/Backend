package dev.promise4.GgUd.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_profile", indexes = {
        @Index(name = "idx_user_profile_user_id", columnList = "user_id", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "preferred_menu", columnDefinition = "TEXT")
    private String preferredMenu;

    @Column(name = "preferred_mood", columnDefinition = "TEXT")
    private String preferredMood;

    @Column(name = "preferred_companion", columnDefinition = "TEXT")
    private String preferredCompanion;

    @Column(name = "preferred_purpose", columnDefinition = "TEXT")
    private String preferredPurpose;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
