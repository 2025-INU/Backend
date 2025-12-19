package dev.promise4.GgUd.event;

import dev.promise4.GgUd.controller.dto.PromiseNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 약속 이벤트 리스너 - WebSocket으로 브로드캐스트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromiseEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 참여자 참여 이벤트 처리
     */
    @Async
    @EventListener
    public void handleParticipantJoined(ParticipantJoinedEvent event) {
        PromiseNotification notification = PromiseNotification.builder()
                .type(event.getEventType().name())
                .promiseId(event.getPromiseId())
                .message("새로운 참여자가 참여했습니다")
                .payload(event.getPayload())
                .build();

        sendNotification(event.getPromiseId(), notification);
    }

    /**
     * 상태 변경 이벤트 처리
     */
    @Async
    @EventListener
    public void handleStatusChanged(PromiseStatusChangedEvent event) {
        PromiseStatusChangedEvent.StatusChangeInfo info = (PromiseStatusChangedEvent.StatusChangeInfo) event
                .getPayload();

        String message = switch (event.getEventType()) {
            case ALL_LOCATIONS_SUBMITTED -> "모든 참여자가 출발지를 입력했습니다";
            case MIDPOINT_CONFIRMED -> "중간 지점이 확정되었습니다: " + info.getConfirmedPlaceName();
            case PROMISE_STARTED -> "약속이 시작되었습니다";
            case PROMISE_COMPLETED -> "약속이 완료되었습니다";
            default -> "약속 상태가 변경되었습니다";
        };

        PromiseNotification notification = PromiseNotification.builder()
                .type(event.getEventType().name())
                .promiseId(event.getPromiseId())
                .message(message)
                .payload(event.getPayload())
                .build();

        sendNotification(event.getPromiseId(), notification);
    }

    /**
     * WebSocket으로 알림 전송
     */
    private void sendNotification(Long promiseId, PromiseNotification notification) {
        String destination = "/topic/promises/" + promiseId + "/status";
        messagingTemplate.convertAndSend(destination, notification);
        log.info("Notification sent to {}: {}", destination, notification.getType());
    }
}
