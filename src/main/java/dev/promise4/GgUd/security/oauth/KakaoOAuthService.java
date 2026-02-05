package dev.promise4.GgUd.security.oauth;

import dev.promise4.GgUd.entity.User;
import dev.promise4.GgUd.entity.UserRole;
import dev.promise4.GgUd.repository.UserRepository;
import dev.promise4.GgUd.security.oauth.dto.KakaoUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/**
 * 카카오 OAuth2 로그인 서비스 (모바일 SDK 전용)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

    private final KakaoOAuthProperties kakaoProperties;
    private final UserRepository userRepository;
    private final RestClient restClient;

    /**
     * 카카오 SDK 로그인 처리 (액세스 토큰 방식 - 모바일용)
     *
     * @param kakaoAccessToken 카카오 SDK에서 받은 액세스 토큰
     * @return 로그인된 사용자 정보
     */
    @Transactional
    public User processKakaoLoginWithToken(String kakaoAccessToken) {
        // 1. 액세스 토큰으로 사용자 정보 요청
        KakaoUserInfo userInfo = requestUserInfo(kakaoAccessToken);
        log.debug("Kakao user info received via SDK token: kakaoId={}", userInfo.getKakaoId());

        // 2. 사용자 정보로 회원 조회 또는 신규 가입
        return findOrCreateUser(userInfo);
    }

    /**
     * 액세스 토큰으로 사용자 정보 요청
     */
    private KakaoUserInfo requestUserInfo(String accessToken) {
        return restClient.get()
                .uri(kakaoProperties.getUserInfoUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(KakaoUserInfo.class);
    }

    /**
     * 카카오 ID로 기존 사용자 조회 또는 신규 생성
     */
    private User findOrCreateUser(KakaoUserInfo userInfo) {
        // 닉네임이 없는 경우 기본값 설정
        String nickname = userInfo.getNickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = "카카오유저_" + userInfo.getKakaoId();
            log.warn("Nickname is null for kakaoId={}, using default: {}", userInfo.getKakaoId(), nickname);
        }

        String finalNickname = nickname;

        return userRepository.findByKakaoId(userInfo.getKakaoId())
                .map(existingUser -> {
                    // 기존 사용자: 프로필 정보 업데이트
                    existingUser.updateProfile(
                            finalNickname,
                            userInfo.getEmail(),
                            userInfo.getProfileImageUrl());
                    log.info("Existing user logged in: kakaoId={}", userInfo.getKakaoId());
                    return existingUser;
                })
                .orElseGet(() -> {
                    // 신규 사용자: 회원 가입
                    User newUser = User.builder()
                            .kakaoId(userInfo.getKakaoId())
                            .nickname(finalNickname)
                            .email(userInfo.getEmail())
                            .profileImageUrl(userInfo.getProfileImageUrl())
                            .role(UserRole.USER)
                            .build();
                    log.info("New user registered: kakaoId={}", userInfo.getKakaoId());
                    return userRepository.save(newUser);
                });
    }
}
