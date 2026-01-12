# ADR-0008: PostgreSQL + Redis 기술 스택 선택

**상태**: 승인됨
**날짜**: 2025-01-10
**결정자**: 개발팀

## 배경
애플리케이션은 영구 저장을 위한 데이터베이스와 성능을 위한 캐싱 레이어가 필요합니다. 기술 선택은 다음에 영향을 미칩니다:
- 데이터 일관성 및 ACID 준수
- 쿼리 성능 및 확장성
- 개발 복잡성 및 팀 친숙도
- 운영 오버헤드 및 인프라 비용
- 향후 기능 요구사항

## 결정
주 데이터베이스로 **PostgreSQL 15+**, 캐싱으로 **Redis 7** 사용:

**PostgreSQL**:
- 모든 엔티티의 주 데이터 저장소
- ACID 준수 트랜잭션
- 외래 키가 있는 관계형 데이터
- 유연한 데이터를 위한 JSON 지원
- 향후 지리공간 기능을 위한 PostGIS

**Redis**:
- 세션 관리 (향후)
- 자주 액세스되는 데이터 캐싱
- 실시간 위치 추적 (인메모리 저장)
- 속도 제한 카운터
- 메시지 큐잉 (필요한 경우)

## 결과

**긍정적**:
- **PostgreSQL**:
  - 중요한 모임 데이터에 대한 강력한 ACID 보장
  - 지리공간 쿼리에 대한 뛰어난 지원 (PostGIS)
  - 성숙한 생태계, 광범위한 문서
  - 훌륭한 Spring Data JPA 통합
  - 유연한 스키마를 위한 JSON 컬럼
  - 전체 텍스트 검색 기능

- **Redis**:
  - 캐시 읽기를 위한 밀리초 미만 지연 시간
  - 실시간 위치 추적에 완벽
  - WebSocket 메시징을 위한 Pub/Sub
  - 간단한 키-값 작업
  - 자동 정리를 위한 TTL (time-to-live)

- **결합**:
  - 최고의 조합: 관계형 + 캐시
  - 명확한 관심사 분리
  - 표준 기술 스택, 쉬운 채용

**부정적**:
- **운영 복잡성**: 두 개의 데이터베이스 관리, 모니터링, 백업
- **인프라 비용**: 두 개의 서비스 실행, 더 많은 리소스
- **학습 곡선**: 팀이 두 시스템 모두 이해해야 함
- **네트워크 지연**: 두 시스템에 분산된 데이터
- **일관성 문제**: 캐시 무효화 복잡성

## 구현

**Docker Compose Configuration**:
```yaml
services:
  db:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: ggud_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes
```

**Spring Configuration**:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ggud_db
    username: postgres
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    database-platform: org.hibernate.dialect.PostgreSQLDialect

  data:
    redis:
      host: localhost
      port: 6379
  cache:
    type: redis
```

## 사용 사례 할당

**PostgreSQL** (영구, 중요 데이터):
- 사용자 및 인증
- 모임 및 참가자
- 출발 위치
- AI 추천 (임시)
- 비용 기록
- 감사 로그

**Redis** (임시, 성능 중요):
- 실시간 참가자 위치 (모임 5분 전 → 모임 후)
- 세션 토큰 (향후)
- API 속도 제한 카운터
- 자주 액세스되는 모임 요약
- WebSocket pub/sub 채널

## 고려된 대안

**MySQL**: 기각됨 - PostgreSQL이 더 나은 JSON 지원 및 지리공간 기능 제공
**MongoDB**: 기각됨 - 관계형 데이터 구조가 SQL에 더 적합, ACID 보장 부족
**PostgreSQL Only** (Redis 없음): 기각됨 - 실시간 위치 추적에 밀리초 미만 지연 시간 필요
**Redis Only**: 기각됨 - 영구 관계형 데이터에 적합하지 않음
**PostgreSQL + Memcached**: 기각됨 - Redis가 더 많은 기능 제공 (pub/sub, 데이터 구조)

## 마이그레이션 경로
- **Phase 1**: PostgreSQL만 (현재 구현)
- **Phase 2**: 위치 추적을 위한 Redis 추가 (Phase 5)
- **Phase 3**: 캐싱 및 세션을 위한 Redis 확장 (향후 최적화)

## 관련 문서
- **ADR-0006**: Midpoint Caching - Redis 대신 PostgreSQL 컬럼 사용
- **ADR-0007**: AI Recommendation Storage - PostgreSQL에 임시 저장
