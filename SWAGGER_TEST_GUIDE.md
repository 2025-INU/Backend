# Swagger API 테스트 가이드

**Swagger UI URL**: http://localhost:8080/swagger-ui/index.html

이 문서는 Swagger에서 모든 API를 순서대로 테스트하는 방법을 안내합니다.
**임시 파일**이므로 테스트 완료 후 삭제하세요.

---

## 사전 준비

### 1. Docker 컨테이너 상태 확인
```bash
docker-compose ps
```
모든 컨테이너가 `healthy` 상태여야 합니다.

### 2. Swagger UI 접속
브라우저에서 http://localhost:8080/swagger-ui/index.html 접속

---

## API 테스트 순서

### ✅ Step 0: Health Check

**목적**: 애플리케이션이 정상 실행 중인지 확인

#### 0-1. Health Check
- **Controller**: `health-check-controller`
- **Endpoint**: `GET /api/v1/health`
- **인증**: 불필요
- **테스트 방법**:
  1. `health-check-controller` 섹션 펼치기
  2. `GET /api/v1/health` 클릭
  3. "Try it out" 버튼 클릭
  4. "Execute" 버튼 클릭
  5. **예상 응답**: `200 OK` + "Application is healthy"

---

## 🔐 Step 1: 인증 (Authentication)

**목적**: 사용자 로그인 및 JWT 토큰 발급

### 중요 사항
⚠️ **실제 카카오 로그인은 브라우저 리다이렉트가 필요하므로 Swagger에서 완전한 테스트가 어렵습니다.**
테스트를 위해 **테스트용 API 또는 Mock 토큰**을 사용하거나, 실제 카카오 로그인 후 발급된 토큰을 사용하세요.

#### 1-1. 카카오 로그인 URL 조회
- **Controller**: `auth-controller`
- **Endpoint**: `GET /api/v1/auth/kakao/login-url`
- **인증**: 불필요
- **테스트 방법**:
  1. `auth-controller` 섹션 펼치기
  2. `GET /api/v1/auth/kakao/login-url` 클릭
  3. "Try it out" 버튼 클릭
  4. "Execute" 버튼 클릭
  5. **예상 응답**:
     ```json
     {
       "loginUrl": "https://kauth.kakao.com/oauth/authorize?..."
     }
     ```

#### 1-2. JWT 토큰 설정 (인증 필요한 API 테스트용)

**실제 환경에서는 카카오 로그인 후 발급된 토큰을 사용합니다.**
**테스트를 위해 다음 중 하나를 선택하세요:**

**방법 A: 실제 카카오 로그인**
1. 위에서 받은 `loginUrl`을 브라우저에 붙여넣기
2. 카카오 로그인 진행
3. 콜백 URL에서 `access_token` 복사
4. Swagger 우측 상단 "Authorize" 버튼 클릭
5. `BearerAuth (http, Bearer)` 필드에 토큰 입력
6. "Authorize" 버튼 클릭

**방법 B: 테스트 계정 생성 (개발 환경)**
- 별도의 테스트 API가 있다면 사용
- 또는 데이터베이스에 직접 테스트 계정 생성

#### 1-3. 토큰 갱신 (Optional)
- **Endpoint**: `POST /api/v1/auth/refresh`
- **Request Body**:
  ```json
  {
    "refreshToken": "your-refresh-token-here"
  }
  ```

---

## 📅 Step 2: 약속 생성 (Promise Creation)

**목적**: 새로운 약속 만들기

### 사전 조건
✅ JWT 토큰 설정 완료 (Step 1-2)

#### 2-1. 약속 생성
- **Controller**: `promise-controller`
- **Endpoint**: `POST /api/v1/promises`
- **인증**: 필요 ✅
- **Request Body**:
  ```json
  {
    "title": "강남역 저녁 약속",
    "description": "팀 회식",
    "promiseDateTime": "2026-01-15T19:00:00",
    "maxParticipants": 10
  }
  ```
