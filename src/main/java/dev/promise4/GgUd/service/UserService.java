package dev.promise4.GgUd.service;

import dev.promise4.GgUd.entity.User;
import dev.promise4.GgUd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 서비스
 * 사용자 조회 및 관리 기능 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 사용자 ID로 조회 (캐시 적용)
     *
     * @param userId 사용자 ID
     * @return 사용자 엔티티
     */
    @Cacheable(value = "users", key = "#userId")
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        log.debug("Fetching user from DB: userId={}", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }

    /**
     * 카카오 ID로 조회
     *
     * @param kakaoId 카카오 고유 ID
     * @return 사용자 엔티티
     */
    @Transactional(readOnly = true)
    public User getUserByKakaoId(String kakaoId) {
        return userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: kakaoId=" + kakaoId));
    }

    /**
     * 프로필 업데이트 (캐시 무효화)
     *
     * @param userId          사용자 ID
     * @param nickname        닉네임
     * @param email           이메일
     * @param profileImageUrl 프로필 이미지 URL
     * @return 업데이트된 사용자
     */
    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    public User updateProfile(Long userId, String nickname, String email, String profileImageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        user.updateProfile(nickname, email, profileImageUrl);
        log.info("User profile updated: userId={}, nickname={}", userId, nickname);

        return user;
    }

    /**
     * 사용자 존재 여부 확인
     *
     * @param userId 사용자 ID
     * @return 존재 여부
     */
    @Transactional(readOnly = true)
    public boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }
}
