package dev.promise4.GgUd.repository;

import dev.promise4.GgUd.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Participant Repository
 */
@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    /**
     * 약속 ID로 참여자 목록 조회
     */
    List<Participant> findByPromiseId(Long promiseId);

    /**
     * 약속 ID와 사용자 ID로 참여자 조회
     */
    Optional<Participant> findByPromiseIdAndUserId(Long promiseId, Long userId);

    /**
     * 약속 ID와 사용자 ID로 참여 여부 확인
     */
    boolean existsByPromiseIdAndUserId(Long promiseId, Long userId);

    /**
     * 약속의 참여자 수 조회
     */
    long countByPromiseId(Long promiseId);

    /**
     * 사용자가 참여한 약속 ID 목록 조회
     */
    @Query("SELECT p.promise.id FROM Participant p WHERE p.user.id = :userId")
    List<Long> findPromiseIdsByUserId(@Param("userId") Long userId);

    /**
     * 약속의 모든 참여자가 출발지를 입력했는지 확인
     */
    @Query("SELECT COUNT(p) = 0 FROM Participant p WHERE p.promise.id = :promiseId AND p.isLocationSubmitted = false")
    boolean allParticipantsSubmittedLocation(@Param("promiseId") Long promiseId);

    /**
     * 약속의 출발지 입력 완료 참여자 수 조회
     */
    long countByPromiseIdAndIsLocationSubmittedTrue(Long promiseId);
}
