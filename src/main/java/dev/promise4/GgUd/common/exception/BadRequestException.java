package dev.promise4.GgUd.common.exception;

/**
 * 잘못된 요청일 때 발생하는 예외
 */
public class BadRequestException extends BusinessException {

    public BadRequestException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BadRequestException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public BadRequestException(String message) {
        super(ErrorCode.INVALID_INPUT_VALUE, message);
    }
}
