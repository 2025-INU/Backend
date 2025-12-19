package dev.promise4.GgUd.dto;

import dev.promise4.GgUd.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorResponse 테스트")
class ErrorResponseTest {

    @Test
    @DisplayName("ErrorCode로 ErrorResponse를 생성한다")
    void createErrorResponseFromErrorCode() {
        // given
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        // when
        ErrorResponse response = ErrorResponse.of(errorCode);

        // then
        assertThat(response.getCode()).isEqualTo(errorCode.getCode());
        assertThat(response.getMessage()).isEqualTo(errorCode.getMessage());
        assertThat(response.getTimestamp()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("ErrorCode와 커스텀 메시지로 ErrorResponse를 생성한다")
    void createErrorResponseFromErrorCodeWithCustomMessage() {
        // given
        ErrorCode errorCode = ErrorCode.USER_NOT_FOUND;
        String customMessage = "User with ID 123 not found";

        // when
        ErrorResponse response = ErrorResponse.of(errorCode, customMessage);

        // then
        assertThat(response.getCode()).isEqualTo(errorCode.getCode());
        assertThat(response.getMessage()).isEqualTo(customMessage);
        assertThat(response.getTimestamp()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("다양한 ErrorCode로 ErrorResponse를 생성한다")
    void createErrorResponseWithVariousErrorCodes() {
        // when & then
        ErrorResponse notFoundResponse = ErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(notFoundResponse.getCode()).isEqualTo("R001");
        assertThat(notFoundResponse.getMessage()).isEqualTo("Resource not found");

        ErrorResponse unauthorizedResponse = ErrorResponse.of(ErrorCode.UNAUTHORIZED);
        assertThat(unauthorizedResponse.getCode()).isEqualTo("A001");
        assertThat(unauthorizedResponse.getMessage()).isEqualTo("Unauthorized");

        ErrorResponse invalidInputResponse = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE);
        assertThat(invalidInputResponse.getCode()).isEqualTo("C002");
        assertThat(invalidInputResponse.getMessage()).isEqualTo("Invalid input value");
    }

    @Test
    @DisplayName("timestamp가 응답 생성 시점을 반영한다")
    void timestampReflectsCreationTime() throws InterruptedException {
        // given
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        LocalDateTime beforeCreation = LocalDateTime.now();
        Thread.sleep(10);

        // when
        ErrorResponse response = ErrorResponse.of(errorCode);
        Thread.sleep(10);
        LocalDateTime afterCreation = LocalDateTime.now();

        // then
        assertThat(response.getTimestamp()).isAfter(beforeCreation);
        assertThat(response.getTimestamp()).isBefore(afterCreation);
    }

    @Test
    @DisplayName("직접 생성자로 ErrorResponse를 생성한다")
    void createErrorResponseWithConstructor() {
        // given
        String code = "TEST001";
        String message = "Test error message";
        LocalDateTime timestamp = LocalDateTime.now();

        // when
        ErrorResponse response = new ErrorResponse(code, message, timestamp);

        // then
        assertThat(response.getCode()).isEqualTo(code);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    @DisplayName("no-args 생성자로 ErrorResponse를 생성한다")
    void createErrorResponseWithNoArgsConstructor() {
        // when
        ErrorResponse response = new ErrorResponse();

        // then
        assertThat(response).isNotNull();
    }
}
