package dev.promise4.GgUd.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 약속 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "약속 생성 요청")
public class CreatePromiseRequest {

    @NotBlank(message = "약속 제목은 필수입니다")
    @Size(max = 50, message = "약속 제목은 50자 이하여야 합니다")
    @Schema(description = "약속 제목", example = "강남역 모임")
    private String title;

    @Size(max = 200, message = "약속 설명은 200자 이하여야 합니다")
    @Schema(description = "약속 설명", example = "친구들과 저녁 식사")
    private String description;

    @NotNull(message = "약속 일시는 필수입니다")
    @Future(message = "약속 일시는 미래여야 합니다")
    @Schema(description = "약속 일시", example = "2024-12-20T18:00:00")
    private LocalDateTime promiseDateTime;
}
