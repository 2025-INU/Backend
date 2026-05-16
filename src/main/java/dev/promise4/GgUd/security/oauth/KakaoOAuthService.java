package dev.promise4.GgUd.security.oauth;

import dev.promise4.GgUd.entity.User;
import dev.promise4.GgUd.entity.UserRole;
import dev.promise4.GgUd.repository.UserRepository;
import dev.promise4.GgUd.security.oauth.dto.KakaoUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

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
    public User processKakaoLoginWithToken(String kakaoAccessToken, String kakaoRefreshToken) {
        // 1. 액세스 토큰으로 사용자 정보 요청
        KakaoUserInfo userInfo = requestUserInfo(kakaoAccessToken);
        log.debug("Kakao user info received via SDK token: kakaoId={}", userInfo.getKakaoId());

        // 2. 사용자 정보로 회원 조회 또는 신규 가입
        User user = findOrCreateUser(userInfo, kakaoAccessToken);

        // 3. 카카오 액세스/리프레시 토큰 저장 (정산 메시지 전송 및 자체 갱신용)
        user.updateKakaoTokens(kakaoAccessToken, kakaoRefreshToken);
        return user;
    }

    /**
     * 저장된 카카오 리프레시 토큰으로 액세스 토큰 자체 갱신
     * 성공 시 user 엔티티에 새 토큰을 반영하고 새 액세스 토큰을 반환, 실패 시 null
     */
    @Transactional
    public String refreshKakaoAccessToken(User user) {
        String refreshToken = user.getKakaoRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("카카오 리프레시 토큰 없음, 자체 갱신 불가: userId={}", user.getId());
            return null;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", kakaoProperties.getClientId());
        form.add("refresh_token", refreshToken);
        if (kakaoProperties.getClientSecret() != null && !kakaoProperties.getClientSecret().isBlank()) {
            form.add("client_secret", kakaoProperties.getClientSecret());
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(kakaoProperties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.get("access_token") == null) {
                log.warn("카카오 토큰 갱신 응답에 access_token 없음: userId={}", user.getId());
                return null;
            }

            String newAccessToken = (String) response.get("access_token");
            String newRefreshToken = (String) response.get("refresh_token");
            user.updateKakaoTokens(newAccessToken, newRefreshToken);

            log.info("카카오 액세스 토큰 자체 갱신 성공: userId={}, refreshTokenRotated={}",
                    user.getId(), newRefreshToken != null);
            return newAccessToken;

        } catch (Exception e) {
            log.warn("카카오 액세스 토큰 자체 갱신 실패: userId={}, error={}", user.getId(), e.getMessage());
            return null;
        }
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
    private User findOrCreateUser(KakaoUserInfo userInfo, String kakaoAccessToken) {
        // 닉네임이 없는 경우 기본값 설정
        String nickname = userInfo.getNickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = "카카오유저_" + userInfo.getKakaoId();
            log.warn("Nickname is null for kakaoId={}, using default: {}", userInfo.getKakaoId(), nickname);
        }

        String finalNickname = nickname;

        return userRepository.findByKakaoId(userInfo.getKakaoId())
                .map(existingUser -> {
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