- **테스트 방법**:
  1. `promise-controller` 섹션 펼치기
  2. `POST /api/v1/promises` 클릭
  3. "Try it out" 버튼 클릭
  4. Request body 입력
  5. "Execute" 버튼 클릭
  6. **예상 응답**: `201 Created`
  7. ⭐ **중요**: 응답에서 `id`와 `inviteCode` 값을 복사해두세요! (다음 단계에서 사용)

**응답 예시**:
```json
{
  "id": 1,
  "title": "강남역 저녁 약속",
  "description": "팀 회식",
  "promiseDateTime": "2026-01-15T19:00:00",
  "status": "CREATED",
  "inviteCode": "550e8400-e29b-41d4-a716-446655440000",
  "inviteExpiredAt": "2026-01-12T20:00:00",
  "maxParticipants": 10,
  "hostId": 1,
  "hostNickname": "홍길동",
  "createdAt": "2026-01-11T20:00:00"
}
```

#### 2-2. 내 약속 목록 조회
- **Endpoint**: `GET /api/v1/promises/my`
- **인증**: 필요 ✅
- **Query Parameters** (Optional):
  - `status`: CREATED, RECRUITING, WAITING_LOCATIONS, SELECTING_MIDPOINT, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED
  - `page`: 0
  - `size`: 20
- **테스트 방법**:
  1. `GET /api/v1/promises/my` 클릭
  2. "Try it out" 버튼 클릭
  3. 필요시 status 선택
  4. "Execute" 버튼 클릭

#### 2-3. 약속 상세 조회
- **Endpoint**: `GET /api/v1/promises/{promiseId}`
- **인증**: 필요 ✅
- **Path Parameter**: `promiseId` = 위에서 생성한 약속 ID (예: 1)
- **테스트 방법**:
  1. `GET /api/v1/promises/{promiseId}` 클릭
  2. "Try it out" 버튼 클릭
  3. `promiseId` 입력 (예: 1)
  4. "Execute" 버튼 클릭

---

## 👥 Step 3: 초대 및 참여 (Invitation & Participation)

**목적**: 다른 사용자를 약속에 초대하고 참여 처리

### 3-1. 초대 코드로 약속 조회
- **Controller**: `invite-controller`
- **Endpoint**: `GET /api/v1/invites/{inviteCode}`
- **인증**: 불필요
- **Path Parameter**: `inviteCode` = Step 2-1에서 복사한 초대 코드
- **테스트 방법**:
  1. `invite-controller` 섹션 펼치기
  2. `GET /api/v1/invites/{inviteCode}` 클릭
  3. "Try it out" 버튼 클릭
  4. `inviteCode` 입력 (예: 550e8400-e29b-41d4-a716-446655440000)
  5. "Execute" 버튼 클릭
  6. **예상 응답**: 약속 기본 정보

### 3-2. 약속 참여
- **Controller**: `promise-controller`
- **Endpoint**: `POST /api/v1/promises/{promiseId}/participants`
- **인증**: 필요 ✅
- **Path Parameter**: `promiseId` = 약속 ID
- **테스트 방법**:
  1. `promise-controller` 섹션 펼치기
  2. `POST /api/v1/promises/{promiseId}/participants` 클릭
  3. "Try it out" 버튼 클릭
  4. `promiseId` 입력
  5. "Execute" 버튼 클릭

**⚠️ 주의**: 같은 사용자(토큰)로는 중복 참여가 불가능합니다. 다른 사용자로 테스트하려면 다른 토큰이 필요합니다.

### 3-3. 참여자 목록 조회
- **Endpoint**: `GET /api/v1/promises/{promiseId}/participants`
- **인증**: 필요 ✅
- **Path Parameter**: `promiseId` = 약속 ID
- **테스트 방법**:
  1. `GET /api/v1/promises/{promiseId}/participants` 클릭
  2. "Try it out" 버튼 클릭
  3. `promiseId` 입력
  4. "Execute" 버튼 클릭

---

## 📍 Step 4: 출발지 입력 (Departure Location)

**목적**: 참여자의 출발 위치 등록 (중간지점 계산을 위해 필수)

### 사전 조건
✅ 약속에 참여한 상태 (Step 3-2 완료)

