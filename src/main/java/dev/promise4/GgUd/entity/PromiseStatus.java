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
     * 중간지점 확정됨 (지하철역 선택 완료, AI 장소 추천 진행 중)
     */
    MIDPOINT_CONFIRMED,

    /**
     * 최종 약속 장소 확정됨
     */
    PLACE_CONFIRMED,

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
