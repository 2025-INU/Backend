# Phase 6: 카카오 API 통합

## 목표 (Goals)
- 카카오 Message API를 통한 초대 링크 전송
- 카카오 Directions API를 통한 길찾기 제공
- 카카오맵 연동 데이터 포맷팅
- 외부 API 에러 핸들링 및 재시도 로직

## Step 6.1: 카카오 API 클라이언트 설정

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

## Step 6.2: 카카오 메시지 API 통합 (초대 링크 공유)

**Claude에게 지시:**
```
카카오톡 메시지 전송 기능을 구현해줘:

1. KakaoMessageService:
   - sendInviteMessage(userId, inviteUrl, meetingTitle)
   - 카카오 메시지 API 사용

2. API:
   - POST /api/v1/meetings/{meetingId}/invite/send
   - 카카오톡 친구에게 초대 링크 전송

3. 초대 메시지 템플릿:
   - 약속 제목
   - 약속 일시
   - 초대 링크 (앱 딥링크 또는 웹 URL)

참고: 카카오 메시지 API는 사용자 동의가 필요하므로,
동의 여부 확인 로직도 포함해줘.
```

**API 상세**: [../api/backend-api.md#meeting-apis](../api/backend-api.md)

## Step 6.3: 카카오 길찾기 API 통합

**Claude에게 지시:**
```
카카오 길찾기 API를 통합해줘:

1. KakaoDirectionsService:
   - getDirections(origin, destination): 대중교통 경로 조회
   - 카카오 모빌리티 API 사용

2. API:
   - GET /api/v1/meetings/{meetingId}/directions
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

## Step 6.4: 카카오맵 연동 데이터 제공

**Claude에게 지시:**
```
프론트엔드 카카오맵 연동을 위한 API를 구현해줘:

1. MapDataController:
   - GET /api/v1/meetings/{meetingId}/map-data
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

## Step 6.5: 외부 API 에러 핸들링 및 재시도

**Claude에게 지시:**
```
외부 API 호출 실패 처리를 구현해줘:

1. ExternalApiRetryService:
   - Spring Retry 사용
   - 최대 3회 재시도
   - Exponential backoff 전략

2. ExternalApiExceptionHandler:
   - 카카오 API 에러 코드별 처리
   - Rate limit 초과 처리
   - 타임아웃 처리

3. 폴백 전략:
   - 메시지 전송 실패 → 이메일 알림 대안
   - 길찾기 실패 → 직선 거리 정보 제공
   - 맵 데이터 조회 실패 → 캐시된 데이터 반환

4. 모니터링:
   - API 호출 성공률 로깅
   - 실패 원인 추적

Spring Retry 설정 및 테스트 포함해줘.
```

## Validation Checklist
- [ ] 카카오 API 클라이언트 설정 완료
- [ ] 카카오톡 초대 메시지 전송 성공
- [ ] 메시지 템플릿 정상 렌더링 확인
- [ ] 길찾기 API 호출 성공
- [ ] 대중교통 경로 정보 정확성 확인
- [ ] 카카오맵 데이터 포맷 검증
- [ ] 재시도 로직 동작 확인 (실패 시나리오)
- [ ] Rate limit 초과 시 에러 핸들링 확인
- [ ] 폴백 전략 동작 확인
- [ ] API 호출 성공률 모니터링 로그 확인
- [ ] Swagger UI에서 모든 API 문서 확인

## 프로젝트 완료
Phase 6 완료 시 MVP 버전의 모든 기능이 구현됩니다!

## 관련 문서
- **API 명세**: [../api/backend-api.md#navigation-apis](../api/backend-api.md)
- **프로젝트 개요**: [../project_brief.md](../project_brief.md)
- **현재 진행 상황**: [../_memory/current_state.md](../_memory/current_state.md)