#### 4-1. 출발지 등록
- **Controller**: `promise-controller`
- **Endpoint**: `PUT /api/v1/promises/{promiseId}/participants/me/location`
- **인증**: 필요 ✅
- **Path Parameter**: `promiseId` = 약속 ID
- **Request Body**:
  ```json
  {
    "latitude": 37.4979,
    "longitude": 127.0276,
    "address": "서울특별시 강남구 역삼동"
  }
  ```
- **테스트 방법**:
  1. `PUT /api/v1/promises/{promiseId}/participants/me/location` 클릭
  2. "Try it out" 버튼 클릭
  3. `promiseId` 입력
  4. Request body 입력 (위도, 경도, 주소)
  5. "Execute" 버튼 클릭

**📝 좌표 샘플**:
- 강남역: `37.4979, 127.0276`
- 홍대입구역: `37.5572, 126.9229`
- 서울역: `37.5547, 126.9707`
- 잠실역: `37.5133, 127.1000`

#### 4-2. 출발지 조회
- **Endpoint**: `GET /api/v1/promises/{promiseId}/participants/me/location`
- **인증**: 필요 ✅
- **Path Parameter**: `promiseId` = 약속 ID
- **테스트 방법**:
  1. `GET /api/v1/promises/{promiseId}/participants/me/location` 클릭
  2. "Try it out" 버튼 클릭
  3. `promiseId` 입력
  4. "Execute" 버튼 클릭

#### 4-3. 약속 모집 마감 (모든 참여자가 모였을 때)
- **Endpoint**: `POST /api/v1/promises/{promiseId}/close-recruiting`
- **인증**: 필요 ✅ (호스트만 가능)
- **Path Parameter**: `promiseId` = 약속 ID
- **테스트 방법**:
  1. `POST /api/v1/promises/{promiseId}/close-recruiting` 클릭
  2. "Try it out" 버튼 클릭
  3. `promiseId` 입력
  4. "Execute" 버튼 클릭
  5. **결과**: 약속 상태가 `RECRUITING` → `WAITING_LOCATIONS`로 변경

---

## 🎯 Step 5: 중간지점 추천 및 확정 (Midpoint Recommendation)

**목적**: 참여자들의 출발지 기반으로 중간지점을 계산하고 최적의 만남 장소 확정

### 사전 조건
✅ 모든 참여자가 출발지 입력 완료 (Step 4-1)
✅ 약속 상태가 `SELECTING_MIDPOINT`

#### 5-1. 약속 상태 변경 (중간지점 선택 단계로)
- **Controller**: `promise-controller`
- **Endpoint**: `POST /api/v1/promises/{promiseId}/start-selecting-midpoint`
- **인증**: 필요 ✅ (호스트만 가능)
- **Path Parameter**: `promiseId` = 약속 ID
- **테스트 방법**:
  1. `promise-controller` 섹션에서 찾기
  2. `POST /api/v1/promises/{promiseId}/start-selecting-midpoint` 클릭
  3. "Try it out" 버튼 클릭
  4. `promiseId` 입력
  5. "Execute" 버튼 클릭
  6. **결과**: 약속 상태가 `WAITING_LOCATIONS` → `SELECTING_MIDPOINT`로 변경

#### 5-2. 중간지점 추천 조회
- **Controller**: `midpoint-controller`
- **Endpoint**: `GET /api/v1/promises/{promiseId}/midpoint/recommendations`
- **인증**: 필요 ✅
- **Path Parameter**: `promiseId` = 약속 ID
- **테스트 방법**:
  1. `midpoint-controller` 섹션 펼치기
  2. `GET /api/v1/promises/{promiseId}/midpoint/recommendations` 클릭
  3. "Try it out" 버튼 클릭
  4. `promiseId` 입력
  5. "Execute" 버튼 클릭
  6. **예상 응답**: 계산된 중간지점 + 가까운 지하철역 5개 추천
  7. ⭐ **중요**: 응답에서 원하는 역의 `stationId`를 복사해두세요!

**응답 예시**:
```json
{
  "calculatedMidpoint": {
    "latitude": 37.5665,
    "longitude": 126.9780
  },
  "recommendedStations": [
    {
      "stationId": 101,
      "stationName": "시청역",
      "lineName": "1호선",
      "distanceKm": 0.5,
      "averageDistanceKm": 3.2
    }
  ],
  "participantCount": 4
}
```

