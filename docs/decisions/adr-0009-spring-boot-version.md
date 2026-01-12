# ADR-0009: Spring Boot 3.5.7 선택

**상태**: 승인됨
**날짜**: 2025-01-10
**결정자**: 개발팀

## 배경
Spring Boot는 전체 애플리케이션의 기반을 제공합니다. 버전 선택은 다음에 영향을 미칩니다:
- 사용 가능한 기능 및 API
- Java 버전 요구사항
- 타사 라이브러리 호환성
- 장기 지원 및 보안 패치
- 향후 업그레이드를 위한 마이그레이션 노력

Spring Boot 3.x는 2.x에서 중요한 변경 사항이 있는 주요 버전입니다.

## 결정
**Java 17**과 함께 **Spring Boot 3.5.7** 사용:

- 최신 안정 Spring Boot 3.x 릴리스
- Java 17 필요 (LTS 버전)
- Jakarta EE 9+ 네임스페이스 (javax.* 대신 jakarta.*)
- 네이티브 컴파일 지원 (GraalVM)
- 관찰성 개선 (Micrometer, 분산 추적)

## 결과

**긍정적**:
- **최신 기능**: 최신 Spring 기능, 향상된 성능
- **Java 17 혜택**: Records, 패턴 매칭, 텍스트 블록, sealed 클래스
- **장기 지원**: Java 17 LTS는 2029년까지 지원됨
- **네이티브 컴파일**: GraalVM 네이티브 이미지를 위한 향후 옵션
- **더 나은 관찰성**: 내장 메트릭, 추적 및 상태 확인
- **보안**: 최신 보안 패치 및 취약점 수정
- **문서**: Spring Boot 3.x에 대한 풍부한 리소스
- **Jakarta EE**: 업계 표준 네임스페이스, 더 나은 IDE 지원

**부정적**:
- **마이그레이션 장벽**: Spring Boot 2.x로 쉽게 다운그레이드할 수 없음
- **타사 라이브러리**: 일부 라이브러리는 아직 Spring Boot 3.x를 지원하지 않을 수 있음
- **학습 곡선**: 팀이 Spring Boot 3.x 변경 사항을 학습해야 함
- **Breaking Changes**: jakarta.* 네임스페이스는 코드 업데이트 필요
- **새로운 기술**: 2.x에 비해 Stack Overflow 답변 적음

## 구현

**build.gradle**:
```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.7'
    id 'io.spring.dependency-management' version '1.1.4'
}

java {
    sourceCompatibility = '17'
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-websocket'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // PostgreSQL
    runtimeOnly 'org.postgresql:postgresql'

    // Other dependencies...
}
```

**Java 17 Features Used**:
```java
// Records for DTOs
public record CreateMeetingRequest(
    String title,
    String description,
    LocalDateTime meetingDateTime
) {}

// Text blocks for SQL
@Query("""
    SELECT m FROM MeetingsEntity m
    LEFT JOIN FETCH m.participants
    WHERE m.id = :id
    """)
Optional<MeetingsEntity> findByIdWithParticipants(@Param("id") Long id);

// Pattern matching
if (result instanceof Success success) {
    return success.getData();
}
```

## Jakarta EE 마이그레이션
모든 javax.* import를 jakarta.*로 변경:
- `javax.persistence.*` → `jakarta.persistence.*`
- `javax.validation.*` → `jakarta.validation.*`
- `javax.servlet.*` → `jakarta.servlet.*`

## 고려된 대안

**Spring Boot 2.7.x**: 기각됨 - 2023년 11월 OSS 지원 종료, 최신 기능 누락
**Spring Boot 3.0.x**: 기각됨 - 3.5.7이 더 안정적이고, 더 많은 기능
**Spring Boot 3.2.x**: 기각됨 - 3.5.7이 최신 개선 사항 포함
**Micronaut/Quarkus**: 기각됨 - Spring에 대한 팀 친숙도, 더 큰 생태계

## 버전 선택 기준
1. **안정성**: 3.5.7은 안정 릴리스, 최첨단 아님
2. **LTS Java**: Java 17 LTS는 2029년까지
3. **업계 채택**: Spring Boot 3.x 널리 채택됨
4. **기능 세트**: 필요한 모든 기능 사용 가능
5. **문서**: 우수한 공식 문서 및 커뮤니티 리소스

## 관련 문서
- **ADR-0008**: PostgreSQL + Redis Choice - 둘 다 Spring Boot 3.x를 훌륭하게 지원
- **ADR-0010**: Entity Auditing Pattern - Spring Data JPA 3.x 기능 사용
