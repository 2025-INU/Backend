package dev.promise4.GgUd.repository;

import dev.promise4.GgUd.entity.Promise;
import dev.promise4.GgUd.entity.PromiseStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Promise Repository
 */
@Repository
public interface PromiseRepository extends JpaRepository<Promise, Long> {

    /**
     * 초대 코드로 약속 조회 (host FETCH JOIN)
     */
    @Query("SELECT p FROM Promise p LEFT JOIN FETCH p.host WHERE p.inviteCode = :inviteCode")
    Optional<Promise> findByInviteCode(@Param("inviteCode") String inviteCode);

    /**
     * 초대 코드로 약속 조회 + Pessimistic Lock (참여 시 동시성 제어용)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Promise p WHERE p.inviteCode = :inviteCode")
    Optional<Promise> findByInviteCodeWithLock(@Param("inviteCode") String inviteCode);

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
     * 사용자가 참여한 약속 목록 조회 (호스트 또는 참여자) - FETCH JOIN 적용
     */
    @Query("SELECT DISTINCT p FROM Promise p " +
            "LEFT JOIN FETCH p.host " +
            "LEFT JOIN Participant pt ON pt.promise = p " +
            "WHERE p.host.id = :userId OR pt.user.id = :userId")
    Page<Promise> findByUserParticipation(@Param("userId") Long userId, Pageable pageable);

    /**
     * 약속 ID로 조회 (host FETCH JOIN)
     */
    @Query("SELECT p FROM Promise p LEFT JOIN FETCH p.host WHERE p.id = :id")
    Optional<Promise> findByIdWithHost(@Param("id") Long id);

    /**
     * 사용자가 참여한 약속 중 상태 필터링 조회
     */
    @Query(value = "SELECT DISTINCT p FROM Promise p " +
            "LEFT JOIN FETCH p.host " +
            "LEFT JOIN Participant pt ON pt.promise = p " +
            "WHERE (p.host.id = :userId OR pt.user.id = :userId) " +
            "AND p.status = :status",
            countQuery = "SELECT COUNT(DISTINCT p.id) FROM Promise p " +
                    "LEFT JOIN Participant pt ON pt.promise = p " +
                    "WHERE (p.host.id = :userId OR pt.user.id = :userId) " +
                    "AND p.status = :status")
    Page<Promise> findByUserParticipationAndStatus(
            @Param("userId") Long userId,
            @Param("status") PromiseStatus status,
            Pageable pageable);

    /**
     * 사용자가 참여한 약속 중 키워드 검색 (약속 제목 또는 참여자 닉네임)
     */
    @Query(value = "SELECT DISTINCT p FROM Promise p " +
            "LEFT JOIN FETCH p.host " +
            "LEFT JOIN Participant pt ON pt.promise = p " +
            "WHERE (p.host.id = :userId OR pt.user.id = :userId) " +
            "AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR EXISTS (SELECT 1 FROM Participant pt2 JOIN pt2.user u " +
            "                WHERE pt2.promise = p AND LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))))",
            countQuery = "SELECT COUNT(DISTINCT p.id) FROM Promise p " +
                    "LEFT JOIN Participant pt ON pt.promise = p " +
                    "WHERE (p.host.id = :userId OR pt.user.id = :userId) " +
                    "AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "     OR EXISTS (SELECT 1 FROM Participant pt2 JOIN pt2.user u " +
                    "                WHERE pt2.promise = p AND LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))))")
    Page<Promise> findByUserParticipationAndKeyword(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 사용자가 참여한 약속 중 상태 + 키워드 검색
     */
    @Query(value = "SELECT DISTINCT p FROM Promise p " +
            "LEFT JOIN FETCH p.host " +
            "LEFT JOIN Participant pt ON pt.promise = p " +
            "WHERE (p.host.id = :userId OR pt.user.id = :userId) " +
            "AND p.status = :status " +
            "AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR EXISTS (SELECT 1 FROM Participant pt2 JOIN pt2.user u " +
            "                WHERE pt2.promise = p AND LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))))",
            countQuery = "SELECT COUNT(DISTINCT p.id) FROM Promise p " +
                    "LEFT JOIN Participant pt ON pt.promise = p " +
                    "WHERE (p.host.id = :userId OR pt.user.id = :userId) " +
                    "AND p.status = :status " +
                    "AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "     OR EXISTS (SELECT 1 FROM Participant pt2 JOIN pt2.user u " +
                    "                WHERE pt2.promise = p AND LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))))")
    Page<Promise> findByUserParticipationAndStatusAndKeyword(
            @Param("userId") Long userId,
            @Param("status") PromiseStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 스케줄러: CONFIRMED 상태 + promiseDateTime 임박한 약속 조회
     */
    @Query("SELECT p FROM Promise p WHERE p.status = :status AND p.promiseDateTime <= :threshold")
    List<Promise> findByStatusAndPromiseDateTimeBefore(
            @Param("status") PromiseStatus status,
            @Param("threshold") LocalDateTime threshold);

    /**
     * 스케줄러: 만료된 초대 링크를 가진 RECRUITING 상태 약속 조회
     */
    @Query("SELECT p FROM Promise p WHERE p.status = 'RECRUITING' AND p.inviteExpiredAt < :now")
    List<Promise> findExpiredRecruitingPromises(@Param("now") LocalDateTime now);
}
