# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [기술 스택](#2-기술-스택)
3. [CLAUDE.md 설정](#3-claudemd-설정)
4. [Phase 1: 프로젝트 초기 설정](#phase-1-프로젝트-초기-설정)
5. [Phase 2: 인증 시스템 구현](#phase-2-인증-시스템-구현)
6. [Phase 3: 약속 핵심 기능 구현](#phase-3-약속-핵심-기능-구현)
7. [Phase 4: 중간지점 추천 시스템](#phase-4-중간지점-추천-시스템)
8. [Phase 5: 실시간 기능 구현](#phase-5-실시간-기능-구현)
9. [Phase 6: 카카오 API 통합](#phase-6-카카오-api-통합)
10. [API 명세](#api-명세)
11. [데이터베이스 스키마](#데이터베이스-스키마)

---

## 1. 프로젝트 개요

### 핵심 기능
- 카카오 OAuth2 로그인
- 약속 생성 및 카카오톡 초대 링크 공유
- 참여자 출발지 입력 및 관리
- 지하철역 기반 중간지점 추천 (단순 평균 좌표)
- 약속 장소 투표 및 확정
- 카카오 API 기반 실시간 길찾기
- 약속 시작 5분 전부터 실시간 위치 공유

### 비즈니스 규칙
- 최대 참여자 수: 10명
- 초대 링크 유효기간: 24시간
- 중간지점 후보: 3~5개
- 약속 확정 권한: 생성자만 가능
- 장소 선호도 표시: 모든 참여자 가능

### 약속 상태 흐름
```
CREATED → RECRUITING → WAITING_LOCATIONS → SELECTING_MIDPOINT → CONFIRMED → IN_PROGRESS → COMPLETED
  생성됨     참여자모집중    출발지입력대기       중간지점선택         장소확정       진행중        완료
```

---

## 2. 기술 스택

| 구분 | 기술 |
|------|------|
| Framework | Spring Boot 3.x |
| Language | Java 17+ |
| Database | PostgreSQL 15+ |
| Container | Docker, Docker Compose |
| Authentication | OAuth2 (Kakao) |
| Real-time | WebSocket + STOMP (SockJS fallback) |
| External API | Kakao Maps API, Kakao Login API, Kakao Message API |
| Build Tool | Gradle |
| Documentation | Springdoc OpenAPI (Swagger) |

---

## 3. CLAUDE.md 설정

프로젝트 루트에 `CLAUDE.md` 파일을 생성하여 Claude Code가 프로젝트 컨텍스트를 이해할 수 있도록 합니다.

```markdown
# Promise App Backend - CLAUDE.md

## 프로젝트 개요
카카오톡 기반 약속 관리 앱의 Spring Boot 백엔드 서버

## Bash 명령어
- ./gradlew build: 프로젝트 빌드
- ./gradlew test: 테스트 실행
- ./gradlew bootRun: 개발 서버 실행
- docker-compose up -d: Docker 환경 실행
- docker-compose down: Docker 환경 종료
- docker-compose logs -f app: 애플리케이션 로그 확인

## 코드 스타일
- Java 17+ 문법 사용 (record, sealed class, pattern matching 등)
- REST API는 /api/v1 prefix 사용
- DTO는 record 클래스로 정의
- Exception은 GlobalExceptionHandler에서 일괄 처리
- 모든 API 응답은 ApiResponse<T> 형태로 래핑

## 패키지 구조
- controller: REST API 컨트롤러
- service: 비즈니스 로직
- repository: 데이터 접근 계층
- domain: 엔티티 클래스
- dto: 요청/응답 DTO
- config: 설정 클래스
- security: 인증/인가 관련
- exception: 예외 처리
- util: 유틸리티 클래스

## 테스트 규칙
- 단위 테스트: src/test/java에 작성
- 통합 테스트: @SpringBootTest 사용
- 테스트 DB: H2 in-memory 사용
- 테스트 네이밍: methodName_givenCondition_expectedResult

## 주요 설정 파일
- application.yml: 기본 설정
- application-local.yml: 로컬 개발 환경
- application-prod.yml: 프로덕션 환경

## 환경 변수
- KAKAO_CLIENT_ID: 카카오 앱 클라이언트 ID
- KAKAO_CLIENT_SECRET: 카카오 앱 시크릿
- KAKAO_REDIRECT_URI: OAuth2 리다이렉트 URI
- DATABASE_URL: PostgreSQL 연결 URL
- JWT_SECRET: JWT 서명 키

## 중요 참고사항
- 모든 엔티티는 BaseTimeEntity를 상속 (createdAt, updatedAt 자동 관리)
- WebSocket 엔드포인트: /ws
- STOMP destination prefix: /topic, /queue
- 약속 상태 변경 시 이벤트 발행 필수
```

---

## Phase 1: 프로젝트 초기 설정

### Step 1.1: 프로젝트 구조 탐색 및 계획

**Claude에게 지시:**
```
프로젝트 구조를 먼저 계획해줘. Spring Boot 3.x + PostgreSQL + Docker Compose 환경으로 
약속 관리 앱 백엔드를 만들 거야. 아직 코드는 작성하지 말고, 다음 항목들에 대해 계획만 세워줘:

1. 프로젝트 디렉토리 구조
2. 필요한 Gradle 의존성 목록
3. Docker Compose 서비스 구성
4. application.yml 설정 항목

think hard about this plan
```

### Step 1.2: Docker Compose 환경 구성

**Claude에게 지시:**
```
docker-compose.yml 파일을 작성해줘. 다음 서비스들이 필요해:

1. app: Spring Boot 애플리케이션 (포트 8080)
2. db: PostgreSQL 15 (포트 5432)
3. 볼륨 설정으로 데이터 영속성 보장
4. 환경 변수는 .env 파일에서 로드

Dockerfile도 함께 작성해줘. 멀티스테이지 빌드로 이미지 크기를 최적화해.
```

**예상 결과물:**
- `docker-compose.yml`
- `Dockerfile`
- `.env.example`

### Step 1.3: Spring Boot 프로젝트 초기화

**Claude에게 지시:**
```
Spring Boot 프로젝트 기본 구조를 생성해줘:

1. build.gradle에 필요한 의존성 추가:
   - Spring Web
   - Spring Data JPA
   - Spring Security
   - Spring WebSocket
   - PostgreSQL Driver
   - Lombok
   - Springdoc OpenAPI
   - JWT (jjwt)

2. application.yml 기본 설정:
   - 데이터베이스 연결 설정
   - JPA 설정 (ddl-auto: validate)
   - 로깅 설정
   - 서버 포트 설정

3. 기본 패키지 구조 생성

docker-compose up으로 실행 가능한지 확인해줘.
```

### Step 1.4: 공통 컴포넌트 구현

**Claude에게 지시:**
```
다음 공통 컴포넌트들을 구현해줘:

1. ApiResponse<T> - 통일된 API 응답 형식
2. BaseTimeEntity - 생성/수정 시간 자동 관리
3. GlobalExceptionHandler - 전역 예외 처리
4. 커스텀 예외 클래스들 (NotFoundException, BadRequestException, UnauthorizedException)

각 컴포넌트에 대한 테스트도 작성해줘.
```

---

## Phase 2: 인증 시스템 구현

### Step 2.1: 카카오 OAuth2 설정 계획

**Claude에게 지시:**
```
카카오 OAuth2 로그인 구현 계획을 세워줘. 아직 코드는 작성하지 말고:

1. 카카오 로그인 플로우 다이어그램
2. 필요한 엔드포인트 목록
3. JWT 토큰 관리 전략
4. 사용자 엔티티 설계

think about the security implications
```

### Step 2.2: User 엔티티 및 Repository 구현

**Claude에게 지시:**
```
User 도메인을 구현해줘:

1. User 엔티티:
   - id (Long, PK)
   - kakaoId (String, unique)
   - nickname (String)
   - profileImageUrl (String, nullable)
   - email (String, nullable)
   - role (enum: USER, ADMIN)
   - BaseTimeEntity 상속

2. UserRepository (JpaRepository 상속)
   - findByKakaoId 메서드

3. 테스트 작성 (TDD 방식으로 먼저 테스트 작성)

테스트가 실패하는지 먼저 확인하고, 그 다음 구현해줘.
```

### Step 2.3: 카카오 OAuth2 인증 서비스 구현

**Claude에게 지시:**
```
카카오 OAuth2 인증을 위한 서비스를 구현해줘:

1. KakaoOAuthService:
   - getKakaoLoginUrl(): 카카오 로그인 URL 생성
   - processKakaoLogin(code): 인가 코드로 사용자 정보 조회 및 저장
   - 카카오 API 호출 (RestTemplate 또는 WebClient 사용)

2. 필요한 DTO:
   - KakaoTokenResponse
   - KakaoUserInfo

3. 설정 클래스:
   - KakaoOAuthProperties (@ConfigurationProperties)

테스트는 MockServer 또는 WireMock을 사용해서 카카오 API를 모킹해줘.
```

### Step 2.4: JWT 토큰 관리 구현

**Claude에게 지시:**
```
JWT 토큰 관리 시스템을 구현해줘:

1. JwtTokenProvider:
   - createAccessToken(userId): 액세스 토큰 생성 (유효기간: 1시간)
   - createRefreshToken(userId): 리프레시 토큰 생성 (유효기간: 7일)
   - validateToken(token): 토큰 유효성 검증
   - getUserIdFromToken(token): 토큰에서 사용자 ID 추출

2. RefreshToken 엔티티 (Redis 대신 PostgreSQL 사용):
   - id, userId, token, expiryDate

3. JwtAuthenticationFilter:
   - OncePerRequestFilter 상속
   - Authorization 헤더에서 토큰 추출 및 검증

테스트 커버리지 90% 이상 목표로 작성해줘.
```

### Step 2.5: 인증 API 컨트롤러 구현

**Claude에게 지시:**
```
인증 관련 REST API를 구현해줘:

AuthController:
- GET /api/v1/auth/kakao/login-url: 카카오 로그인 URL 반환
- POST /api/v1/auth/kakao/callback: 카카오 로그인 콜백 처리
- POST /api/v1/auth/refresh: 토큰 갱신
- POST /api/v1/auth/logout: 로그아웃

각 엔드포인트에 대한 통합 테스트도 작성해줘.
Swagger 문서화도 추가해줘.
```

---

## Phase 3: 약속 핵심 기능 구현

### Step 3.1: 약속 도메인 설계

**Claude에게 지시:**
```
약속(Promise) 도메인을 설계해줘. 아직 코드는 작성하지 말고 ERD와 엔티티 설계만:

요구사항:
- 최대 참여자 10명
- 약속 상태 관리 (CREATED → RECRUITING → WAITING_LOCATIONS → SELECTING_MIDPOINT → CONFIRMED → IN_PROGRESS → COMPLETED)
- 초대 링크 유효기간 24시간
- 생성자만 확정 권한

think hard about the entity relationships
```

### Step 3.2: Promise 엔티티 구현

**Claude에게 지시:**
```
Promise(약속) 엔티티를 구현해줘 (TDD 방식):

1. 먼저 테스트 작성:
   - 약속 생성 테스트
   - 상태 변경 테스트
   - 최대 참여자 수 검증 테스트

2. Promise 엔티티:
   - id (Long, PK)
   - title (String)
   - description (String, nullable)
   - promiseDateTime (LocalDateTime)
   - status (enum: PromiseStatus)
   - inviteCode (String, unique) - UUID 기반
   - inviteExpiredAt (LocalDateTime) - 생성 후 24시간
   - maxParticipants (int, default: 10)
   - host (User, ManyToOne) - 생성자
   - confirmedLatitude (Double, nullable)
   - confirmedLongitude (Double, nullable)
   - confirmedPlaceName (String, nullable)

테스트가 통과할 때까지 구현해줘.
```

### Step 3.3: Participant 엔티티 구현

**Claude에게 지시:**
```
Participant(참여자) 엔티티를 구현해줘 (TDD 방식):

1. 테스트 먼저 작성

2. Participant 엔티티:
   - id (Long, PK)
   - promise (Promise, ManyToOne)
   - user (User, ManyToOne)
   - departureLatitude (Double, nullable)
   - departureLongitude (Double, nullable)
   - departureAddress (String, nullable)
   - isLocationSubmitted (boolean)
   - isHost (boolean)
   - joinedAt (LocalDateTime)

3. 복합 유니크 제약: (promise_id, user_id)

테스트 통과 확인 후 커밋해줘.
```

### Step 3.4: 약속 생성 API 구현

**Claude에게 지시:**
```
약속 생성 API를 구현해줘:

1. PromiseService.createPromise():
   - 약속 생성
   - 생성자를 참여자로 자동 등록 (isHost: true)
   - 초대 코드 생성 (UUID)
   - 초대 링크 만료 시간 설정 (24시간 후)

2. PromiseController:
   - POST /api/v1/promises
   
3. DTO:
   - CreatePromiseRequest (title, description, promiseDateTime)
   - PromiseResponse

4. 통합 테스트 작성

Swagger 문서화 포함해줘.
```

### Step 3.5: 약속 참여 API 구현

**Claude에게 지시:**
```
약속 참여(초대 수락) API를 구현해줘:

1. PromiseService.joinPromise():
   - 초대 코드 유효성 검증
   - 만료 시간 검증
   - 최대 참여자 수 검증 (10명)
   - 중복 참여 방지
   - 참여자 등록

2. PromiseController:
   - POST /api/v1/promises/join/{inviteCode}
   - GET /api/v1/promises/invite/{inviteCode} - 초대 정보 조회

3. 예외 처리:
   - InvalidInviteCodeException
   - InviteExpiredException  
   - MaxParticipantsExceededException
   - AlreadyJoinedException

테스트 케이스별로 검증해줘.
```

### Step 3.6: 출발지 입력 API 구현

**Claude에게 지시:**
```
참여자 출발지 입력 API를 구현해줘:

1. ParticipantService.submitDepartureLocation():
   - 출발지 좌표 및 주소 저장
   - isLocationSubmitted true로 변경
   - 모든 참여자가 입력 완료 시 약속 상태 변경 체크

2. PromiseController:
   - PUT /api/v1/promises/{promiseId}/departure
   - GET /api/v1/promises/{promiseId}/participants - 참여자 목록 및 출발지 입력 상태

3. DTO:
   - UpdateDepartureRequest (latitude, longitude, address)
   - ParticipantResponse

상태 변경 로직도 구현해줘:
- 모든 참여자 출발지 입력 완료 → WAITING_LOCATIONS에서 SELECTING_MIDPOINT로 변경
```

### Step 3.7: 약속 조회 API 구현

**Claude에게 지시:**
```
약속 조회 관련 API를 구현해줘:

1. PromiseController:
   - GET /api/v1/promises - 내 약속 목록
   - GET /api/v1/promises/{promiseId} - 약속 상세 조회
   - GET /api/v1/promises/{promiseId}/status - 약속 상태 조회

2. 필터링 옵션:
   - status: 상태별 필터
   - role: HOST/PARTICIPANT 필터
   - 페이징 처리

3. DTO:
   - PromiseListResponse
   - PromiseDetailResponse

Swagger 문서화 포함해줘.
```

---

## Phase 4: 중간지점 추천 시스템

### Step 4.1: 지하철역 데이터 로딩

**Claude에게 지시:**
```
지하철역 CSV 데이터를 로드하는 시스템을 구현해줘:

1. SubwayStation 엔티티:
   - id (Long, PK)
   - stationName (String)
   - lineName (String)
   - latitude (Double)
   - longitude (Double)

2. CSV 데이터 로딩:
   - ApplicationRunner로 서버 시작 시 로드
   - CSV 파일 경로: /Users/yoon_eunseok/Documents/컴퓨터공학/3-2/캡스톤 디자인/GgUd/src/main/resources/data/seoul_subway_stations.csv
   - 컬럼: station_name, line_name, latitude, longitude

3. SubwayStationRepository:
   - findAll()
   - findByStationNameContaining()

CSV 파일 예시도 만들어줘 (서울 주요 지하철역 10개 정도).
```

### Step 4.2: 중간지점 계산 서비스 구현

**Claude에게 지시:**
```
중간지점 계산 서비스를 구현해줘 (단순 평균 좌표 기반):

1. MidpointCalculationService:
   - calculateMidpoint(List<Coordinate> departures): 평균 좌표 계산
   - findNearestStations(Coordinate midpoint, int count): 가장 가까운 역 3~5개 반환

2. 거리 계산:
   - Haversine 공식 사용 (지구 곡률 고려)

3. DTO:
   - Coordinate (latitude, longitude)
   - MidpointResult (midpoint, nearestStations)
   - StationDistance (station, distanceKm)

4. 테스트:
   - 2명 참여자 중간지점 계산
   - 10명 참여자 중간지점 계산
   - 가장 가까운 역 찾기

TDD로 진행해줘.
```

### Step 4.3: 중간지점 추천 API 구현

**Claude에게 지시:**
```
중간지점 추천 API를 구현해줘:

1. MidpointController:
   - GET /api/v1/promises/{promiseId}/midpoint/recommendations
     - 모든 참여자 출발지 기반 중간지점 계산
     - 가까운 지하철역 3~5개 반환
     - 각 역까지의 평균 거리 포함

2. 비즈니스 로직:
   - 약속 상태가 SELECTING_MIDPOINT일 때만 조회 가능
   - 모든 참여자가 출발지를 입력해야 조회 가능

3. Response DTO:
   - MidpointRecommendationResponse
     - calculatedMidpoint (Coordinate)
     - recommendedStations (List<StationRecommendation>)
       - stationId, stationName, lineName, latitude, longitude
       - distanceFromMidpoint, averageDistanceFromParticipants

테스트 및 Swagger 문서화 포함해줘.
```

### Step 4.4: 중간지점 선택 및 투표 API 구현

**Claude에게 지시:**
```
중간지점 선택 및 투표 시스템을 구현해줘:

1. MidpointVote 엔티티:
   - id (Long, PK)
   - promise (Promise, ManyToOne)
   - participant (Participant, ManyToOne)
   - subwayStation (SubwayStation, ManyToOne)
   - votedAt (LocalDateTime)

2. API:
   - POST /api/v1/promises/{promiseId}/midpoint/vote
     - 참여자가 원하는 역에 투표 (체크 표시)
   - GET /api/v1/promises/{promiseId}/midpoint/votes
     - 현재 투표 현황 조회
   - POST /api/v1/promises/{promiseId}/midpoint/confirm
     - 호스트만 가능, 최종 중간지점 확정
     - 약속 상태를 CONFIRMED로 변경

3. 확정 시 Promise 엔티티 업데이트:
   - confirmedLatitude, confirmedLongitude
   - confirmedPlaceName (역 이름)

테스트 포함해줘.
```

---

## Phase 5: 실시간 기능 구현

### Step 5.1: WebSocket 설정

**Claude에게 지시:**
```
WebSocket + STOMP 설정을 구현해줘:

1. WebSocketConfig:
   - STOMP 엔드포인트: /ws
   - SockJS fallback 활성화
   - Application destination prefix: /app
   - Broker prefix: /topic, /queue

2. WebSocketSecurityConfig:
   - JWT 토큰 기반 인증
   - HandshakeInterceptor로 토큰 검증

3. WebSocketEventListener:
   - 연결/해제 이벤트 로깅

설정 후 연결 테스트 방법도 알려줘.
```

### Step 5.2: 실시간 위치 공유 서비스 구현

**Claude에게 지시:**
```
실시간 위치 공유 서비스를 구현해줘:

1. LocationTrackingService:
   - updateLocation(promiseId, userId, latitude, longitude)
   - getParticipantLocations(promiseId)
   - 위치 정보는 메모리에 캐싱 (약속 종료 후 삭제)

2. LocationTrackingController (WebSocket):
   - /app/promises/{promiseId}/location - 위치 업데이트 수신
   - /topic/promises/{promiseId}/locations - 참여자들에게 위치 브로드캐스트

3. 비즈니스 규칙:
   - 약속 시작 5분 전부터만 위치 공유 가능
   - 약속 상태가 IN_PROGRESS일 때만 가능
   - 1초 단위로 위치 업데이트 (클라이언트 제어)

4. DTO:
   - LocationUpdateMessage (userId, latitude, longitude, timestamp)
   - ParticipantLocationResponse

테스트도 작성해줘.
```

### Step 5.3: 약속 상태 실시간 알림

**Claude에게 지시:**
```
약속 상태 변경 시 실시간 알림을 구현해줘:

1. PromiseEventPublisher:
   - 약속 상태 변경 시 이벤트 발행
   - Spring의 ApplicationEventPublisher 사용

2. PromiseEventListener:
   - 상태 변경 이벤트 수신
   - WebSocket으로 참여자들에게 브로드캐스트

3. WebSocket destination:
   - /topic/promises/{promiseId}/status

4. 알림 대상 이벤트:
   - 새 참여자 참여
   - 출발지 입력 완료
   - 중간지점 확정
   - 약속 시작 (IN_PROGRESS)
   - 약속 완료

이벤트 기반 아키텍처로 구현해줘.
```

---

## Phase 6: 카카오 API 통합

### Step 6.1: 카카오 API 클라이언트 설정

**Claude에게 지시:**
```
카카오 API 클라이언트를 구현해줘:

1. KakaoApiClient:
   - WebClient 기반
   - 공통 헤더 설정 (Authorization)
   - 에러 핸들링

2. KakaoApiProperties:
   - REST API 키
   - JavaScript 키
   - API 베이스 URL

3. 설정 클래스:
   - WebClient Bean 설정
   - 타임아웃 설정

테스트용 Mock 설정도 포함해줘.
```

### Step 6.2: 카카오 메시지 API 통합 (초대 링크 공유)

**Claude에게 지시:**
```
카카오톡 메시지 전송 기능을 구현해줘:

1. KakaoMessageService:
   - sendInviteMessage(userId, inviteUrl, promiseTitle)
   - 카카오 메시지 API 사용

2. API:
   - POST /api/v1/promises/{promiseId}/invite/send
   - 카카오톡 친구에게 초대 링크 전송

3. 초대 메시지 템플릿:
   - 약속 제목
   - 약속 일시
   - 초대 링크 (앱 딥링크 또는 웹 URL)

참고: 카카오 메시지 API는 사용자 동의가 필요하므로, 
동의 여부 확인 로직도 포함해줘.
```

### Step 6.3: 카카오 길찾기 API 통합

**Claude에게 지시:**
```
카카오 길찾기 API를 통합해줘:

1. KakaoDirectionsService:
   - getDirections(origin, destination): 대중교통 경로 조회
   - 카카오 모빌리티 API 사용

2. API:
   - GET /api/v1/promises/{promiseId}/directions
   - 현재 위치에서 약속 장소까지 경로 반환

3. Response DTO:
   - DirectionsResponse
     - totalDuration (분)
     - totalDistance (m)
     - routes (List<RouteStep>)
       - type (도보/버스/지하철)
       - instruction
       - duration
       - distance

4. 에러 처리:
   - API 호출 실패 시 재시도
   - 경로 없음 처리

테스트 및 Swagger 문서화 포함해줘.
```

### Step 6.4: 카카오맵 연동 데이터 제공

**Claude에게 지시:**
```
프론트엔드 카카오맵 연동을 위한 API를 구현해줘:

1. MapDataController:
   - GET /api/v1/promises/{promiseId}/map-data
     - 약속 장소 좌표
     - 참여자 출발지 좌표들
     - 추천 중간지점들

2. Response:
   - MapDataResponse
     - destination (확정된 약속 장소)
     - participantDepartures (출발지 목록)
     - recommendedMidpoints (추천 중간지점들)
     - currentLocations (실시간 위치, IN_PROGRESS일 때만)

프론트엔드에서 카카오맵에 마커로 표시할 수 있는 형태로 제공해줘.
```

---

## API 명세

### 인증 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /api/v1/auth/kakao/login-url | 카카오 로그인 URL 조회 |
| POST | /api/v1/auth/kakao/callback | 카카오 로그인 콜백 |
| POST | /api/v1/auth/refresh | 토큰 갱신 |
| POST | /api/v1/auth/logout | 로그아웃 |

### 약속 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /api/v1/promises | 약속 생성 |
| GET | /api/v1/promises | 내 약속 목록 |
| GET | /api/v1/promises/{promiseId} | 약속 상세 조회 |
| GET | /api/v1/promises/invite/{inviteCode} | 초대 정보 조회 |
| POST | /api/v1/promises/join/{inviteCode} | 약속 참여 |
| PUT | /api/v1/promises/{promiseId}/departure | 출발지 입력 |
| GET | /api/v1/promises/{promiseId}/participants | 참여자 목록 |
| POST | /api/v1/promises/{promiseId}/invite/send | 초대 메시지 전송 |

### 중간지점 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /api/v1/promises/{promiseId}/midpoint/recommendations | 중간지점 추천 |
| POST | /api/v1/promises/{promiseId}/midpoint/vote | 중간지점 투표 |
| GET | /api/v1/promises/{promiseId}/midpoint/votes | 투표 현황 |
| POST | /api/v1/promises/{promiseId}/midpoint/confirm | 중간지점 확정 (호스트) |

### 길찾기 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /api/v1/promises/{promiseId}/directions | 경로 조회 |
| GET | /api/v1/promises/{promiseId}/map-data | 지도 데이터 조회 |

### WebSocket Endpoints

| Type | Destination | 설명 |
|------|-------------|------|
| SEND | /app/promises/{promiseId}/location | 위치 업데이트 |
| SUBSCRIBE | /topic/promises/{promiseId}/locations | 참여자 위치 수신 |
| SUBSCRIBE | /topic/promises/{promiseId}/status | 약속 상태 변경 수신 |

---

## 데이터베이스 스키마

```sql
-- 사용자
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    kakao_id VARCHAR(255) UNIQUE NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    profile_image_url VARCHAR(500),
    email VARCHAR(255),
    role VARCHAR(20) DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 리프레시 토큰
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    token VARCHAR(500) NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 약속
CREATE TABLE promises (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    promise_date_time TIMESTAMP NOT NULL,
    status VARCHAR(30) DEFAULT 'CREATED',
    invite_code VARCHAR(36) UNIQUE NOT NULL,
    invite_expired_at TIMESTAMP NOT NULL,
    max_participants INT DEFAULT 10,
    host_id BIGINT REFERENCES users(id),
    confirmed_latitude DOUBLE PRECISION,
    confirmed_longitude DOUBLE PRECISION,
    confirmed_place_name VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 참여자
CREATE TABLE participants (
    id BIGSERIAL PRIMARY KEY,
    promise_id BIGINT REFERENCES promises(id),
    user_id BIGINT REFERENCES users(id),
    departure_latitude DOUBLE PRECISION,
    departure_longitude DOUBLE PRECISION,
    departure_address VARCHAR(500),
    is_location_submitted BOOLEAN DEFAULT FALSE,
    is_host BOOLEAN DEFAULT FALSE,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(promise_id, user_id)
);

-- 지하철역
CREATE TABLE subway_stations (
    id BIGSERIAL PRIMARY KEY,
    station_name VARCHAR(100) NOT NULL,
    line_name VARCHAR(50) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL
);

-- 중간지점 투표
CREATE TABLE midpoint_votes (
    id BIGSERIAL PRIMARY KEY,
    promise_id BIGINT REFERENCES promises(id),
    participant_id BIGINT REFERENCES participants(id),
    subway_station_id BIGINT REFERENCES subway_stations(id),
    voted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(promise_id, participant_id)
);

-- 인덱스
CREATE INDEX idx_promises_host_id ON promises(host_id);
CREATE INDEX idx_promises_status ON promises(status);
CREATE INDEX idx_promises_invite_code ON promises(invite_code);
CREATE INDEX idx_participants_promise_id ON participants(promise_id);
CREATE INDEX idx_participants_user_id ON participants(user_id);
CREATE INDEX idx_subway_stations_name ON subway_stations(station_name);
```

---

## 개발 순서 체크리스트

### Phase 1: 프로젝트 초기 설정
- [ ] 프로젝트 디렉토리 구조 생성
- [ ] Docker Compose 환경 구성
- [ ] Spring Boot 프로젝트 초기화
- [ ] 공통 컴포넌트 구현 (ApiResponse, BaseTimeEntity, GlobalExceptionHandler)

### Phase 2: 인증 시스템
- [ ] User 엔티티 및 Repository
- [ ] 카카오 OAuth2 서비스
- [ ] JWT 토큰 관리
- [ ] 인증 API 컨트롤러

### Phase 3: 약속 핵심 기능
- [ ] Promise, Participant 엔티티
- [ ] 약속 생성 API
- [ ] 약속 참여 API
- [ ] 출발지 입력 API
- [ ] 약속 조회 API

### Phase 4: 중간지점 추천
- [ ] 지하철역 데이터 로딩
- [ ] 중간지점 계산 서비스
- [ ] 중간지점 추천 API
- [ ] 중간지점 투표 및 확정 API

### Phase 5: 실시간 기능
- [ ] WebSocket 설정
- [ ] 실시간 위치 공유
- [ ] 약속 상태 실시간 알림

### Phase 6: 카카오 API 통합
- [ ] 카카오 API 클라이언트
- [ ] 카카오 메시지 API (초대 링크)
- [ ] 카카오 길찾기 API
- [ ] 카카오맵 연동 데이터

---

## Claude Code 워크플로우 팁

### 1. 탐색 → 계획 → 코딩 → 커밋 패턴 활용
각 Step 시작 시 "아직 코드는 작성하지 말고 계획만 세워줘"라고 지시하여 Claude가 먼저 설계를 검토하도록 합니다.

### 2. TDD 방식 적용
```
테스트 작성 → 테스트 실패 확인 → 구현 → 테스트 통과 확인 → 커밋
```

### 3. 구체적인 지시
```
좋은 예: "JwtTokenProvider를 구현해줘. 액세스 토큰 유효기간은 1시간, 
       리프레시 토큰은 7일로 설정하고, HS256 알고리즘을 사용해."

나쁜 예: "JWT 구현해줘"
```

### 4. Extended Thinking 활용
복잡한 설계가 필요한 경우 "think hard" 또는 "think harder"를 사용합니다.
```
"think hard about the security implications of this OAuth2 implementation"
```

### 5. 진행 상황 추적
이 문서의 체크리스트를 Claude의 작업 스크래치패드로 활용하여 진행 상황을 추적합니다.

### 6. 자주 /clear 사용
Phase가 바뀔 때마다 `/clear`를 실행하여 컨텍스트를 정리합니다.

---

## 참고 자료

- [Claude Code Best Practices](https://www.anthropic.com/engineering/claude-code-best-practices)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [카카오 로그인 API](https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api)
- [카카오 메시지 API](https://developers.kakao.com/docs/latest/ko/message/rest-api)
- [카카오 모빌리티 API](https://developers.kakaomobility.com/docs/)