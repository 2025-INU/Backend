package dev.promise4.GgUd.service;

import dev.promise4.GgUd.controller.dto.*;
import dev.promise4.GgUd.entity.RefreshToken;
import dev.promise4.GgUd.entity.User;
import dev.promise4.GgUd.repository.RefreshTokenRepository;
import dev.promise4.GgUd.security.jwt.JwtTokenProvider;
import dev.promise4.GgUd.security.jwt.TokenBlacklistService;
import dev.promise4.GgUd.security.oauth.KakaoOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 인증 서비스
 * 로그인, 토큰 갱신, 로그아웃 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoOAuthService kakaoOAuthService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * 카카오 로그인 URL 생성
     */
    public KakaoLoginUrlResponse getKakaoLoginUrl() {
        KakaoOAuthService.KakaoLoginUrlResponse response = kakaoOAuthService.getKakaoLoginUrl();
        return KakaoLoginUrlResponse.builder()
                .loginUrl(response.loginUrl())
                .state(response.state())
                .build();
    }

    /**
     * 카카오 로그인 처리
     */
    @Transactional
    public LoginResponse processKakaoLogin(String code) {
        // 카카오 로그인 처리 및 사용자 정보 조회/저장
        User user = kakaoOAuthService.processKakaoLogin(code);

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // Refresh Token DB 저장
        saveRefreshToken(user.getId(), refreshToken);

        log.info("User logged in: userId={}, nickname={}", user.getId(), user.getNickname());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration() / 1000)
                .userId(user.getId())
                .nickname(user.getNickname())
                .build();
    }

    /**
     * 토큰 갱신
     */
    @Transactional
    public TokenRefreshResponse refreshToken(String refreshToken) {
        // 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다");
        }

        // 토큰에서 정보 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String tokenId = jwtTokenProvider.getTokenIdFromToken(refreshToken);

        // DB에서 Refresh Token 조회 및 검증
        RefreshToken storedToken = refreshTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Refresh Token입니다"));

        if (!storedToken.isValid()) {
            throw new IllegalArgumentException("만료되었거나 무효화된 Refresh Token입니다");
        }

        // 새 Access Token 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(userId);

        log.debug("Token refreshed for userId: {}", userId);

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration() / 1000)
                .build();
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(Long userId) {
        // Redis 블랙리스트에 사용자 토큰 무효화 등록
        tokenBlacklistService.revokeAllUserTokens(userId, jwtTokenProvider.getRefreshTokenExpiration());

        // DB에서도 해당 사용자의 모든 Refresh Token 무효화
        int revokedCount = refreshTokenRepository.revokeAllByUserId(userId);
        log.info("User logged out: userId={}, revokedTokens={}", userId, revokedCount);
    }

    /**
     * Refresh Token DB 저장
     */
    private void saveRefreshToken(Long userId, String refreshToken) {
        String tokenId = jwtTokenProvider.getTokenIdFromToken(refreshToken);
        LocalDateTime expiryDate = LocalDateTime.now()
                .plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000);

        // 기존 토큰 무효화 (선택적: 단일 세션 정책)
        refreshTokenRepository.revokeAllByUserId(userId);

        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .tokenId(tokenId)
                .expiryDate(expiryDate)
                .build();

        refreshTokenRepository.save(token);
    }
}
