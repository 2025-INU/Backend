package dev.promise4.GgUd.security.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카카오 사용자 정보 DTO
 * 카카오 사용자 정보 API 응답을 매핑
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KakaoUserInfo {

    private Long id;

    @JsonProperty("connected_at")
    private String connectedAt;

    private Properties properties;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    /**
     * 카카오 프로필 정보
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Properties {
        private String nickname;

        @JsonProperty("profile_image")
        private String profileImage;

        @JsonProperty("thumbnail_image")
        private String thumbnailImage;
    }

    /**
     * 카카오 계정 정보
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KakaoAccount {

        @JsonProperty("profile_nickname_needs_agreement")
        private Boolean profileNicknameNeedsAgreement;

        private Profile profile;

        @JsonProperty("has_email")
        private Boolean hasEmail;

        @JsonProperty("email_needs_agreement")
        private Boolean emailNeedsAgreement;

        @JsonProperty("is_email_valid")
        private Boolean isEmailValid;

        @JsonProperty("is_email_verified")
        private Boolean isEmailVerified;

        private String email;
    }

    /**
     * 프로필 정보
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Profile {
        private String nickname;

        @JsonProperty("thumbnail_image_url")
        private String thumbnailImageUrl;

        @JsonProperty("profile_image_url")
        private String profileImageUrl;

        @JsonProperty("is_default_image")
        private Boolean isDefaultImage;
    }

    /**
     * 카카오 ID를 문자열로 반환
     */
    public String getKakaoId() {
        return String.valueOf(id);
    }

    /**
     * 닉네임 반환 (kakaoAccount.profile 또는 properties에서)
     */
    public String getNickname() {
        if (kakaoAccount != null && kakaoAccount.getProfile() != null) {
            return kakaoAccount.getProfile().getNickname();
        }
        if (properties != null) {
            return properties.getNickname();
        }
        return null;
    }

    /**
     * 프로필 이미지 URL 반환
     */
    public String getProfileImageUrl() {
        if (kakaoAccount != null && kakaoAccount.getProfile() != null) {
            return kakaoAccount.getProfile().getProfileImageUrl();
        }
        if (properties != null) {
            return properties.getProfileImage();
        }
        return null;
    }

    /**
     * 이메일 반환
     */
    public String getEmail() {
        if (kakaoAccount != null) {
            return kakaoAccount.getEmail();
        }
        return null;
    }
}
