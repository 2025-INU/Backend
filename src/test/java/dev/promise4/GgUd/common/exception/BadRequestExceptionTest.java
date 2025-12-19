package dev.promise4.GgUd.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BadRequestException 테스트")
class BadRequestExceptionTest {

    @Test
    @DisplayName("ErrorCode로 예외를 생성한다")
    void whenCreateWithErrorCode_thenCreatesException() {
        // given
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;

        // when
        BadRequestException exception = new BadRequestException(errorCode);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(errorCode.getMessage());
    }

    @Test
    @DisplayName("ErrorCode와 커스텀 메시지로 예외를 생성한다")
    void whenCreateWithErrorCodeAndMessage_thenUsesCustomMessage() {
        // given
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        String customMessage = "Invalid email format";

        // when
        BadRequestException exception = new BadRequestException(errorCode, customMessage);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(customMessage);
    }

    @Test
    @DisplayName("메시지만으로 예외를 생성하면 기본 ErrorCode를 사용한다")
    void whenCreateWithMessage_thenUsesDefaultErrorCode() {
        // given
        String message = "Invalid request parameter";

        // when
        BadRequestException exception = new BadRequestException(message);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("BadRequestException은 BusinessException을 상속한다")
    void badRequestExceptionExtendsBusinessException() {
        // when
        BadRequestException exception = new BadRequestException("test");

        // then
        assertThat(exception).isInstanceOf(BusinessException.class);
    }
}
