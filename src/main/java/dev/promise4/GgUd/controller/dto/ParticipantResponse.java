package dev.promise4.GgUd.controller.dto;

import dev.promise4.GgUd.entity.Participant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 참여자 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "참여자 응답")
public class ParticipantResponse {

    @Schema(description = "참여자 ID", example = "1")
    private Long id;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "닉네임", example = "홍길동")
    private String nickname;

    @Schema(description = "프로필 이미지 URL")
    private String profileImageUrl;

    @Schema(description = "출발지 위도")
    private Double departureLatitude;

    @Schema(description = "출발지 경도")
    private Double departureLongitude;

    @Schema(description = "출발지 주소")
    private String departureAddress;

    @Schema(description = "출발지 입력 완료 여부", example = "true")
    private boolean locationSubmitted;

    @Schema(description = "호스트 여부", example = "false")
    private boolean host;

    @Schema(description = "참여 시간")
    private LocalDateTime joinedAt;

    public static ParticipantResponse from(Participant participant) {
        return ParticipantResponse.builder()
                .id(participant.getId())
                .userId(participant.getUser().getId())
                .nickname(participant.getUser().getNickname())
                .profileImageUrl(participant.getUser().getProfileImageUrl())
                .departureLatitude(participant.getDepartureLatitude())
                .departureLongitude(participant.getDepartureLongitude())
                .departureAddress(participant.getDepartureAddress())
                .locationSubmitted(participant.isLocationSubmitted())
                .host(participant.isHost())
                .joinedAt(participant.getJoinedAt())
                .build();
    }
}
