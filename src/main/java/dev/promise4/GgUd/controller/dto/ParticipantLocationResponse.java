package dev.promise4.GgUd.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 참여자 위치 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "참여자 위치 응답")
public class ParticipantLocationResponse {

    @Schema(description = "약속 ID", example = "1")
    private Long promiseId;

    @Schema(description = "참여자 위치 목록")
    private List<LocationUpdateMessage> locations;

    @Schema(description = "마지막 업데이트 시간")
    private LocalDateTime lastUpdated;
}
