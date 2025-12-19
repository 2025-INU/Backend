package dev.promise4.GgUd.exception;

/**
 * 초대 코드 만료 예외
 */
public class InviteExpiredException extends RuntimeException {

    public InviteExpiredException() {
        super("초대 코드가 만료되었습니다");
    }
}
