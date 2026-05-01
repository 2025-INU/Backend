package dev.promise4.GgUd.repository;

import dev.promise4.GgUd.entity.UserQueryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserQueryHistoryRepository extends JpaRepository<UserQueryHistory, Long> {

    List<UserQueryHistory> findTop10ByUserIdOrderBySearchedAtDesc(Long userId);
}
