# 백엔드 API 명세

## Base URL
```
http://localhost:8080/api/v1
```

## 인증
대부분의 엔드포인트는 `Authorization` 헤더를 통한 JWT 인증이 필요합니다:
```
Authorization: Bearer <access_token>
```

---

## 인증 APIs

### GET /auth/kakao/login-url
클라이언트 측 리다이렉트를 위한 카카오 OAuth2 로그인 URL을 가져옵니다.

**인증**: 불필요

**응답**:
```json
{
  "loginUrl": "https://kauth.kakao.com/oauth/authorize?client_id=..."
}
```

---

### POST /auth/kakao/callback
인가 코드로 카카오 로그인 콜백을 처리합니다.

**인증**: 불필요

**요청**:
```json
{
  "code": "authorization_code_from_kakao"
}
```

**응답**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "kakaoId": "123456789",
    "nickname": "홍길동",
    "profileImageUrl": "https://...",
    "email": "user@example.com"
  }
}
```

---

### POST /auth/refresh
리프레시 토큰을 사용하여 만료된 액세스 토큰을 갱신합니다.

**인증**: 불필요

**요청**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**응답**:
```json
{
  "accessToken": "new_access_token",
  "refreshToken": "new_refresh_token"
}
```

---

### POST /auth/logout
사용자 로그아웃 및 리프레시 토큰 무효화.

**인증**: 필요

**요청**: 없음

**응답**:
```json
{
  "message": "Logged out successfully"
}
```

---

## 모임 APIs

### POST /meetings
새 모임을 생성합니다.

**인증**: 필요

**요청**:
```json
{
  "title": "팀 회의",
  "description": "프로젝트 진행 상황 논의",
  "meetingDateTime": "2025-01-15T14:00:00"
}
```

**응답**:
```json
{
  "id": 1,
  "title": "팀 회의",
  "description": "프로젝트 진행 상황 논의",
  "meetingDateTime": "2025-01-15T14:00:00",
  "status": "PLANNING",
  "inviteCode": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "inviteExpiredAt": "2025-01-11T10:00:00",
  "maxParticipants": 10,
  "host": {
    "id": 1,
    "nickname": "홍길동"
  },
  "createdAt": "2025-01-10T10:00:00"
}
```

---

### GET /meetings
사용자의 모임 목록을 가져옵니다.

**인증**: 필요

**쿼리 파라미터**:
- `status` (선택): 모임 상태로 필터링 (PLANNING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED)
- `role` (선택): 사용자 역할로 필터링 (HOST, PARTICIPANT)
- `page` (선택): 페이지 번호 (기본값: 0)
- `size` (선택): 페이지 크기 (기본값: 20)

**응답**:
```json
{
  "content": [
    {
      "id": 1,
      "title": "팀 회의",
      "meetingDateTime": "2025-01-15T14:00:00",
      "status": "PLANNING",
      "participantCount": 3,
      "isHost": true
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1
}
```

---

### GET /meetings/{meetingId}
모임 상세 정보를 가져옵니다.

**인증**: 필요 (참가자만)

**응답**:
```json
{
  "id": 1,
  "title": "팀 회의",
  "description": "프로젝트 진행 상황 논의",
  "meetingDateTime": "2025-01-15T14:00:00",
  "status": "PLANNING",
  "inviteCode": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "host": {
    "id": 1,
    "nickname": "홍길동"
  },
  "participants": [
    {
      "id": 1,
      "nickname": "홍길동",
      "isHost": true,
      "isLocationSubmitted": true
    }
  ],
  "confirmedPlace": null,
  "createdAt": "2025-01-10T10:00:00"
}
```

---

### GET /meetings/invite/{inviteCode}
모임 초대 정보를 가져옵니다.

**인증**: 불필요

**응답**:
```json
{
  "meetingId": 1,
  "title": "팀 회의",
  "meetingDateTime": "2025-01-15T14:00:00",
  "host": {
    "id": 1,
    "nickname": "홍길동"
  },
  "currentParticipants": 3,
  "maxParticipants": 10,
  "isExpired": false,
  "expiredAt": "2025-01-11T10:00:00"
}
```

---

### POST /meetings/join/{inviteCode}
초대 코드를 사용하여 모임에 참가합니다.

**인증**: 필요

**응답**:
```json
{
  "meetingId": 1,
  "participantId": 2,
  "message": "Successfully joined the meeting"
}
```

**에러**:
- `400`: 유효하지 않은 초대 코드, 만료된 초대, 또는 모임 인원 초과
- `409`: 이미 참가함

---

### PUT /meetings/{meetingId}/departure
출발 위치를 제출합니다.

**인증**: 필요 (참가자만)

**요청**:
```json
{
  "latitude": 37.5665,
  "longitude": 126.9780,
  "address": "서울특별시 중구 세종대로 110"
}
```

**응답**:
```json
{
  "participantId": 1,
  "departureLatitude": 37.5665,
  "departureLongitude": 126.9780,
  "departureAddress": "서울특별시 중구 세종대로 110",
  "isLocationSubmitted": true
}
```

---

### GET /meetings/{meetingId}/participants
모임 참가자 목록을 가져옵니다.

**인증**: 필요 (참가자만)

**응답**:
```json
{
  "participants": [
    {
      "id": 1,
      "userId": 1,
      "nickname": "홍길동",
      "isHost": true,
      "isLocationSubmitted": true,
      "joinedAt": "2025-01-10T10:00:00"
    },
    {
      "id": 2,
      "userId": 2,
      "nickname": "김철수",
      "isHost": false,
      "isLocationSubmitted": false,
      "joinedAt": "2025-01-10T11:00:00"
    }
  ],
  "allLocationsSubmitted": false
}
```

---

### POST /meetings/{meetingId}/invite/send
카카오톡으로 모임 초대를 전송합니다.

**인증**: 필요 (참가자만)

**요청**:
```json
{
  "recipientKakaoIds": ["123456789", "987654321"]
}
```

**응답**:
```json
{
  "successCount": 2,
  "failedCount": 0,
  "inviteUrl": "https://ggud.app/invite/a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

## 중간 지점 APIs

### GET /meetings/{meetingId}/midpoint/recommendations
중간 지점 근처의 AI 기반 장소 추천을 가져옵니다.

**인증**: 필요 (참가자만)

**응답**:
```json
{
  "calculatedMidpoint": {
    "latitude": 37.5665,
    "longitude": 126.9780
  },
  "recommendedStations": [
    {
      "stationId": 1,
      "stationName": "서울역",
      "lineName": "1호선",
      "latitude": 37.5547,
      "longitude": 126.9707,
      "distanceFromMidpoint": 1.5,
      "averageDistanceFromParticipants": 3.2
    }
  ]
}
```

**에러**:
- `400`: 모든 참가자가 출발 위치를 제출하지 않음
- `403`: 모임이 SELECTING_MIDPOINT 상태가 아님

---

### POST /meetings/{meetingId}/midpoint/confirm
선택한 모임 장소를 확정합니다 (호스트만).

**인증**: 필요 (호스트만)

**요청**:
```json
{
  "subwayStationId": 1
}
```

**응답**:
```json
{
  "meetingId": 1,
  "status": "CONFIRMED",
  "confirmedPlace": {
    "stationId": 1,
    "stationName": "서울역",
    "lineName": "1호선",
    "latitude": 37.5547,
    "longitude": 126.9707
  },
  "confirmedAt": "2025-01-10T12:00:00"
}
```

**에러**:
- `403`: 호스트만 확정할 수 있거나, 모임이 SELECTING_MIDPOINT 상태가 아님
- `400`: 유효하지 않은 역 ID (추천 목록에 없음)

---

## 길찾기 APIs

### GET /meetings/{meetingId}/directions
현재 위치에서 모임 장소까지의 길찾기 정보를 가져옵니다.

**인증**: 필요 (참가자만)

**쿼리 파라미터**:
- `originLat`: 현재 위도
- `originLon`: 현재 경도

**응답**:
```json
{
  "totalDuration": 25,
  "totalDistance": 2500,
  "routes": [
    {
      "type": "WALK",
      "instruction": "서울역 방면으로 도보 이동",
      "duration": 5,
      "distance": 400
    },
    {
      "type": "SUBWAY",
      "instruction": "1호선 탑승, 서울역 하차",
      "duration": 15,
      "distance": 2000
    },
    {
      "type": "WALK",
      "instruction": "출구 2번으로 이동",
      "duration": 5,
      "distance": 100
    }
  ]
}
```

---

### GET /meetings/{meetingId}/map-data
프론트엔드 KakaoMap 통합을 위한 지도 데이터를 가져옵니다.

**인증**: 필요 (참가자만)

**응답**:
```json
{
  "destination": {
    "latitude": 37.5547,
    "longitude": 126.9707,
    "placeName": "서울역",
    "type": "CONFIRMED"
  },
  "participantDepartures": [
    {
      "userId": 1,
      "nickname": "홍길동",
      "latitude": 37.5665,
      "longitude": 126.9780
    }
  ],
  "recommendedMidpoints": [
    {
      "stationName": "서울역",
      "latitude": 37.5547,
      "longitude": 126.9707
    }
  ],
  "currentLocations": []
}
```

---

## WebSocket 엔드포인트

### 연결
**엔드포인트**: `/ws`
**프로토콜**: STOMP over SockJS

### 구독 목적지

#### /topic/meetings/{meetingId}/locations
모든 참가자의 실시간 위치 업데이트를 수신합니다.

**메시지 형식**:
```json
{
  "userId": 1,
  "nickname": "홍길동",
  "latitude": 37.5665,
  "longitude": 126.9780,
  "timestamp": "2025-01-15T14:05:00"
}
```

#### /topic/meetings/{meetingId}/status
모임 상태 변경 알림을 수신합니다.

**메시지 형식**:
```json
{
  "meetingId": 1,
  "previousStatus": "PLANNING",
  "currentStatus": "CONFIRMED",
  "message": "Meeting place has been confirmed",
  "timestamp": "2025-01-10T12:00:00"
}
```

### 전송 목적지

#### /app/meetings/{meetingId}/location
서버로 위치 업데이트를 전송합니다.

**메시지 형식**:
```json
{
  "latitude": 37.5665,
  "longitude": 126.9780
}
```

---

## 에러 응답

모든 에러 응답은 다음 형식을 따릅니다:

```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable error message",
  "timestamp": "2025-01-10T10:00:00",
  "path": "/api/v1/meetings/1"
}
```

### 일반적인 에러 코드
- `400 Bad Request`: 유효하지 않은 요청 파라미터
- `401 Unauthorized`: 인증 토큰이 없거나 유효하지 않음
- `403 Forbidden`: 권한 부족
- `404 Not Found`: 리소스를 찾을 수 없음
- `409 Conflict`: 리소스 충돌 (예: 이미 참가함)
- `500 Internal Server Error`: 서버 에러

---

## 상태 코드 요약

| 코드 | 의미 | 사용처 |
|------|---------|-------|
| 200 | OK | 성공한 GET, PUT, DELETE |
| 201 | Created | 성공한 POST (리소스 생성됨) |
| 204 | No Content | 성공한 DELETE (응답 본문 없음) |
| 400 | Bad Request | 유효하지 않은 입력, 검증 오류 |
| 401 | Unauthorized | 토큰이 없거나 유효하지 않음 |
| 403 | Forbidden | 유효한 토큰이지만 권한 부족 |
| 404 | Not Found | 리소스가 존재하지 않음 |
| 409 | Conflict | 중복 리소스, 상태 충돌 |
| 500 | Internal Server Error | 예상치 못한 서버 오류 |

---

## 관련 문서
- **구현 단계**: [../phases/phase-0-overview.md](../phases/phase-0-overview.md)
- **아키텍처 결정**: [../decisions/adr-index.md](../decisions/adr-index.md)
- **프로젝트 개요**: [../project_brief.md](../project_brief.md)
