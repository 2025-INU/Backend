# ADR-0004: 호스트 전용 모임 확정

**상태**: 승인됨
**날짜**: 2025-01-10
**결정자**: 개발팀, 제품 책임자

## 배경
AI 추천에서 최종 모임 장소를 선택할 때 애플리케이션은 의사 결정 메커니즘이 필요합니다. 옵션으로는 투표 시스템(다수결), 합의(만장일치), 호스트 권한이 있습니다. 이 결정은 UX 복잡성, 모임 확정 속도 및 의사결정 교착 상태 가능성에 영향을 미칩니다.

## 결정
호스트 전용 확정 권한 구현:

- **모임 생성자(호스트)만 최종 모임 장소를 확정할 수 있음**
- 참가자 간 투표 또는 합의 메커니즘 없음
- 참가자는 모든 AI 추천을 볼 수 있지만 투표하거나 선택에 영향을 미칠 수 없음
- 호스트가 추천 장소 중 하나를 선택하고 API 엔드포인트를 통해 확정

이는 명확한 의사결정 권한을 확립하고 확정 프로세스를 간소화합니다.

## 결과

**긍정적**:
- **빠른 의사결정**: 투표나 합의를 기다릴 필요 없이 호스트가 즉시 결정
- **간단한 UX**: 복잡한 투표 UI 없음, 명확한 권한 구조
- **교착 상태 방지**: 동점 없음, 정족수 요구사항 없음, 조정 오버헤드 없음
- **명확한 책임**: 호스트가 결정을 소유하여 그룹 우유부단함 감소
- **API 복잡성 감소**: 투표/집계 시스템 대신 단일 확정 엔드포인트

**부정적**:
- **민주주의 감소**: 참가자가 최종 선택에 공식적 의견을 낼 수 없음
- **잠재적 불만**: 일부 참가자가 호스트의 선택에 동의하지 않을 수 있음
- **호스트 압박**: 모든 결정 압박이 한 사람에게 집중됨
- **협력적 의사결정 없음**: 그룹 선호도를 반영할 수 없음

## 완화 전략
- 참가자는 외부 채널(KakaoTalk)을 통해 선호도를 전달할 수 있음
- AI 추천이 모든 참가자에게 투명하게 표시됨
- 호스트는 모든 참가자의 평균 거리를 확인하여 정보에 입각한 선택 가능
- 향후 기능: 확정을 차단하지 않는 선택적 피드백 메커니즘

## 구현

**API Endpoint** (Host Only):
```java
@PostMapping("/meetings/{meetingId}/midpoint/confirm")
@PreAuthorize("@meetingSecurityService.isHost(#meetingId, authentication)")
public ResponseEntity<MeetingResponse> confirmMidpoint(
    @PathVariable Long meetingId,
    @RequestBody ConfirmMidpointRequest request,
    Authentication authentication
) {
    Meeting meeting = meetingService.confirmMidpoint(
        meetingId,
        request.getSubwayStationId(),
        authentication.getName()
    );
    return ResponseEntity.ok(toResponse(meeting));
}
```

**Authorization Check**:
```java
public void confirmMidpoint(Long meetingId, Long stationId, String username) {
    Meeting meeting = findMeetingById(meetingId);

    // Host-only authorization
    if (!meeting.getHost().getUsername().equals(username)) {
        throw new UnauthorizedException("Only meeting host can confirm place");
    }

    // Status validation
    if (meeting.getStatus() != MeetingStatus.SELECTING_MIDPOINT) {
        throw new BadRequestException("Meeting not in SELECTING_MIDPOINT status");
    }

    // Confirm and update status
    meeting.confirmPlace(stationId);
    meeting.setStatus(MeetingStatus.CONFIRMED);
    meetingRepository.save(meeting);
}
```

## 고려된 대안

**Voting System (Majority Rule)**:
- 기각됨 - UI/API 복잡성 증가, 동점 가능성, 정족수 로직 필요
- UX 마찰: 참가자가 투표해야 하고, 다른 사람을 기다려야 하며, 교착 상태 처리 필요

**Consensus (Unanimous Agreement)**:
- 기각됨 - 높은 조정 비용, 매우 느림, 단일 거부권으로 전체 모임 차단
- 결정에 도달하지 못할 위험

**First-Come-First-Served**:
- 기각됨 - 불공평함, 신중한 결정보다 빠른 클릭을 보상
- 참가자 선호도 고려 제거

**Weighted Voting by Distance**:
- 기각됨 - 복잡한 알고리즘, 설명 어려움, 여전히 투표 인프라 필요
- 옵션에 가까운 참가자가 더 많은 가중치 - 공정성 문제 제기

## 관련 문서
- **ADR-0005**: Maximum 10 Participants Rule - 의사결정을 관리 가능하게 유지
- **ADR-0006**: Midpoint Caching - 확정 중 일관된 추천 보장
- **API Specification**: [../api/backend-api.md#post-meetingsmeetingidmidpointconfirm](../api/backend-api.md)
