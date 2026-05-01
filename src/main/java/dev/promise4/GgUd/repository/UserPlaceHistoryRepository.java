package dev.promise4.GgUd.repository;

import dev.promise4.GgUd.entity.UserPlaceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPlaceHistoryRepository extends JpaRepository<UserPlaceHistory, Long> {

    List<UserPlaceHistory> findTop20ByUserIdOrderBySelectedAtDesc(Long userId);
}
