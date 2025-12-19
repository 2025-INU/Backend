package dev.promise4.GgUd.exception;

import dev.promise4.GgUd.common.exception.*;
import dev.promise4.GgUd.dto.ApiResponse;
import dev.promise4.GgUd.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("BusinessException을 처리한다")
    void handleBusinessException() {
        // given
        BusinessException exception = new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);

        // when
        ResponseEntity<ApiResponse<ErrorResponse>> response = exceptionHandler.handleBusinessException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
    }

    @Test
    @DisplayName("NotFoundException을 처리한다")
    void handleNotFoundException() {
        // given
        NotFoundException exception = new NotFoundException(ErrorCode.USER_NOT_FOUND);

        // when
        ResponseEntity<ApiResponse<ErrorResponse>> response = exceptionHandler.handleNotFoundException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData().getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("BadRequestException을 처리한다")
    void handleBadRequestException() {
        // given
        BadRequestException exception = new BadRequestException(ErrorCode.INVALID_INPUT_VALUE);

        // when
        ResponseEntity<ApiResponse<ErrorResponse>> response = exceptionHandler.handleBadRequestException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData().getCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
    }

    @Test
    @DisplayName("UnauthorizedException을 처리한다")
    void handleUnauthorizedException() {
        // given
        UnauthorizedException exception = new UnauthorizedException(ErrorCode.UNAUTHORIZED);

        // when
        ResponseEntity<ApiResponse<ErrorResponse>> response = exceptionHandler.handleUnauthorizedException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData().getCode()).isEqualTo(ErrorCode.UNAUTHORIZED.getCode());
    }

    @Test
    @DisplayName("MethodArgumentNotValidException을 처리한다")
    void handleMethodArgumentNotValidException() {
        // given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        FieldError fieldError = new FieldError("object", "field", "must not be null");
        when(exception.getBindingResult()).thenReturn(new BindException(new Object(), "object"));
        exception.getBindingResult().addError(fieldError);

        // when
        ResponseEntity<ApiResponse<ErrorResponse>> response = exceptionHandler.handleMethodArgumentNotValidException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData().getCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
    }

    @Test
    @DisplayName("BindException을 처리한다")
    void handleBindException() {
        // given
        BindException exception = new BindException(new Object(), "object");
        exception.addError(new FieldError("object", "field", "must not be null"));

        // when
        ResponseEntity<ApiResponse<ErrorResponse>> response = exceptionHandler.handleBindException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData().getCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException을 처리한다")
    void handleMethodArgumentTypeMismatchException() {
        // given
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);

        // when
        ResponseEntity<ApiResponse<ErrorResponse>> response = exceptionHandler.handleMethodArgumentTypeMismatchException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData().getCode()).isEqualTo(ErrorCode.INVALID_TYPE_VALUE.getCode());
    }

    @Test
    @DisplayName("HttpRequestMethodNotSupportedException을 처리한다")
    void handleHttpRequestMethodNotSupportedException() {
        // given
        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException("POST");

        // when
        ResponseEntity<ApiResponse<ErrorResponse>> response = exceptionHandler.handleHttpRequestMethodNotSupportedException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData().getCode()).isEqualTo(ErrorCode.METHOD_NOT_ALLOWED.getCode());
    }

    @Test
    @DisplayName("AccessDeniedException을 처리한다")
    void handleAccessDeniedException() {
        // given
        AccessDeniedException exception = new AccessDeniedException("Access denied");

        // when
        ResponseEntity<ApiResponse<ErrorResponse>> response = exceptionHandler.handleAccessDeniedException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData().getCode()).isEqualTo(ErrorCode.HANDLE_ACCESS_DENIED.getCode());
    }

    @Test
    @DisplayName("IllegalArgumentException을 처리한다")
    void handleIllegalArgumentException() {
        // given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

        // when
        ResponseEntity<ApiResponse<ErrorResponse>> response = exceptionHandler.handleIllegalArgumentException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData().getCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
    }

    @Test
    @DisplayName("IllegalStateException을 처리한다")
    void handleIllegalStateException() {
        // given
        IllegalStateException exception = new IllegalStateException("Invalid state");

        // when
        ResponseEntity<ApiResponse<ErrorResponse>> response = exceptionHandler.handleIllegalStateException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData().getCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
    }

    @Test
    @DisplayName("처리되지 않은 예외를 처리한다")
    void handleException() {
        // given
        Exception exception = new Exception("Unexpected error");

        // when
        ResponseEntity<ApiResponse<ErrorResponse>> response = exceptionHandler.handleException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData().getCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
    }

    @Test
    @DisplayName("커스텀 메시지를 가진 BusinessException을 처리한다")
    void handleBusinessExceptionWithCustomMessage() {
        // given
        String customMessage = "Custom error message";
        BusinessException exception = new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, customMessage);

        // when
        ResponseEntity<ApiResponse<ErrorResponse>> response = exceptionHandler.handleBusinessException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData()).isNotNull();
    }
}
