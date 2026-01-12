# ADR-0007: AI 추천 임시 저장

**상태**: 승인됨
**날짜**: 2025-01-10
**결정자**: 개발팀

## 배경
AI 추천 서버는 계산된 중간 지점을 기반으로 장소 제안을 제공합니다. 이러한 추천에는 다음이 포함됩니다:
- 장소 세부 정보 (이름, 주소, 좌표)
- AI 생성 점수 및 순위
- 리뷰 요약

동일한 모임에 대해 AI 서버를 반복적으로 호출하는 것은:
- 비용이 많이 듦 (외부 API 비용)
- 느림 (네트워크 지연 + AI 처리 시간)
- 불필요함 (동일한 중간 지점에 대해 결과가 변경되지 않음)

시스템은 캐싱 효율성과 데이터 신선도 간의 균형을 맞춰야 합니다.

## 결정
`ai_place_recommendations` 테이블에 AI 추천을 임시로 저장:

- **저장 기간**: 초기 생성부터 모임 확정까지
- **범위**: 모임별, 특정 중간 지점에 연결됨
- **정리**: 호스트가 최종 장소를 확정한 후 자동 삭제
- **검색**: AI 서버를 다시 호출하는 대신 데이터베이스에서 읽기
- **무효화**: 중간 지점이 변경되면 지움 (드물게 발생)

이는 지속적인 외부 API 비용 없이 빠른 액세스를 제공합니다.

## 결과

**긍정적**:
- **비용 절감**: AI API가 모임당 한 번만 호출됨
- **성능**: 데이터베이스 읽기 (< 10ms) 대 AI 서버 호출 (1-3초)
- **신뢰성**: AI 서버가 일시적으로 사용 불가능해도 작동
- **일관성**: 모든 참가자가 동일한 추천을 봄
- **간단한 로직**: 서비스 레이어는 데이터베이스에서만 읽음

**부정적**:
- **저장 비용**: 잠재적으로 많은 행이 있는 추가 테이블
- **정리 필요**: 확정 후 추천 삭제 필요
- **오래된 데이터 위험**: AI 모델이 업데이트되면 캐시된 결과가 오래됨
- **데이터베이스 크기**: 적절한 정리 없이 커질 수 있음

## 구현

**Database Schema**:
```sql
CREATE TABLE ai_place_recommendations (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    place_id VARCHAR(100),
    place_name VARCHAR(200) NOT NULL,
    place_address VARCHAR(500),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    ai_score DECIMAL(5, 2),
    ranking INTEGER,
    review_summary TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_recommendations_meeting ON ai_place_recommendations(meeting_id);
```

**Entity**:
```java
@Entity
@Table(name = "ai_place_recommendations")
public class AiPlaceRecommendationsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private MeetingsEntity meeting;

    private String placeId;
    private String placeName;
    private String placeAddress;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal aiScore;
    private Integer ranking;
    private String reviewSummary;
}
```

**Service Logic**:
```java
@Service
public class AiRecommendationService {
    public List<PlaceRecommendation> getRecommendations(Long meetingId) {
        // Check if recommendations already cached
        List<AiPlaceRecommendation> cached =
            recommendationRepository.findByMeetingId(meetingId);

        if (!cached.isEmpty()) {
            return cached.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        }

        // Call AI server for new recommendations
        Coordinate midpoint = getMeetingMidpoint(meetingId);
        List<PlaceRecommendation> recommendations =
            aiServerClient.getRecommendations(midpoint);

        // Cache in database
        recommendations.forEach(rec -> {
            AiPlaceRecommendation entity = new AiPlaceRecommendation();
            entity.setMeeting(meeting);
            entity.setPlaceName(rec.getName());
            entity.setAiScore(rec.getScore());
            // ... set other fields
            recommendationRepository.save(entity);
        });

        return recommendations;
    }

    public void cleanupRecommendations(Long meetingId) {
        // Called after host confirms place
        recommendationRepository.deleteByMeetingId(meetingId);
    }
}
```

## 정리 전략

**자동 정리 트리거**:
1. 호스트가 최종 장소를 확정한 후 (즉시 정리)
2. 모임이 취소된 경우 (외래 키를 통한 cascade delete)
3. 예약된 작업: 완료된 모임의 추천을 30일 이상 경과 시 제거

**수동 정리**:
```java
@Scheduled(cron = "0 0 2 * * *")  // Daily at 2 AM
public void cleanupOldRecommendations() {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
    recommendationRepository.deleteOldRecommendations(cutoff);
}
```

## 고려된 대안

**No Caching**: 기각됨 - 비용이 많이 드는 반복 AI 호출, 느린 UX, 높은 비용
**Redis Cache**: 기각됨 - 인프라 추가, 이 사용 사례에 불필요
**Permanent Storage**: 기각됨 - 불필요한 장기 저장, 데이터베이스 비대
**In-Memory Cache**: 기각됨 - 재시작 시 손실, 인스턴스 간 공유 안 됨

## 관련 문서
- **ADR-0002**: Cascade Delete - 모임 삭제 시 추천 자동 삭제
- **ADR-0006**: Midpoint Caching - 성능을 위한 유사한 캐싱 패턴
