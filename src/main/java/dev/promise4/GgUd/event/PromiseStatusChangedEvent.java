package dev.promise4.GgUd.event;

import dev.promise4.GgUd.entity.PromiseStatus;
import lombok.Builder;
import lombok.Getter;

/**
 * 약속 상태 변경 이벤트
 */
@Getter
public class PromiseStatusChangedEvent extends PromiseEvent {

    public PromiseStatusChangedEvent(Object source, Long promiseId, StatusChangeInfo statusChange) {
        super(source, promiseId, getEventType(statusChange.getNewStatus()), statusChange);
    }

    private static PromiseEventType getEventType(PromiseStatus status) {
        return switch (status) {
            case SELECTING_MIDPOINT -> PromiseEventType.ALL_LOCATIONS_SUBMITTED;
            case CONFIRMED -> PromiseEventType.MIDPOINT_CONFIRMED;
            case IN_PROGRESS -> PromiseEventType.PROMISE_STARTED;
            case COMPLETED -> PromiseEventType.PROMISE_COMPLETED;
            default -> PromiseEventType.LOCATION_SUBMITTED;
        };
    }

    @Getter
    @Builder
    public static class StatusChangeInfo {
        private PromiseStatus previousStatus;
        private PromiseStatus newStatus;
        private String confirmedPlaceName; // 확정 시 장소명
    }
}