#### 5-3. 중간지점 확정 (호스트 전용)
- **Controller**: `midpoint-controller`
- **Endpoint**: `POST /api/v1/promises/{promiseId}/midpoint/confirm`
- **인증**: 필요 ✅ (호스트만 가능)
- **Path Parameter**: `promiseId` = 약속 ID
- **Request Body**:
  ```json
  {
    "stationId": 101
  }
  ```
- **테스트 방법**:
  1. `POST /api/v1/promises/{promiseId}/midpoint/confirm` 클릭
  2. "Try it out" 버튼 클릭
  3. `promiseId` 입력
  4. Request body에 선택한 `stationId` 입력 (Step 5-2에서 복사)
  5. "Execute" 버튼 클릭
  6. **결과**: 약속 상태가 `SELECTING_MIDPOINT` → `CONFIRMED`로 변경

---

## 📍 Step 6: 실시간 위치 공유 (Real-time Location)

**목적**: 약속 진행 중 참여자들의 실시간 위치 공유

### 사전 조건
✅ 약속 상태가 `CONFIRMED` 이상

#### 6-1. 약속 시작 (실시간 위치 공유 시작)
- **Controller**: `promise-controller`
- **Endpoint**: `POST /api/v1/promises/{promiseId}/start`
- **인증**: 필요 ✅
- **Path Parameter**: `promiseId` = 약속 ID
- **테스트 방법**:
  1. `POST /api/v1/promises/{promiseId}/start` 클릭
  2. "Try it out" 버튼 클릭
  3. `promiseId` 입력
  4. "Execute" 버튼 클릭
  5. **결과**: 약속 상태가 `CONFIRMED` → `IN_PROGRESS`로 변경

#### 6-2. 내 현재 위치 업데이트
- **Controller**: `location-tracking-controller`
- **Endpoint**: `POST /api/v1/promises/{promiseId}/location`
- **인증**: 필요 ✅
- **Path Parameter**: `promiseId` = 약속 ID
- **Request Body**:
  ```json
  {
    "latitude": 37.5000,
    "longitude": 127.0300
  }
  ```
- **테스트 방법**:
  1. `location-tracking-controller` 섹션 펼치기
  2. `POST /api/v1/promises/{promiseId}/location` 클릭
  3. "Try it out" 버튼 클릭
  4. `promiseId` 입력
  5. Request body 입력
  6. "Execute" 버튼 클릭

#### 6-3. 모든 참여자 위치 조회
- **Endpoint**: `GET /api/v1/promises/{promiseId}/locations`
- **인증**: 필요 ✅
- **Path Parameter**: `promiseId` = 약속 ID
- **테스트 방법**:
  1. `GET /api/v1/promises/{promiseId}/locations` 클릭
  2. "Try it out" 버튼 클릭
  3. `promiseId` 입력
  4. "Execute" 버튼 클릭
  5. **예상 응답**: 모든 참여자의 최신 위치 정보

---

## 🗺️ Step 7: 지도 데이터 (Map Data)

**목적**: 지하철역 데이터 및 지도 관련 정보 조회

#### 7-1. 지하철역 검색
- **Controller**: `map-data-controller`
- **Endpoint**: `GET /api/v1/map/stations/search`
- **인증**: 필요 ✅
- **Query Parameter**: `keyword` = 검색할 역 이름 (예: "강남")
- **테스트 방법**:
  1. `map-data-controller` 섹션 펼치기
  2. `GET /api/v1/map/stations/search` 클릭
  3. "Try it out" 버튼 클릭
  4. `keyword` 입력 (예: "강남")
  5. "Execute" 버튼 클릭

#### 7-2. 특정 지하철역 상세 정보
- **Endpoint**: `GET /api/v1/map/stations/{stationId}`
- **인증**: 필요 ✅
- **Path Parameter**: `stationId` = 역 ID
- **테스트 방법**:
  1. `GET /api/v1/map/stations/{stationId}` 클릭
  2. "Try it out" 버튼 클릭
  3. `stationId` 입력
  4. "Execute" 버튼 클릭

