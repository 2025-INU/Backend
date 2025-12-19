package dev.promise4.GgUd.common.exception;

/**
 * 인증되지 않은 사용자가 접근할 때 발생하는 예외
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UnauthorizedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
