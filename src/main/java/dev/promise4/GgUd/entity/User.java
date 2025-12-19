package dev.promise4.GgUd.entity;

import dev.promise4.GgUd.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자 엔티티
 * 카카오 OAuth2 로그인 사용자 정보를 저장
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_kakao_id", columnList = "kakao_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kakao_id", unique = true, nullable = false, length = 100)
    private String kakaoId;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    @Column(length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 프로필 정보 업데이트
     */
    public void updateProfile(String nickname, String email, String profileImageUrl) {
        this.nickname = nickname;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
    }

    /**
     * 역할 변경
     */
    public void changeRole(UserRole role) {
        this.role = role;
    }
}
