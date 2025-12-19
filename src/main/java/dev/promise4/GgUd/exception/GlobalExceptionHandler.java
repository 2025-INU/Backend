package dev.promise4.GgUd.exception;

import dev.promise4.GgUd.common.exception.BadRequestException;
import dev.promise4.GgUd.common.exception.BusinessException;
import dev.promise4.GgUd.common.exception.ErrorCode;
import dev.promise4.GgUd.common.exception.NotFoundException;
import dev.promise4.GgUd.common.exception.UnauthorizedException;
import dev.promise4.GgUd.dto.ApiResponse;
import dev.promise4.GgUd.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {} - {}", e.getErrorCode().getCode(), e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse errorResponse = ErrorResponse.of(errorCode);
        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(ApiResponse.error(errorResponse));
    }

    /**
     * NotFoundException 처리
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleNotFoundException(NotFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse errorResponse = ErrorResponse.of(errorCode);
        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(ApiResponse.error(errorResponse));
    }

    /**
     * BadRequestException 처리
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBadRequestException(BadRequestException e) {
        log.warn("Bad request: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse errorResponse = ErrorResponse.of(errorCode);
        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(ApiResponse.error(errorResponse));
    }

    /**
     * UnauthorizedException 처리
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleUnauthorizedException(UnauthorizedException e) {
        log.warn("Unauthorized: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse errorResponse = ErrorResponse.of(errorCode);
        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(ApiResponse.error(errorResponse));
    }

    /**
     * Validation 예외 처리 (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getMessage());
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, message);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(errorResponse));
    }

    /**
     * BindException 처리
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBindException(BindException e) {
        log.warn("Bind exception: {}", e.getMessage());
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, message);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(errorResponse));
    }

    /**
     * 타입 미스매치 예외 처리
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMethodArgumentTypeMismatchException(
        MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INVALID_TYPE_VALUE);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(errorResponse));
    }

    /**
     * HTTP 메서드 미지원 예외 처리
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleHttpRequestMethodNotSupportedException(
        HttpRequestMethodNotSupportedException e) {
        log.warn("Method not supported: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED);
        return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(ApiResponse.error(errorResponse));
    }

    /**
     * 접근 거부 예외 처리
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.HANDLE_ACCESS_DENIED);
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(errorResponse));
    }

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Invalid argument: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(errorResponse));
    }

    /**
     * IllegalStateException 처리
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleIllegalStateException(IllegalStateException e) {
        log.warn("Invalid state: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getMessage());
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(errorResponse));
    }

    /**
     * 처리되지 않은 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleException(Exception e) {
        log.error("Unexpected error occurred", e);
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(errorResponse));
    }
}
