package dev.promise4.GgUd.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투표 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "투표 요청")
public class VoteRequest {

    @NotNull(message = "역 ID는 필수입니다")
    @Schema(description = "투표할 역 ID", example = "1")
    private Long stationId;
}
