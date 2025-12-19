package dev.promise4.GgUd.dto;

import dev.promise4.GgUd.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 에러 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String code;
    private String message;
    private LocalDateTime timestamp;

    /**
     * ErrorCode로부터 ErrorResponse 생성
     */
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(
            errorCode.getCode(),
            errorCode.getMessage(),
            LocalDateTime.now()
        );
    }

    /**
     * ErrorCode와 커스텀 메시지로부터 ErrorResponse 생성
     */
    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(
            errorCode.getCode(),
            message,
            LocalDateTime.now()
        );
    }
}
