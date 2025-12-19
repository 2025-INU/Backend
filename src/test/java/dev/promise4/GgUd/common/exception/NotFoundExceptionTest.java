package dev.promise4.GgUd.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotFoundException 테스트")
class NotFoundExceptionTest {

    @Test
    @DisplayName("ErrorCode로 예외를 생성한다")
    void whenCreateWithErrorCode_thenCreatesException() {
        // given
        ErrorCode errorCode = ErrorCode.USER_NOT_FOUND;

        // when
        NotFoundException exception = new NotFoundException(errorCode);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(errorCode.getMessage());
    }

    @Test
    @DisplayName("ErrorCode와 커스텀 메시지로 예외를 생성한다")
    void whenCreateWithErrorCodeAndMessage_thenUsesCustomMessage() {
        // given
        ErrorCode errorCode = ErrorCode.USER_NOT_FOUND;
        String customMessage = "User with ID 123 not found";

        // when
        NotFoundException exception = new NotFoundException(errorCode, customMessage);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(customMessage);
    }

    @Test
    @DisplayName("메시지만으로 예외를 생성하면 기본 ErrorCode를 사용한다")
    void whenCreateWithMessage_thenUsesDefaultErrorCode() {
        // given
        String message = "Resource not found";

        // when
        NotFoundException exception = new NotFoundException(message);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("NotFoundException은 BusinessException을 상속한다")
    void notFoundExceptionExtendsBusinessException() {
        // when
        NotFoundException exception = new NotFoundException("test");

        // then
        assertThat(exception).isInstanceOf(BusinessException.class);
    }
}
