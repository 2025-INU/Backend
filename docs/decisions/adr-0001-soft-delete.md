# ADR-0001: Soft Delete 전략

**상태**: 승인됨
**날짜**: 2025-01-10
**결정자**: 개발팀

## 배경
사용자와 모임은 삭제 가능해야 하지만, 분석, 규정 준수 및 잠재적인 데이터 복구를 위해 데이터 이력을 보존해야 합니다. Hard delete는 데이터를 영구적으로 제거하고 참조 무결성을 파괴합니다. 데이터 이력을 유지하면서 레코드를 활성 사용에서 논리적으로 제거하는 솔루션이 필요합니다.

## 결정
Users 및 Meetings 엔티티에 대해 `deletedAt` 타임스탬프 컬럼을 사용한 soft delete 패턴 구현:

- 실제 DELETE 실행 대신 `deletedAt`을 현재 타임스탬프로 설정
- JPA `@Where(clause = "deleted_at IS NULL")`를 사용하여 쿼리에서 삭제된 레코드 필터링
- 모든 외래 키 관계 및 이력 데이터 보존
- 삭제된 레코드는 데이터베이스에 남아있지만 애플리케이션 쿼리에서는 숨겨짐

## 결과

**긍정적**:
- 분석 및 감사 추적을 위한 데이터 이력 보존
- 모든 관계에서 참조 무결성 유지
- 쉬운 데이터 복구 - `deleted_at`을 NULL로 설정하기만 하면 됨
- 데이터 보존 요구사항 준수
- 삭제된 엔티티에 대한 이력 리포팅 가능

**부정적**:
- 시간이 지남에 따라 데이터베이스 저장 공간 요구사항 증가
- 쿼리에 `deleted_at IS NULL` 필터 필요 (JPA @Where로 처리됨)
- 고유 제약조건은 soft-deleted 중복을 허용하기 위한 특별한 처리 필요
- 매우 오래된 soft-deleted 레코드에 대해 주기적인 정리가 필요할 수 있음

## 구현

**Entity Annotation**:
```java
@Entity
@Table(name = "users")
@Where(clause = "deleted_at IS NULL")
@SQLDelete(sql = "UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class UsersEntity {
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // other fields...
}
```

**Service Layer**:
```java
@Service
public class UserService {
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        // JPA will use SQLDelete annotation
        userRepository.delete(user);
        // Sets deleted_at automatically
    }
}
```

## 고려된 대안

**Hard Delete**: 기각됨 - 데이터 손실을 일으키고 분석을 파괴함
**Archive Table**: 기각됨 - 테이블 중복으로 복잡성 증가, 활성/보관 데이터 간 쿼리 어려움
**Status Flag**: 기각됨 - 타임스탬프보다 직관적이지 않으며, 삭제 시점을 추적하지 않음

## 관련 문서
- **ADR-0002**: Cascade Delete Configuration - soft delete가 관련 엔티티로 전파되는 방법 정의
