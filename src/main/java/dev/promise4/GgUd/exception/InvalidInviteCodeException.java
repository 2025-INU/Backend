package dev.promise4.GgUd.exception;

/**
 * 유효하지 않은 초대 코드 예외
 */
public class InvalidInviteCodeException extends RuntimeException {

    public InvalidInviteCodeException(String inviteCode) {
        super("유효하지 않은 초대 코드입니다: " + inviteCode);
    }
}
