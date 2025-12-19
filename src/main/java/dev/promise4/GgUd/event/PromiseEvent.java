package dev.promise4.GgUd.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 약속 이벤트 베이스 클래스
 */
@Getter
public abstract class PromiseEvent extends ApplicationEvent {

    private final Long promiseId;
    private final PromiseEventType eventType;
    private final Object payload;

    protected PromiseEvent(Object source, Long promiseId, PromiseEventType eventType, Object payload) {
        super(source);
        this.promiseId = promiseId;
        this.eventType = eventType;
        this.payload = payload;
    }

    public enum PromiseEventType {
        PARTICIPANT_JOINED, // 새 참여자 참여
        LOCATION_SUBMITTED, // 출발지 입력 완료
        ALL_LOCATIONS_SUBMITTED, // 모든 출발지 입력 완료
        MIDPOINT_CONFIRMED, // 중간지점 확정
        PROMISE_STARTED, // 약속 시작 (IN_PROGRESS)
        PROMISE_COMPLETED // 약속 완료
    }
}
