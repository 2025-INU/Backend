package dev.promise4.GgUd.exception;

/**
 * 최대 참여자 수 초과 예외
 */
public class MaxParticipantsExceededException extends RuntimeException {

    public MaxParticipantsExceededException(int max) {
        super("최대 참여자 수(" + max + "명)를 초과했습니다");
    }
}
