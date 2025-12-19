package dev.promise4.GgUd.event;

import dev.promise4.GgUd.entity.PromiseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 약속 이벤트 발행자
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromiseEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 참여자 참여 이벤트 발행
     */
    public void publishParticipantJoined(Long promiseId, Long userId, String nickname,
            String profileImageUrl, int currentCount) {
        ParticipantJoinedEvent.ParticipantInfo info = ParticipantJoinedEvent.ParticipantInfo.builder()
                .userId(userId)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .currentParticipantCount(currentCount)
                .build();

        eventPublisher.publishEvent(new ParticipantJoinedEvent(this, promiseId, info));
        log.debug("Published ParticipantJoinedEvent: promiseId={}, userId={}", promiseId, userId);
    }

    /**
     * 상태 변경 이벤트 발행
     */
    public void publishStatusChanged(Long promiseId, PromiseStatus previousStatus,
            PromiseStatus newStatus, String confirmedPlaceName) {
        PromiseStatusChangedEvent.StatusChangeInfo info = PromiseStatusChangedEvent.StatusChangeInfo.builder()
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .confirmedPlaceName(confirmedPlaceName)
                .build();

        eventPublisher.publishEvent(new PromiseStatusChangedEvent(this, promiseId, info));
        log.debug("Published PromiseStatusChangedEvent: promiseId={}, {} -> {}",
                promiseId, previousStatus, newStatus);
    }
}
