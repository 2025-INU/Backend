package dev.promise4.GgUd.controller.dto;

import dev.promise4.GgUd.entity.Promise;
import dev.promise4.GgUd.entity.PromiseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 약속 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "약속 응답")
public class PromiseResponse {

    @Schema(description = "약속 ID", example = "1")
    private Long id;

    @Schema(description = "약속 제목", example = "강남역 모임")
    private String title;

    @Schema(description = "약속 설명", example = "친구들과 저녁 식사")
    private String description;

    @Schema(description = "약속 일시", example = "2024-12-20T18:00:00")
    private LocalDateTime promiseDateTime;

    @Schema(description = "약속 상태", example = "CREATED")
    private PromiseStatus status;

    @Schema(description = "초대 코드", example = "550e8400-e29b-41d4-a716-446655440000")
    private String inviteCode;

    @Schema(description = "초대 만료 시간")
    private LocalDateTime inviteExpiredAt;

    @Schema(description = "최대 참여자 수", example = "10")
    private int maxParticipants;

    @Schema(description = "호스트 ID", example = "1")
    private Long hostId;

    @Schema(description = "호스트 닉네임", example = "홍길동")
    private String hostNickname;

    @Schema(description = "참여 인원 수", example = "4")
    private long participantCount;

    @Schema(description = "확정 위도")
    private Double confirmedLatitude;

    @Schema(description = "확정 경도")
    private Double confirmedLongitude;

    @Schema(description = "확정 장소명")
    private String confirmedPlaceName;

    @Schema(description = "생성 일시")
    private LocalDateTime createdAt;

    public static PromiseResponse from(Promise promise) {
        return from(promise, 0);
    }

    public static PromiseResponse from(Promise promise, long participantCount) {
        return PromiseResponse.builder()
                .id(promise.getId())
                .title(promise.getTitle())
                .description(promise.getDescription())
                .promiseDateTime(promise.getPromiseDateTime())
                .status(promise.getStatus())
                .inviteCode(promise.getInviteCode())
                .inviteExpiredAt(promise.getInviteExpiredAt())
                .maxParticipants(promise.getMaxParticipants())
                .hostId(promise.getHost().getId())
                .hostNickname(promise.getHost().getNickname())
                .participantCount(participantCount)
                .confirmedLatitude(promise.getConfirmedLatitude())
                .confirmedLongitude(promise.getConfirmedLongitude())
                .confirmedPlaceName(promise.getConfirmedPlaceName())
                .createdAt(promise.getCreatedAt())
                .build();
    }
}
