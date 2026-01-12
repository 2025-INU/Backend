# Phase 3: 약속 핵심 기능 구현

## 목표 (Goals)
- Meeting(약속) 및 Participant(참여자) 엔티티 구현
- 약속 생성 및 참여 시스템 구축
- 초대 링크 기반 참여 메커니즘 구현
- 출발지 입력 및 관리 기능

## Step 3.1: 약속 도메인 설계

**Claude에게 지시:**
```
약속(Meeting) 도메인을 설계해줘. 아직 코드는 작성하지 말고 ERD와 엔티티 설계만:

요구사항:
- 최대 참여자 10명
- 약속 상태 관리 (PLANNING → CONFIRMED → IN_PROGRESS → COMPLETED → CANCELLED)
- 초대 링크 유효기간 24시간
- 생성자만 확정 권한

think hard about the entity relationships
```

## Step 3.2: Meeting 엔티티 구현

**Claude에게 지시:**
```
Meeting(약속) 엔티티를 구현해줘 (TDD 방식):

1. 먼저 테스트 작성:
   - 약속 생성 테스트
   - 상태 변경 테스트
   - 최대 참여자 수 검증 테스트

2. Meeting 엔티티:
   - id (Long, PK)
   - title (String)
   - description (String, nullable)
   - meetingDateTime (LocalDateTime)
   - status (enum: MeetingStatus)
   - inviteCode (String, unique) - UUID 기반
   - inviteExpiredAt (LocalDateTime) - 생성 후 24시간
   - maxParticipants (int, default: 10)
   - host (User, ManyToOne) - 생성자
   - confirmedLatitude (Double, nullable)
   - confirmedLongitude (Double, nullable)
   - confirmedPlaceName (String, nullable)

테스트가 통과할 때까지 구현해줘.
```

**참고**: [ADR-0005](../decisions/adr-0005-max-participants.md) - 최대 10명 제한

## Step 3.3: Participant 엔티티 구현

**Claude에게 지시:**
```
Participant(참여자) 엔티티를 구현해줘 (TDD 방식):

1. 테스트 먼저 작성

2. Participant 엔티티:
   - id (Long, PK)
   - meeting (Meeting, ManyToOne)
   - user (User, ManyToOne)
   - departureLatitude (Double, nullable)
   - departureLongitude (Double, nullable)
   - departureAddress (String, nullable)
   - isLocationSubmitted (boolean)
   - isHost (boolean)
   - joinedAt (LocalDateTime)

3. 복합 유니크 제약: (meeting_id, user_id)

테스트 통과 확인 후 커밋해줘.
```

## Step 3.4: 약속 생성 API 구현

**Claude에게 지시:**
```
약속 생성 API를 구현해줘:

1. MeetingService.createMeeting():
   - 약속 생성
   - 생성자를 참여자로 자동 등록 (isHost: true)
   - 초대 코드 생성 (UUID)
   - 초대 링크 만료 시간 설정 (24시간 후)

2. MeetingController:
   - POST /api/v1/meetings

3. DTO:
   - CreateMeetingRequest (title, description, meetingDateTime)
   - MeetingResponse

4. 통합 테스트 작성

Swagger 문서화 포함해줘.
```

**API 상세**: [../api/backend-api.md#meeting-apis](../api/backend-api.md)

## Step 3.5: 약속 참여 API 구현

**Claude에게 지시:**
```
약속 참여(초대 수락) API를 구현해줘:

1. MeetingService.joinMeeting():
   - 초대 코드 유효성 검증
   - 만료 시간 검증
   - 최대 참여자 수 검증 (10명)
   - 중복 참여 방지
   - 참여자 등록

2. MeetingController:
   - POST /api/v1/meetings/join/{inviteCode}
   - GET /api/v1/meetings/invite/{inviteCode} - 초대 정보 조회

3. 예외 처리:
   - InvalidInviteCodeException
   - InviteExpiredException
   - MaxParticipantsExceededException
   - AlreadyJoinedException

테스트 케이스별로 검증해줘.
```

## Step 3.6: 출발지 입력 API 구현

**Claude에게 지시:**
```
참여자 출발지 입력 API를 구현해줘:

1. ParticipantService.submitDepartureLocation():
   - 출발지 좌표 및 주소 저장
   - isLocationSubmitted true로 변경
   - 모든 참여자가 입력 완료 시 약속 상태 변경 체크

2. MeetingController:
   - PUT /api/v1/meetings/{meetingId}/departure
   - GET /api/v1/meetings/{meetingId}/participants - 참여자 목록 및 출발지 입력 상태

3. DTO:
   - UpdateDepartureRequest (latitude, longitude, address)
   - ParticipantResponse

상태 변경 로직도 구현해줘:
- 모든 참여자 출발지 입력 완료 → PLANNING에서 SELECTING_MIDPOINT로 변경
```

## Step 3.7: 약속 조회 API 구현

**Claude에게 지시:**
```
약속 조회 관련 API를 구현해줘:

1. MeetingController:
   - GET /api/v1/meetings - 내 약속 목록
   - GET /api/v1/meetings/{meetingId} - 약속 상세 조회
   - GET /api/v1/meetings/{meetingId}/status - 약속 상태 조회

2. 필터링 옵션:
   - status: 상태별 필터
   - role: HOST/PARTICIPANT 필터
   - 페이징 처리

3. DTO:
   - MeetingListResponse
   - MeetingDetailResponse

Swagger 문서화 포함해줘.
```

## Validation Checklist
- [ ] Meeting 엔티티 테스트 통과
- [ ] Participant 엔티티 테스트 통과
- [ ] 약속 생성 API 성공
- [ ] 초대 코드 생성 및 만료 시간 설정 확인
- [ ] 약속 참여 API 성공
- [ ] 최대 참여자 수 제한 동작 확인
- [ ] 출발지 입력 API 성공
- [ ] 모든 참여자 입력 시 상태 변경 확인
- [ ] 약속 조회 API 성공 (목록, 상세)
- [ ] 페이징 처리 동작 확인
- [ ] Swagger UI에서 모든 API 문서 확인

## 다음 Phase
[phase-4-midpoint.md](phase-4-midpoint.md) - 중간지점 추천 시스템

## 관련 문서
- **API 명세**: [../api/backend-api.md#meeting-apis](../api/backend-api.md)
- **ADR-0005**: [../decisions/adr-0005-max-participants.md](../decisions/adr-0005-max-participants.md)
- **ADR-0002**: [../decisions/adr-0002-cascade-delete.md](../decisions/adr-0002-cascade-delete.md)
- **현재 진행 상황**: [../_memory/current_state.md](../_memory/current_state.md)
