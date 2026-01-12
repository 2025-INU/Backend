# ADR-0003: Entity Lazy Loading 패턴

**상태**: 승인됨
**날짜**: 2025-01-10
**결정자**: 개발팀

## 배경
JPA 관계는 EAGER 또는 LAZY 로딩 전략을 사용할 수 있습니다. EAGER 로딩은 즉시 관련 엔티티를 가져오는 반면, LAZY는 필요할 때 로드합니다. 이 선택은 쿼리 성능, 데이터베이스 부하 및 N+1 쿼리 문제에 영향을 미칩니다. 신중한 구성 없이는 불필요한 데이터 가져오기로 인해 애플리케이션 성능 저하가 발생할 수 있습니다.

## 결정
모든 엔티티 관계에 대해 `FetchType.LAZY` 사용:

- **모든 @OneToMany 관계**: 기본적으로 Lazy이지만, 명시적으로 구성
- **모든 @ManyToOne 관계**: Lazy (JPA 기본 EAGER 오버라이드)
- **모든 @ManyToMany 관계**: 기본적으로 Lazy이지만, 명시적으로 구성

필요할 때 JOIN FETCH 쿼리 또는 entity graph를 사용하여 관련 엔티티를 명시적으로 로드합니다.

## 결과

**긍정적**:
- **성능 최적화**: 실제로 필요할 때만 데이터 로드
- **데이터베이스 부하 감소**: 쿼리 수 감소, 결과 집합 크기 감소
- **N+1 방지**: 명시적 fetch 전략 강제, 성능 문제를 가시화
- **예측 가능한 동작**: 개발자가 데이터 액세스 패턴에 대해 생각해야 함
- **메모리 효율성**: 사용하지 않는 관계를 메모리에 로드하지 않음

**부정적**:
- **LazyInitializationException 위험**: 트랜잭션 외부에서 lazy 컬렉션 액세스 시 예외 발생
- **더 장황한 쿼리**: 명시적 JOIN FETCH 또는 entity graph 필요
- **학습 곡선**: 개발자가 lazy loading 개념을 이해해야 함
- **테스트 복잡성**: 테스트에 트랜잭션 관리 또는 명시적 fetching 필요

## 구현

**Entity Relationships**:
```java
@Entity
public class MeetingsEntity {
    // OneToMany: Lazy by default, but explicit is better
    @OneToMany(
        mappedBy = "meeting",
        fetch = FetchType.LAZY
    )
    private List<MeetingParticipantsEntity> participants;

    // ManyToOne: Override default EAGER
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private UsersEntity host;
}
```

**Explicit Fetching in Repository**:
```java
@Repository
public interface MeetingRepository extends JpaRepository<MeetingsEntity, Long> {
    @Query("SELECT m FROM MeetingsEntity m " +
           "LEFT JOIN FETCH m.participants " +
           "WHERE m.id = :id")
    Optional<MeetingsEntity> findByIdWithParticipants(@Param("id") Long id);

    @Query("SELECT m FROM MeetingsEntity m " +
           "LEFT JOIN FETCH m.participants p " +
           "LEFT JOIN FETCH m.recommendations " +
           "WHERE m.id = :id")
    Optional<MeetingsEntity> findByIdWithDetails(@Param("id") Long id);
}
```

**Service Layer Pattern**:
```java
@Service
@Transactional(readOnly = true)
public class MeetingService {
    public MeetingDetailResponse getMeetingDetails(Long id) {
        // Explicitly fetch what we need
        Meeting meeting = meetingRepository
            .findByIdWithParticipants(id)
            .orElseThrow(() -> new NotFoundException("Meeting not found"));

        // All data loaded within transaction
        return toDetailResponse(meeting);
    }
}
```

## 고려된 대안

**EAGER Loading**: 기각됨 - N+1 문제 발생, 불필요한 데이터 로드, 성능 문제
**Mixed Strategy**: 기각됨 - 일관성 없음, 동작 예측 어려움, 유지보수 악몽
**Default JPA Behavior**: 기각됨 - OneToMany는 lazy이지만 ManyToOne은 eager로 불일치 발생

## 관련 문서
- **ADR-0002**: Cascade Delete Configuration - lazy loading이 cascade 중 불필요한 쿼리를 방지
