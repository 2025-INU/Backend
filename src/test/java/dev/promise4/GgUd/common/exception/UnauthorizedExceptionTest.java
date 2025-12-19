package dev.promise4.GgUd.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UnauthorizedException 테스트")
class UnauthorizedExceptionTest {

    @Test
    @DisplayName("ErrorCode로 예외를 생성한다")
    void whenCreateWithErrorCode_thenCreatesException() {
        // given
        ErrorCode errorCode = ErrorCode.INVALID_TOKEN;

        // when
        UnauthorizedException exception = new UnauthorizedException(errorCode);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(errorCode.getMessage());
    }

    @Test
    @DisplayName("ErrorCode와 커스텀 메시지로 예외를 생성한다")
    void whenCreateWithErrorCodeAndMessage_thenUsesCustomMessage() {
        // given
        ErrorCode errorCode = ErrorCode.EXPIRED_TOKEN;
        String customMessage = "Token expired at 2024-01-01";

        // when
        UnauthorizedException exception = new UnauthorizedException(errorCode, customMessage);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(customMessage);
    }

    @Test
    @DisplayName("메시지만으로 예외를 생성하면 기본 ErrorCode를 사용한다")
    void whenCreateWithMessage_thenUsesDefaultErrorCode() {
        // given
        String message = "Authentication required";

        // when
        UnauthorizedException exception = new UnauthorizedException(message);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("UnauthorizedException은 BusinessException을 상속한다")
    void unauthorizedExceptionExtendsBusinessException() {
        // when
        UnauthorizedException exception = new UnauthorizedException("test");

        // then
        assertThat(exception).isInstanceOf(BusinessException.class);
    }
}
