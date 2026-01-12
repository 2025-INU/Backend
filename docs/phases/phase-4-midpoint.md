# Phase 4: 중간지점 추천 시스템

## 목표 (Goals)
- 지하철역 CSV 데이터 로드 시스템 구현
- 중간지점 계산 알고리즘 구현 (평균 좌표 + Haversine 공식)
- AI 서버 연동하여 장소 추천 받기
- 호스트 전용 장소 확정 API 구현

## Step 4.1: 지하철역 데이터 로딩

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
   - CSV 파일 경로: src/main/resources/data/seoul_subway_stations.csv
   - 컬럼: station_name, line_name, latitude, longitude

3. SubwayStationRepository:
   - findAll()
   - findByStationNameContaining()

CSV 파일 예시도 만들어줘 (서울 주요 지하철역 10개 정도).
```

## Step 4.2: 중간지점 계산 서비스 구현

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

**참고**: [ADR-0006](../decisions/adr-0006-midpoint-caching.md) - 중간지점 캐싱 전략

## Step 4.3: 중간지점 추천 API 구현

**Claude에게 지시:**
```
중간지점 추천 API를 구현해줘:

1. MidpointController:
   - GET /api/v1/meetings/{meetingId}/midpoint/recommendations
     - 모든 참여자 출발지 기반 중간지점 계산
     - 가까운 지하철역 3~5개 반환
     - 각 역까지의 평균 거리 포함
     - 모든 참여자가 조회 가능 (게스트도 확인 가능)

2. 비즈니스 로직:
   - 약속 상태가 SELECTING_MIDPOINT일 때만 조회 가능
   - 모든 참여자가 출발지를 입력해야 조회 가능
   - 약속 참여자만 조회 가능

3. Response DTO:
   - MidpointRecommendationResponse
     - calculatedMidpoint (Coordinate)
     - recommendedStations (List<StationRecommendation>)
       - stationId, stationName, lineName, latitude, longitude
       - distanceFromMidpoint, averageDistanceFromParticipants

테스트 및 Swagger 문서화 포함해줘.
```

**API 상세**: [../api/backend-api.md#midpoint-apis](../api/backend-api.md)

## Step 4.4: 중간지점 확정 API 구현

**Claude에게 지시:**
```
호스트가 중간지점을 확정하는 API를 구현해줘:

1. API:
   - POST /api/v1/meetings/{meetingId}/midpoint/confirm
     - 호스트만 가능
     - 추천된 지하철역 목록 중 하나를 호스트가 직접 선택하여 확정
     - 약속 상태를 CONFIRMED로 변경
     - 참여자들의 투표나 동의 없이 호스트 단독으로 결정

2. Request DTO:
   - ConfirmMidpointRequest
     - subwayStationId (호스트가 선택한 지하철역 ID)

3. 확정 시 Meeting 엔티티 업데이트:
   - confirmedLatitude, confirmedLongitude
   - confirmedPlaceName (역 이름)
   - status를 CONFIRMED로 변경

4. 권한 검증:
   - 호스트가 아닌 경우 UnauthorizedException 발생
   - 약속 상태가 SELECTING_MIDPOINT가 아닌 경우 예외 발생
   - 선택한 stationId가 추천 목록에 있는지 검증

테스트 포함해줘.
```

**참고**: [ADR-0004](../decisions/adr-0004-host-only-confirmation.md) - 호스트 전용 확정 권한

## Validation Checklist
- [ ] CSV 데이터 로딩 성공 및 SubwayStation 테이블 채워짐
- [ ] 중간지점 계산 정확성 테스트 통과
- [ ] Haversine 거리 계산 정확성 검증
- [ ] 추천 API가 3-5개 역 반환
- [ ] 모든 참여자가 추천 조회 가능
- [ ] 호스트만 확정 가능 (권한 검증)
- [ ] 확정 후 약속 상태가 CONFIRMED로 변경
- [ ] Swagger UI에서 API 문서 확인 가능

## 다음 Phase
[phase-5-realtime.md](phase-5-realtime.md) - 실시간 기능 구현

## 관련 문서
- **API 명세**: [../api/backend-api.md#midpoint-apis](../api/backend-api.md)
- **ADR-0004**: [../decisions/adr-0004-host-only-confirmation.md](../decisions/adr-0004-host-only-confirmation.md)
- **ADR-0006**: [../decisions/adr-0006-midpoint-caching.md](../decisions/adr-0006-midpoint-caching.md)
- **현재 진행 상황**: [../_memory/current_state.md](../_memory/current_state.md)
