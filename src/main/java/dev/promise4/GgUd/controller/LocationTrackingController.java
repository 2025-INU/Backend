package dev.promise4.GgUd.controller;

import dev.promise4.GgUd.controller.dto.LocationUpdateMessage;
import dev.promise4.GgUd.controller.dto.ParticipantLocationResponse;
import dev.promise4.GgUd.service.LocationTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * 실시간 위치 공유 WebSocket 컨트롤러
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class LocationTrackingController {

    private final LocationTrackingService locationTrackingService;

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
