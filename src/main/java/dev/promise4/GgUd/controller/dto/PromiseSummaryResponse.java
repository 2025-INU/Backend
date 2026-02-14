package dev.promise4.GgUd.controller.dto;

import dev.promise4.GgUd.entity.Promise;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 약속 요약 응답 DTO (제목, 일시, 주최자)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "약속 요약 응답")
public class PromiseSummaryResponse {

    @Schema(description = "약속 ID", example = "1")
    private Long id;

    @Schema(description = "약속 제목", example = "강남역 모임")
    private String title;

    @Schema(description = "약속 일시", example = "2024-12-20T18:00:00")
    private LocalDateTime promiseDateTime;

    @Schema(description = "호스트 ID", example = "1")
    private Long hostId;

    @Schema(description = "호스트 닉네임", example = "홍길동")
    private String hostNickname;

    public static PromiseSummaryResponse from(Promise promise) {
        return PromiseSummaryResponse.builder()
                .id(promise.getId())
                .title(promise.getTitle())
                .promiseDateTime(promise.getPromiseDateTime())
                .hostId(promise.getHost().getId())
                .hostNickname(promise.getHost().getNickname())
                .build();
    }
}
