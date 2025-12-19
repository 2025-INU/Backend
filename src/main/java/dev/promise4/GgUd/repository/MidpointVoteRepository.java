package dev.promise4.GgUd.repository;

import dev.promise4.GgUd.entity.MidpointVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MidpointVote Repository
 */
@Repository
public interface MidpointVoteRepository extends JpaRepository<MidpointVote, Long> {

    /**
     * 약속의 투표 목록 조회
     */
    List<MidpointVote> findByPromiseId(Long promiseId);

    /**
     * 참여자의 투표 조회
     */
    Optional<MidpointVote> findByPromiseIdAndParticipantId(Long promiseId, Long participantId);

    /**
     * 약속의 역별 투표 수 조회
     */
    @Query("SELECT v.subwayStation.id, COUNT(v) FROM MidpointVote v WHERE v.promise.id = :promiseId " +
            "GROUP BY v.subwayStation.id ORDER BY COUNT(v) DESC")
    List<Object[]> countVotesByStation(@Param("promiseId") Long promiseId);

    /**
     * 약속의 가장 많은 투표를 받은 역 조회
     */
    @Query("SELECT v.subwayStation FROM MidpointVote v WHERE v.promise.id = :promiseId " +
            "GROUP BY v.subwayStation ORDER BY COUNT(v) DESC")
    List<Object> findMostVotedStations(@Param("promiseId") Long promiseId);
}