---

## 🏁 Step 8: 약속 완료 (Complete Promise)

**목적**: 약속 종료 처리

#### 8-1. 약속 완료
- **Controller**: `promise-controller`
- **Endpoint**: `POST /api/v1/promises/{promiseId}/complete`
- **인증**: 필요 ✅
- **Path Parameter**: `promiseId` = 약속 ID
- **테스트 방법**:
  1. `POST /api/v1/promises/{promiseId}/complete` 클릭
  2. "Try it out" 버튼 클릭
  3. `promiseId` 입력
  4. "Execute" 버튼 클릭
  5. **결과**: 약속 상태가 `IN_PROGRESS` → `COMPLETED`로 변경

#### 8-2. 약속 취소 (Optional)
- **Endpoint**: `DELETE /api/v1/promises/{promiseId}`
- **인증**: 필요 ✅ (호스트만 가능)
- **Path Parameter**: `promiseId` = 약속 ID
- **테스트 방법**:
  1. `DELETE /api/v1/promises/{promiseId}` 클릭
  2. "Try it out" 버튼 클릭
  3. `promiseId` 입력
  4. "Execute" 버튼 클릭
  5. **결과**: 약속 상태가 `CANCELLED`로 변경

---

## 📊 전체 플로우 요약

```
1. [인증] 카카오 로그인 → JWT 토큰 발급
         ↓
2. [약속 생성] POST /promises → 약속 ID, 초대 코드 받기
         ↓
3. [참여] POST /promises/{id}/participants
         ↓
4. [출발지] PUT /promises/{id}/participants/me/location
         ↓
5. [모집 마감] POST /promises/{id}/close-recruiting
         ↓
6. [중간지점 시작] POST /promises/{id}/start-selecting-midpoint
         ↓
7. [추천 조회] GET /promises/{id}/midpoint/recommendations
         ↓
8. [확정] POST /promises/{id}/midpoint/confirm (호스트)
         ↓
9. [약속 시작] POST /promises/{id}/start
         ↓
10. [위치 공유] POST /promises/{id}/location
         ↓
11. [약속 완료] POST /promises/{id}/complete
```

---

## 🐛 문제 해결 (Troubleshooting)

### 401 Unauthorized 에러
- JWT 토큰이 설정되지 않았거나 만료됨
- **해결**: Swagger 우측 상단 "Authorize" 버튼 클릭 후 토큰 재입력

### 403 Forbidden 에러
- 권한이 없는 작업 시도 (예: 호스트가 아닌데 확정 시도)
- **해결**: 해당 작업의 권한 확인 (호스트 전용인지 등)

### 400 Bad Request 에러
- 잘못된 요청 데이터
- **해결**: Request Body의 JSON 형식 확인, 필수 필드 누락 확인

### 404 Not Found 에러
- 존재하지 않는 리소스 요청
- **해결**: `promiseId`, `stationId` 등의 ID 값이 올바른지 확인

### 500 Internal Server Error
- 서버 내부 오류
- **해결**:
  1. 애플리케이션 로그 확인: `docker-compose logs -f app`
  2. 데이터베이스 상태 확인
  3. 필요시 컨테이너 재시작: `docker-compose restart app`

---

## 📝 테스트 체크리스트

완료한 항목에 체크하세요:

- [ ] Step 0: Health Check 확인
- [ ] Step 1: 인증 및 JWT 토큰 발급
- [ ] Step 2: 약속 생성 및 조회
- [ ] Step 3: 초대 코드로 조회 및 참여
- [ ] Step 4: 출발지 입력
- [ ] Step 5: 중간지점 추천 조회 및 확정
- [ ] Step 6: 실시간 위치 공유
- [ ] Step 7: 지도 데이터 조회
- [ ] Step 8: 약속 완료 처리

---

## ⚠️ 이 파일 삭제 시점

모든 API 테스트 완료 후 이 파일은 삭제하세요:
```bash
rm SWAGGER_TEST_GUIDE.md
```

---

**마지막 업데이트**: 2026-01-11
**작성자**: Claude Code
