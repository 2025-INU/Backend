package dev.promise4.GgUd.repository;

import dev.promise4.GgUd.entity.ExpenseRecordsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRecordRepository extends JpaRepository<ExpenseRecordsEntity, Long> {

    @Query("SELECT e FROM ExpenseRecordsEntity e LEFT JOIN FETCH e.user WHERE e.promise.id = :promiseId")
    List<ExpenseRecordsEntity> findByPromiseId(@Param("promiseId") Long promiseId);

    @Query("SELECT e FROM ExpenseRecordsEntity e WHERE e.promise.id = :promiseId AND e.user.id = :userId")
    Optional<ExpenseRecordsEntity> findByPromiseIdAndUserId(@Param("promiseId") Long promiseId, @Param("userId") Long userId);
}
