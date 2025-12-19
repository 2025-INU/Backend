package dev.promise4.GgUd.event;

import lombok.Builder;
import lombok.Getter;

/**
 * 참여자 참여 이벤트
 */
@Getter
public class ParticipantJoinedEvent extends PromiseEvent {

    public ParticipantJoinedEvent(Object source, Long promiseId, ParticipantInfo participant) {
        super(source, promiseId, PromiseEventType.PARTICIPANT_JOINED, participant);
    }

    @Getter
    @Builder
    public static class ParticipantInfo {
        private Long userId;
        private String nickname;
        private String profileImageUrl;
        private int currentParticipantCount;
    }
}
