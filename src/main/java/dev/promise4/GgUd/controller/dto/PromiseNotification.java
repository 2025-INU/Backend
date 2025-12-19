package dev.promise4.GgUd.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 약속 알림 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "약속 알림")
public class PromiseNotification {

    @Schema(description = "알림 타입", example = "PARTICIPANT_JOINED")
    private String type;

    @Schema(description = "약속 ID", example = "1")
    private Long promiseId;

    @Schema(description = "알림 메시지", example = "새로운 참여자가 참여했습니다")
    private String message;

    @Schema(description = "추가 데이터")
    private Object payload;

    @Schema(description = "알림 시간")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
