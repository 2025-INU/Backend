package dev.promise4.GgUd.controller;

import dev.promise4.GgUd.controller.dto.LocationUpdateMessage;
import dev.promise4.GgUd.controller.dto.ParticipantLocationResponse;
import dev.promise4.GgUd.entity.Participant;
import dev.promise4.GgUd.repository.ParticipantRepository;
import dev.promise4.GgUd.service.LocationTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * 실시간 위치 공유 WebSocket 컨트롤러
 */
@Slf4j
@Controller
@RestController
@RequestMapping("/api/v1/promises")
@RequiredArgsConstructor
@Tag(name = "Location", description = "실시간 위치 및 도착 현황 API")
public class LocationTrackingController {

    private final LocationTrackingService locationTrackingService;
    private final ParticipantRepository participantRepository;

    /**
     * 도착 현황 조회
     * GET /api/v1/promises/{promiseId}/arrivals
     */
    @GetMapping("/{promiseId}/arrivals")
    @Operation(summary = "도착 현황 조회", description = "약속 장소에 도착한 참여자 현황을 조회합니다. (n/전체)")
    public ResponseEntity<Map<String, Object>> getArrivalStatus(@PathVariable Long promiseId) {
        List<Participant> participants = participantRepository.findByPromiseId(promiseId);

        long arrivedCount = participants.stream().filter(Participant::isArrived).count();
        long totalCount = participants.size();

        List<Map<String, Object>> participantStatus = participants.stream()
                .map(p -> Map.<String, Object>of(
                        "userId", p.getUser().getId(),
                        "nickname", p.getUser().getNickname(),
                        "arrived", p.isArrived(),
                        "arrivedAt", p.getArrivedAt() != null ? p.getArrivedAt().toString() : null
                ))
                .toList();

        return ResponseEntity.ok(Map.of(
                "arrivedCount", arrivedCount,
                "totalCount", totalCount,
                "participants", participantStatus
        ));
    }

    /**
     * 위치 업데이트 수신 및 브로드캐스트
     * 
     * 클라이언트 → /app/promises/{promiseId}/location
     * 서버 → /topic/promises/{promiseId}/locations
     */
    @MessageMapping("/promises/{promiseId}/location")
    @SendTo("/topic/promises/{promiseId}/locations")
    public LocationUpdateMessage updateLocation(
            @DestinationVariable Long promiseId,
            @Payload LocationUpdateRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        Principal principal = headerAccessor.getUser();
        if (principal == null) {
            throw new IllegalStateException("인증되지 않은 사용자입니다");
        }

        Long userId = Long.parseLong(principal.getName());

        return locationTrackingService.updateLocation(
                promiseId,
                userId,
                request.getLatitude(),
                request.getLongitude());
    }

    /**
     * 위치 업데이트 요청 내부 DTO
     */
    @lombok.Getter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LocationUpdateRequest {
        private double latitude;
        private double longitude;
    }
}
