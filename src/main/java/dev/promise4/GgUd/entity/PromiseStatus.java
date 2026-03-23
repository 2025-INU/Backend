package dev.promise4.GgUd.entity;

/**
 * 약속 상태
 */
public enum PromiseStatus {

    /**
     * 약속 생성됨
     */
    CREATED,

    /**
     * 참여자 모집 중 (참여자들이 참여하면서 출발지도 입력)
     */
    RECRUITING,

    /**
     * 중간 지점 선택 중
     */
    SELECTING_MIDPOINT,

    /**
     * 장소 확정됨
     */
    CONFIRMED,

    /**
     * 약속 진행 중
     */
    IN_PROGRESS,

    /**
     * 약속 완료
     */
    COMPLETED,

    /**
     * 약속 취소
     */
    CANCELLED
}
