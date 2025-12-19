package dev.promise4.GgUd.repository;

import dev.promise4.GgUd.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 카카오 ID로 사용자 조회
     * 
     * @param kakaoId 카카오 고유 ID
     * @return 사용자 Optional
     */
    Optional<User> findByKakaoId(String kakaoId);
}
