package dev.promise4.GgUd.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 정의
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "Internal server error"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C002", "Invalid input value"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C003", "Method not allowed"),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C004", "Invalid type value"),
    HANDLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "C005", "Access denied"),

    // Resource
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "Resource not found"),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "Unauthorized"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "Invalid token"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "Expired token"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "User not found"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "Duplicate email"),

    // Meeting
    MEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "Meeting not found"),
    MEETING_ALREADY_STARTED(HttpStatus.BAD_REQUEST, "M002", "Meeting already started"),
    MEETING_FULL(HttpStatus.BAD_REQUEST, "M003", "Meeting is full"),
    INVALID_MEETING_TIME(HttpStatus.BAD_REQUEST, "M004", "Invalid meeting time"),

    // Settlement
    SETTLEMENT_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "S001", "이미 정산이 완료되었습니다"),
    SETTLEMENT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "S002", "정산은 약속이 진행 중이거나 완료된 상태에서만 가능합니다"),
    EXPENSE_AMOUNT_INVALID(HttpStatus.BAD_REQUEST, "S003", "결제 금액은 0원 이상이어야 합니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
