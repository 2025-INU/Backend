# ADR-0010: Entity Auditing 패턴

**상태**: 승인됨
**날짜**: 2025-01-10
**결정자**: 개발팀

## 배경
대부분의 엔티티는 다음을 위해 생성 및 마지막 수정 시점을 추적해야 합니다:
- 디버깅 및 문제 해결
- 감사 추적 및 규정 준수
- 비즈니스 분석 및 리포팅
- 정렬 및 필터링 (최근 항목)

모든 서비스 메서드에서 이러한 타임스탬프를 수동으로 설정하는 것은:
- 오류가 발생하기 쉬움 (개발자가 잊어버림)
- 반복적인 보일러플레이트 코드
- 코드베이스 전반에 걸쳐 일관성 없음

## 결정
Spring Data JPA Auditing을 사용한 자동 타임스탬프 관리 구현:

- 모든 엔티티에 **@EntityListeners(AuditingEntityListener.class)**
- 생성 타임스탬프를 위한 **@CreatedDate** (불변)
- 업데이트 타임스탬프를 위한 **@LastModifiedDate** (자동 업데이트)
- 상속을 위한 **BaseTimeEntity** 추상 클래스
- 메인 애플리케이션 클래스에 **@EnableJpaAuditing**

이는 모든 엔티티에 걸쳐 자동적이고 일관된 타임스탬프 관리를 제공합니다.

## 결과

**긍정적**:
- **제로 보일러플레이트**: 서비스 레이어에서 수동 타임스탬프 설정 없음
- **일관성**: 모든 엔티티가 균일한 타임스탬프 동작을 가짐
- **신뢰성**: 타임스탬프 설정을 잊을 수 없으며, 항상 정확함
- **감사 추적**: 엔티티 변경의 내장 이력
- **성능**: JPA 라이프사이클 콜백이 효율적
- **개발자 경험**: 작성 및 유지보수할 코드 감소

**부정적**:
- **마법 같은 동작**: 타임스탬프가 자동으로 업데이트됨 (덜 명시적)
- **테스트 복잡성**: 테스트에서 자동 생성된 타임스탬프를 고려해야 함
- **시간대 문제**: 일관된 시간대 구성 보장 필요
- **클럭 스큐**: 분산 시스템에서 약간의 시간 차이가 있을 수 있음

## 구현

**Base Entity Class**:
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime updatedAt;

    // Getters only - no setters (managed by JPA)
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
```

**Entity Usage**:
```java
@Entity
@Table(name = "users")
public class UsersEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String kakaoId;
    private String nickname;

    // No need to manage createdAt/updatedAt manually
}
```

**Application Configuration**:
```java
@SpringBootApplication
@EnableJpaAuditing  // Enable auditing
public class GgUdApplication {
    public static void main(String[] args) {
        SpringApplication.run(GgUdApplication.class, args);
    }
}
```

**Time Zone Configuration**:
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          time_zone: UTC  # Store all timestamps in UTC
```

## 데이터베이스 스키마 규약
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    -- business fields...
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## 테스트 전략

**어설션에서 타임스탬프 무시**:
```java
@Test
void createUser_shouldSucceed() {
    User user = new User("kakao123", "홍길동");
    userRepository.save(user);

    assertThat(user.getId()).isNotNull();
    assertThat(user.getCreatedAt()).isNotNull();  // Verify populated
    assertThat(user.getUpdatedAt()).isNotNull();

    // Don't assert exact timestamp values (timing issues)
    assertThat(user.getCreatedAt())
        .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS));
}
```

**업데이트 감지**:
```java
@Test
void updateUser_shouldUpdateTimestamp() throws InterruptedException {
    User user = userRepository.save(new User("kakao123", "홍길동"));
    LocalDateTime originalUpdatedAt = user.getUpdatedAt();

    Thread.sleep(100);  // Ensure time passes

    user.setNickname("김철수");
    userRepository.save(user);

    assertThat(user.getUpdatedAt()).isAfter(originalUpdatedAt);
}
```

## 향후 확장

**추가 감사 필드** (향후 고려사항):
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditEntity extends BaseTimeEntity {

    @CreatedBy
    @Column(name = "created_by")
    private String createdBy;  // Who created this entity

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;  // Who last modified

    // Requires AuditorAware bean configuration
}
```

## 고려된 대안

**Manual Timestamps**: 기각됨 - 오류 발생 가능, 보일러플레이트, 일관성 없음
**Database Triggers**: 기각됨 - 데이터베이스의 로직, 테스트 어려움, 이식성 낮음
**@PrePersist/@PreUpdate**: 기각됨 - 여전히 각 엔티티에 수동 코드 필요
**Hibernate Interceptors**: 기각됨 - 더 복잡하고, Spring과 덜 관용적
**No Timestamps**: 기각됨 - 문제 해결 및 규정 준수를 위한 감사 추적 필수

## 관련 문서
- **ADR-0001**: Soft Delete Strategy - deletedAt 타임스탬프는 유사한 패턴을 따름
- **ADR-0009**: Spring Boot 3.5.7 - Spring Data JPA 3.x auditing 기능 사용
