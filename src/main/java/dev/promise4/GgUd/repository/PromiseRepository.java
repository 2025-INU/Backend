package dev.promise4.GgUd.repository;

import dev.promise4.GgUd.entity.Promise;
import dev.promise4.GgUd.entity.PromiseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Promise Repository
 */
@Repository
public interface PromiseRepository extends JpaRepository<Promise, Long> {

    /**
     * 초대 코드로 약속 조회
     */
    Optional<Promise> findByInviteCode(String inviteCode);

    /**
     * 호스트 ID로 약속 목록 조회
     */
    Page<Promise> findByHostId(Long hostId, Pageable pageable);

    /**
     * 호스트 ID와 상태로 약속 목록 조회
     */
    Page<Promise> findByHostIdAndStatus(Long hostId, PromiseStatus status, Pageable pageable);

    /**
     * 상태로 약속 목록 조회
     */
    Page<Promise> findByStatus(PromiseStatus status, Pageable pageable);

    /**
     * 사용자가 참여한 약속 목록 조회 (호스트 또는 참여자)
     */
    @Query("SELECT DISTINCT p FROM Promise p LEFT JOIN Participant pt ON pt.promise = p " +
            "WHERE p.host.id = :userId OR pt.user.id = :userId")
    Page<Promise> findByUserParticipation(@Param("userId") Long userId, Pageable pageable);
}
