# GgUd - 약속 조율 백엔드

## 한 줄 요약
참여자들 간의 중간지점을 계산하고, AI 기반 장소 추천 및 공유 비용 추적 기능을 제공하는 Spring Boot 백엔드

## 기술 스택
- **프레임워크**: Spring Boot 3.5.7 (Java 17)
- **데이터베이스**: PostgreSQL 15+
- **캐시**: Redis 7
- **컨테이너**: Docker Compose
- **배포**: AWS EC2 (t2.medium), Nginx
- **CI/CD**: GitHub Actions
- **모니터링**: AWS CloudWatch
- **외부 API**: 네이버 지도 API, AI 추천 서버, 카카오 OAuth2 & 메시징

## 핵심 기능
- **카카오 OAuth2 인증**: 카카오 계정을 통한 사용자 로그인
- **약속 조율**: 약속 생성 및 카카오톡을 통한 참여자 초대
- **중간지점 계산**: 참여자들의 출발지로부터 최적의 만남 지점 계산
- **AI 장소 추천**: 중간지점 근처 만남 장소에 대한 AI 기반 추천
- **실시간 위치 공유**: 진행 중인 약속의 참여자 위치 추적
- **비용 추적**: 정산을 위한 간단한 1인당 비용 기록

## 비즈니스 규칙
- **최대 참여 인원**: 약속당 10명
- **호스트 권한**: 약속 생성자(호스트)만 최종 장소 확정 가능
- **초대 만료**: 초대 링크는 24시간 동안 유효
- **추천 개수**: 약속당 3-5개의 중간지점 후보 제안
- **위치 공유 시간**: 약속 시작 5분 전부터 완료까지
- **데이터 보존**: 사용자 및 약속에 소프트 삭제 적용 (분석용 보존)

## 빠른 시작
```bash
# Docker Compose로 서비스 시작
docker-compose up -d

# 프로젝트 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# 테스트 실행
./gradlew test

# 애플리케이션 로그 확인
docker-compose logs -f app
```

자세한 설정: [runbooks/local-dev.md](runbooks/local-dev.md)

## 약속 라이프사이클
```
PLANNING → CONFIRMED → IN_PROGRESS → COMPLETED → CANCELLED
```

1. **PLANNING**: 호스트가 약속 생성, 참여자들이 참여하고 출발지 입력
2. **CONFIRMED**: 호스트가 AI 추천 중에서 장소를 선택하고 확정
3. **IN_PROGRESS**: 약속 시작 (5분 전), 실시간 위치 공유 활성화
4. **COMPLETED**: 약속 종료, 비용 추적 가능
5. **CANCELLED**: 호스트가 약속 취소

## 아키텍처

### 패키지 구조
```
dev.promise4.GgUd/
├── entity/              # JPA 엔티티 (데이터베이스 모델)
├── repository/          # 데이터 접근 계층
├── service/             # 비즈니스 로직
└── controller/          # REST API 엔드포인트
```

### 핵심 엔티티

**1. UsersEntity**
- 카카오 OAuth2 인증
- 소프트 삭제 지원 (`deletedAt`)
- 푸시 메시지용 알림 토큰

**2. MeetingsEntity**
- 약속 라이프사이클 상태
- 중간지점 캐싱 (`midpointLatitude`, `midpointLongitude`)
- 선택된 장소 정보
- 참여자와의 양방향 관계

**3. MeetingParticipantsEntity**
- 초대 상태: `PENDING` → `ACCEPTED`/`DECLINED`
- 출발지 저장
- 복합 유니크 키: `(meeting_id, user_id)`

**4. AiPlaceRecommendationsEntity**
- 임시 AI 추천 저장소
- 순위 및 점수 시스템
- AI 분석으로부터의 리뷰 요약

**5. ExpenseRecordsEntity**
- 1인당 비용 추적
- 간단한 정산 (복잡한 계산 없음)

### 엔티티 관계
```
Users 1:N MeetingParticipants N:1 Meetings
Meetings 1:N AiPlaceRecommendations
Meetings 1:N ExpenseRecordsEntity
```

### 외부 연동
- **AI 추천 서버**: 중간지점 좌표를 받아 순위가 매겨진 장소 제안 반환
- **네이버 지도 API**: `place_id`로 장소 정보 조회, 주소 및 좌표 데이터
- **카카오 OAuth2**: 사용자 인증
- **카카오톡 메시징**: 초대 링크 및 알림

## 주요 설계 결정사항

### 데이터 패턴
- **소프트 삭제**: 사용자와 약속은 데이터 보존 및 분석을 위해 하드 삭제 대신 `deletedAt` 타임스탬프 사용
- **연쇄 작업**: 약속 삭제 시 참여자, 추천, 비용을 자동으로 제거 (`CascadeType.ALL`과 `orphanRemoval`)
- **지연 로딩**: 모든 엔티티 관계는 N+1 쿼리 문제 방지를 위해 `FetchType.LAZY` 사용
- **엔티티 감사**: `@EntityListeners(AuditingEntityListener.class)`를 통한 자동 `createdAt` 및 `updatedAt` 타임스탬프

### 성능 최적화
- **중간지점 캐싱**: 계산된 중간지점을 약속 엔티티에 저장하여 비용이 많이 드는 재계산 방지
- **AI 추천 저장**: AI 서버 재요청을 피하기 위한 임시 캐싱
- **데이터베이스 인덱스**: 외래 키, 상태 필드, 검색 컬럼에 전략적 인덱스 적용
- **Redis 캐싱**: 향후 세션 및 데이터 캐싱 요구사항에 대비

### 보안 및 데이터 무결성
- **복합 유니크 제약조건**: `(meeting_id, user_id)`로 중복 참여자 방지
- **ON DELETE CASCADE**: 자동 정리로 참조 무결성 유지
- **소수점 정밀도**:
  - 좌표: 위도는 `DECIMAL(10, 8)`, 경도는 `DECIMAL(11, 8)`
  - 금액: 비용 금액은 `DECIMAL(12, 2)`
  - 점수: AI 점수는 `DECIMAL(5, 2)`

## 문서 맵

**시작하기**
- 현재 개발 단계: [_memory/current_state.md](_memory/current_state.md)
- 로컬 개발 설정: [runbooks/local-dev.md](runbooks/local-dev.md)

**구현 가이드**
- 단계 개요: [phases/phase-0-overview.md](phases/phase-0-overview.md)
- 단계 상세: [phases/phase-1-setup.md](phases/phase-1-setup.md)부터 [phases/phase-7-deployment.md](phases/phase-7-deployment.md)까지

**기술 참조**
- API 명세서: [api/backend-api.md](api/backend-api.md)
- 아키텍처 결정사항: [decisions/adr-index.md](decisions/adr-index.md)

**주요 결정사항**
- [ADR-0001](decisions/adr-0001-soft-delete.md): 소프트 삭제 전략
- [ADR-0002](decisions/adr-0002-cascade-delete.md): 연쇄 삭제 설정
- [ADR-0004](decisions/adr-0004-host-only-confirmation.md): 호스트 전용 약속 확정

## 프로젝트 상태
현재 단계 진행상황 및 다음 단계는 [_memory/current_state.md](_memory/current_state.md)를 참조하세요.
