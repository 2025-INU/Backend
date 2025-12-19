package dev.promise4.GgUd.entity;

import dev.promise4.GgUd.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 중간지점 투표 엔티티
 */
@Entity
@Table(name = "midpoint_votes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_vote_promise_participant", columnNames = { "promise_id", "participant_id" })
}, indexes = {
        @Index(name = "idx_vote_promise_id", columnList = "promise_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MidpointVote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promise_id", nullable = false)
    private Promise promise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subway_station_id", nullable = false)
    private SubwayStation subwayStation;

    @Column(name = "voted_at", nullable = false)
    private LocalDateTime votedAt;

    /**
     * Builder 커스텀: votedAt 자동 생성
     */
    public static class MidpointVoteBuilder {
        private LocalDateTime votedAt = LocalDateTime.now();
    }

    /**
     * 투표 변경
     */
    public void changeVote(SubwayStation newStation) {
        this.subwayStation = newStation;
        this.votedAt = LocalDateTime.now();
    }
}
