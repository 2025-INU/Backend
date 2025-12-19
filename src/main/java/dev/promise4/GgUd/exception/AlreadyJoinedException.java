package dev.promise4.GgUd.exception;

/**
 * 이미 참여한 약속 예외
 */
public class AlreadyJoinedException extends RuntimeException {

    public AlreadyJoinedException() {
        super("이미 참여한 약속입니다");
    }
}
