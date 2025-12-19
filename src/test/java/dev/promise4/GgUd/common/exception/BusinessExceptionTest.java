package dev.promise4.GgUd.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BusinessException 테스트")
class BusinessExceptionTest {

    @Test
    @DisplayName("ErrorCode로 예외를 생성하면 ErrorCode의 메시지를 사용한다")
    void whenCreateWithErrorCode_thenUsesErrorCodeMessage() {
        // given
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        // when
        BusinessException exception = new BusinessException(errorCode);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(errorCode.getMessage());
    }

    @Test
    @DisplayName("ErrorCode와 커스텀 메시지로 예외를 생성하면 커스텀 메시지를 사용한다")
    void whenCreateWithErrorCodeAndMessage_thenUsesCustomMessage() {
        // given
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        String customMessage = "Custom error message";

        // when
        BusinessException exception = new BusinessException(errorCode, customMessage);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(customMessage);
    }

    @Test
    @DisplayName("ErrorCode와 원인으로 예외를 생성하면 원인을 포함한다")
    void whenCreateWithErrorCodeAndCause_thenIncludesCause() {
        // given
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        RuntimeException cause = new RuntimeException("Original cause");

        // when
        BusinessException exception = new BusinessException(errorCode, cause);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(errorCode.getMessage());
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}
