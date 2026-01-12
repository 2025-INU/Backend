# Phase 5: 실시간 기능 구현

## 목표 (Goals)
- WebSocket + STOMP 설정 구성
- 실시간 위치 공유 서비스 구현
- 약속 상태 변경 실시간 알림
- 참여자 presence 트래킹

## Step 5.1: WebSocket 설정

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

**API 상세**: [../api/backend-api.md#websocket-endpoints](../api/backend-api.md)

## Step 5.2: 실시간 위치 공유 서비스 구현

**Claude에게 지시:**
```
실시간 위치 공유 서비스를 구현해줘:

1. LocationTrackingService:
   - updateLocation(meetingId, userId, latitude, longitude)
   - getParticipantLocations(meetingId)
   - 위치 정보는 메모리에 캐싱 (약속 종료 후 삭제)

2. LocationTrackingController (WebSocket):
   - /app/meetings/{meetingId}/location - 위치 업데이트 수신
   - /topic/meetings/{meetingId}/locations - 참여자들에게 위치 브로드캐스트

3. 비즈니스 규칙:
   - 약속 시작 5분 전부터만 위치 공유 가능
   - 약속 상태가 IN_PROGRESS일 때만 가능
   - 1초 단위로 위치 업데이트 (클라이언트 제어)

4. DTO:
   - LocationUpdateMessage (userId, latitude, longitude, timestamp)
   - ParticipantLocationResponse

테스트도 작성해줘.
```

## Step 5.3: 약속 상태 실시간 알림

**Claude에게 지시:**
```
약속 상태 변경 시 실시간 알림을 구현해줘:

1. MeetingEventPublisher:
   - 약속 상태 변경 시 이벤트 발행
   - Spring의 ApplicationEventPublisher 사용

2. MeetingEventListener:
   - 상태 변경 이벤트 수신
   - WebSocket으로 참여자들에게 브로드캐스트

3. WebSocket destination:
   - /topic/meetings/{meetingId}/status

4. 알림 대상 이벤트:
   - 새 참여자 참여
   - 출발지 입력 완료
   - 중간지점 확정
   - 약속 시작 (IN_PROGRESS)
   - 약속 완료

이벤트 기반 아키텍처로 구현해줘.
```

## Step 5.4: 위치 공유 시간 관리

**Claude에게 지시:**
```
위치 공유 시작 및 종료 관리를 구현해줘:

1. MeetingSchedulerService:
   - 약속 시작 5분 전 자동으로 상태를 IN_PROGRESS로 변경
   - Spring Scheduler 사용 (@Scheduled)
   - 약속 종료 시 위치 데이터 자동 정리

2. 비즈니스 로직:
   - meetingDateTime - 5분 → IN_PROGRESS
   - meetingDateTime + 2시간 → COMPLETED (기본 종료 시간)
   - 위치 캐시 자동 삭제

3. 설정:
   - @EnableScheduling 활성화
   - 스케줄링 주기: 1분마다 체크

테스트는 Mockito로 시간 제어해서 작성해줘.
```

## Validation Checklist
- [ ] WebSocket 연결 성공
- [ ] STOMP 메시지 송수신 테스트 통과
- [ ] JWT 토큰 기반 WebSocket 인증 동작
- [ ] 위치 업데이트 브로드캐스트 성공
- [ ] 약속 상태 변경 알림 수신 확인
- [ ] 5분 전 자동 상태 변경 동작 확인
- [ ] 약속 종료 후 위치 데이터 삭제 확인
- [ ] 동시 접속자 처리 테스트 (부하 테스트)
- [ ] SockJS fallback 동작 확인

## 다음 Phase
[phase-6-kakao.md](phase-6-kakao.md) - 카카오 API 통합

## 관련 문서
- **API 명세**: [../api/backend-api.md#websocket-endpoints](../api/backend-api.md)
- **프로젝트 개요**: [../project_brief.md](../project_brief.md)
- **현재 진행 상황**: [../_memory/current_state.md](../_memory/current_state.md)
