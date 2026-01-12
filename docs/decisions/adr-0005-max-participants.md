# ADR-0005: 최대 10명 참가자 규칙

**상태**: 승인됨
**날짜**: 2025-01-10
**결정자**: 개발팀, 제품 책임자

## 배경
참가자 수가 증가함에 따라 중간 지점 계산 알고리즘과 모임 조정이 더 복잡해집니다. 기능 사용성과 기술적 실행 가능성 및 사용자 경험 간의 균형을 맞춰야 합니다. 너무 많은 참가자는 다음을 초래합니다:
- 복잡한 중간 지점 계산
- 어려운 합의 구축
- 불량한 모임 조정
- 까다로운 위치 추적

## 결정
각 모임을 최대 10명의 참가자로 제한:

- **데이터베이스 및 애플리케이션 레이어에서 하드 제한 시행**
- 10명을 초과하는 초대 시도는 명확한 오류 메시지와 함께 거부됨
- 제한은 예외 없이 모든 모임 유형에 적용됨
- 모임 생성자(호스트)는 1명의 참가자로 계산됨

이는 실용적인 모임 조정과 기술적 성능의 균형을 맞추는 관리 가능한 그룹 크기를 만듭니다.

## 결과

**긍정적**:
- **실행 가능한 중간 지점 계산**: 10개 위치 평균이 빠르게 계산되고, 합리적인 지리적 분포
- **더 나은 UX**: 작은 그룹은 조정이 쉽고, 커뮤니케이션 오버헤드 감소
- **성능**: 모임당 제한된 데이터베이스 행, 더 빠른 쿼리 및 계산
- **명확한 기대**: 사용자가 몇 명이 참여할 수 있는지 미리 알 수 있음
- **남용 방지**: 리소스를 압박하는 대규모 모임 생성 중지

**부정적**:
- **대규모 그룹 제한**: 10명 이상의 팀은 여러 모임으로 분할해야 함
- **유연성 부족**: 특수한 경우에 제한을 늘릴 수 있는 옵션 없음
- **비즈니스 제한**: 대규모 이벤트나 컨퍼런스를 지원할 수 없음
- **기능 요청 위험**: 사용자가 더 높은 제한을 자주 요청할 수 있음

## 완화 전략
- 해결 방법 문서화: 대규모 그룹을 위해 여러 모임 생성
- 향후 기능: 조직을 위한 "하위 모임" 또는 모임 그룹
- 제한을 설명하고 대안을 제안하는 명확한 오류 메시지
- 제한에 도달한 빈도를 추적하는 분석 (향후 결정 정보 제공)

## 구현

**Entity Constraint**:
```java
@Entity
@Table(name = "meetings")
public class MeetingsEntity {
    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants = 10;

    // Business logic
    public void validateParticipantLimit(int currentCount) {
        if (currentCount >= maxParticipants) {
            throw new MaxParticipantsExceededException(
                "Meeting has reached maximum participants (" + maxParticipants + ")"
            );
        }
    }
}
```

**Service Layer Validation**:
```java
@Service
public class MeetingService {
    public void joinMeeting(String inviteCode, Long userId) {
        Meeting meeting = findByInviteCode(inviteCode);

        // Check participant count
        long currentCount = meeting.getParticipants().size();
        if (currentCount >= meeting.getMaxParticipants()) {
            throw new MaxParticipantsExceededException(
                "This meeting is full (10/10 participants). " +
                "Please ask the host to create an additional meeting."
            );
        }

        // Add participant
        Participant participant = new Participant(meeting, user);
        participantRepository.save(participant);
    }
}
```

**API Error Response**:
```json
{
  "error": "MAX_PARTICIPANTS_EXCEEDED",
  "message": "This meeting is full (10/10 participants)",
  "maxParticipants": 10,
  "currentParticipants": 10,
  "suggestion": "Please ask the host to create an additional meeting for your group."
}
```

## 고려된 대안

**No Limit**: 기각됨 - 기술적 복잡성, 성능 문제, 대규모에서 불량한 UX
**20 Participants**: 기각됨 - 중간 지점이 덜 의미있어지고, 조정이 너무 복잡함
**5 Participants**: 기각됨 - 너무 제한적이며, 일반적인 그룹 크기를 지원하지 않음
**Configurable Limit**: 기각됨 - 복잡성 증가, 대부분의 사용자는 어차피 변경하지 않을 것
**Tiered Limits** (free=5, paid=20): 기각됨 - MVP를 위한 청구 복잡성 증가

## 관련 문서
- **ADR-0004**: Host-Only Meeting Confirmation - 더 큰 그룹은 합의를 더욱 어렵게 만들 것
- **ADR-0006**: Midpoint Caching - 10명의 참가자로 계산하기 위한 성능 최적화
