# ADR-0002: Cascade Delete 설정

**상태**: 승인됨
**날짜**: 2025-01-10
**결정자**: 개발팀

## 배경
모임이 삭제될 때 (soft delete), 시스템은 관련 엔티티들(참가자, AI 추천, 비용 기록)을 어떻게 처리할지 결정해야 합니다. 옵션으로는 수동 정리, orphan removal, cascade delete가 있습니다. 이 결정은 데이터 일관성, 데이터베이스 무결성 및 정리 복잡도에 영향을 미칩니다.

## 결정
Meeting 엔티티 관계에 대해 orphan removal과 함께 cascade delete 구현:

- **Meeting → MeetingParticipants**: `CascadeType.ALL` with `orphanRemoval = true`
- **Meeting → AiPlaceRecommendations**: `CascadeType.ALL` with `orphanRemoval = true`
- **Meeting → ExpenseRecords**: `CascadeType.ALL` with `orphanRemoval = true`
- **Database**: `ON DELETE CASCADE` 외래 키 제약조건

모임이 soft-deleted(deletedAt 설정)되면 모든 관련 엔티티가 자동으로 제거됩니다. 이는 참조 무결성을 유지하고 고아 레코드를 방지합니다.

## 결과

**긍정적**:
- **자동 정리**: 수동 정리 코드 불필요, JPA가 처리
- **데이터 일관성**: 고아 참가자/추천/비용 방지
- **간단한 코드**: 서비스 레이어에서 관련 엔티티를 수동으로 삭제할 필요 없음
- **데이터베이스 무결성**: 외래 키 제약조건이 DB 레벨에서 일관성 강제
- **트랜잭션 안전성**: 모든 삭제가 단일 트랜잭션에서 발생

**부정적**:
- **Cascading 위험**: 실수로 모임 삭제 시 모든 관련 데이터 제거
- **성능 영향**: 많은 참가자가 있는 대규모 모임은 삭제가 느릴 수 있음
- **부분 정리 불가**: 일부 관련 엔티티를 선택적으로 유지할 수 없음
- **감사 추적 손실**: 관련 엔티티 이력 제거 (모임의 soft delete로 완화됨)

## 구현

**Entity Annotation**:
```java
@Entity
@Table(name = "meetings")
public class MeetingsEntity {
    @OneToMany(
        mappedBy = "meeting",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<MeetingParticipantsEntity> participants;

    @OneToMany(
        mappedBy = "meeting",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<AiPlaceRecommendationsEntity> recommendations;

    @OneToMany(
        mappedBy = "meeting",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<ExpenseRecordsEntity> expenses;
}
```

**Database Schema**:
```sql
ALTER TABLE meeting_participants
ADD CONSTRAINT fk_meeting
FOREIGN KEY (meeting_id) REFERENCES meetings(id)
ON DELETE CASCADE;

ALTER TABLE ai_place_recommendations
ADD CONSTRAINT fk_meeting
FOREIGN KEY (meeting_id) REFERENCES meetings(id)
ON DELETE CASCADE;

ALTER TABLE expense_records
ADD CONSTRAINT fk_meeting
FOREIGN KEY (meeting_id) REFERENCES meetings(id)
ON DELETE CASCADE;
```

## 고려된 대안

**Manual Cleanup**: 기각됨 - 서비스 레이어 코드 필요, 버그 발생 가능, 불일치
**Orphan Records**: 기각됨 - 데이터베이스 오염, 참조 무결성 파괴
**Soft Delete Everything**: 기각됨 - 중요하지 않은 엔티티에 대한 불필요한 복잡성

## 관련 문서
- **ADR-0001**: Soft Delete Strategy - 모임은 soft-deleted되지만 관련 엔티티는 hard-deleted됨
- **ADR-0003**: Lazy Loading - cascade delete가 불필요한 쿼리를 트리거하는 것을 방지
