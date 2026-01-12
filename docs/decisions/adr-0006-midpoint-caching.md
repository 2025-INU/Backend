# ADR-0006: 중간 지점 좌표 캐싱

**상태**: 승인됨
**날짜**: 2025-01-10
**결정자**: 개발팀

## 배경
여러 참가자 위치에서 중간 지점을 계산하는 것은 좌표 평균화 및 Haversine 거리 공식을 사용한 가장 가까운 지하철역 찾기를 포함합니다. 이 계산은:
- 계산 비용이 높음 (특히 10명의 참가자의 경우)
- 여러 번 수행됨 (참가자가 추천을 보거나, 호스트가 확정할 때)
- 결정론적임 (동일한 입력은 항상 동일한 출력 생성)
- 시간에 민감함 (활성 모임 조정 중 필요)

매 요청마다 재계산하는 것은 리소스를 낭비하고 지연 시간을 증가시킵니다.

## 결정
`meetings` 테이블에 직접 계산된 중간 지점 좌표 캐시:

- **필드**: `midpointLatitude`, `midpointLongitude` (DECIMAL 정밀도)
- **한 번 계산**: 모든 참가자가 출발 위치를 제출할 때
- **데이터베이스에 저장**: 모임 데이터와 함께 영구 캐시
- **재사용**: 모든 후속 요청은 캐시된 값 사용
- **무효화**: 참가자 위치가 변경되는 경우에만 재계산

이는 데이터 정확성을 유지하면서 중복 계산을 제거합니다.

## 결과

**긍정적**:
- **성능**: 거의 즉시 중간 지점 검색 (데이터베이스 읽기 대 계산)
- **일관성**: 모든 사용자가 조정 중 동일한 중간 지점을 봄
- **CPU 감소**: 반복적인 Haversine 계산 없음
- **간단한 로직**: API 호출은 캐시된 값만 반환
- **감사 추적**: 모임 완료 후에도 중간 지점 값 보존

**부정적**:
- **저장 오버헤드**: 모임당 두 개의 추가 DECIMAL 컬럼 (~16 바이트)
- **캐시 무효화**: 참가자가 위치를 변경하면 재계산 필요 (엣지 케이스)
- **오래된 데이터 위험**: 참가자 위치가 업데이트되면 캐시를 새로 고쳐야 함
- **추가 코드**: 재계산이 필요한 시점을 감지하는 로직

## 구현

**Database Schema**:
```sql
ALTER TABLE meetings
ADD COLUMN midpoint_latitude DECIMAL(10, 8),
ADD COLUMN midpoint_longitude DECIMAL(11, 8),
ADD COLUMN midpoint_calculated_at TIMESTAMP;
```

**Entity**:
```java
@Entity
public class MeetingsEntity {
    @Column(name = "midpoint_latitude", precision = 10, scale = 8)
    private BigDecimal midpointLatitude;

    @Column(name = "midpoint_longitude", precision = 11, scale = 8)
    private BigDecimal midpointLongitude;

    @Column(name = "midpoint_calculated_at")
    private LocalDateTime midpointCalculatedAt;

    public void cacheMidpoint(Coordinate midpoint) {
        this.midpointLatitude = BigDecimal.valueOf(midpoint.getLatitude());
        this.midpointLongitude = BigDecimal.valueOf(midpoint.getLongitude());
        this.midpointCalculatedAt = LocalDateTime.now();
    }

    public boolean isMidpointCached() {
        return midpointLatitude != null && midpointLongitude != null;
    }
}
```

**Service Logic**:
```java
@Service
public class MidpointService {
    public MidpointRecommendationResponse getRecommendations(Long meetingId) {
        Meeting meeting = findMeetingById(meetingId);

        // Use cached midpoint if available
        Coordinate midpoint;
        if (meeting.isMidpointCached()) {
            midpoint = new Coordinate(
                meeting.getMidpointLatitude(),
                meeting.getMidpointLongitude()
            );
        } else {
            // Calculate and cache
            List<Coordinate> departures = meeting.getParticipants()
                .stream()
                .map(p -> new Coordinate(p.getLatitude(), p.getLongitude()))
                .collect(Collectors.toList());

            midpoint = midpointCalculator.calculate(departures);
            meeting.cacheMidpoint(midpoint);
            meetingRepository.save(meeting);
        }

        // Find nearest stations using cached midpoint
        List<SubwayStation> nearest = stationService.findNearest(midpoint, 5);
        return new MidpointRecommendationResponse(midpoint, nearest);
    }
}
```

## 캐시 무효화 전략
다음의 경우 중간 지점 재계산:
- 참가자가 출발 위치를 추가/제거할 때
- 참가자가 출발 위치를 업데이트할 때
- 참가자가 모임을 떠날 때

트리거: 서비스 레이어 메서드 `invalidateMidpointCache()`가 캐시된 값을 null로 설정

## 고려된 대안

**Redis Caching**: 기각됨 - 인프라 복잡성 증가, 데이터베이스 캐시로 충분
**No Caching**: 기각됨 - 반복 계산으로 CPU 낭비
**Application Memory**: 기각됨 - 서버 재시작 시 손실, 인스턴스 간 공유 안 됨
**Calculate Always**: 기각됨 - 불필요한 부하, 불량한 사용자 경험

## 관련 문서
- **ADR-0005**: Maximum 10 Participants - 제한된 참가자로 캐싱이 더 효과적임
- **ADR-0007**: AI Recommendation Storage - 외부 API 결과에 대한 유사한 캐싱 전략
