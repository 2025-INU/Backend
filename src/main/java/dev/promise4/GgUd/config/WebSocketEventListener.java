package dev.promise4.GgUd.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * WebSocket 연결/해제 이벤트 리스너
 */
@Slf4j
@Component
public class WebSocketEventListener {

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        Principal user = accessor.getUser();
        if (user != null) {
            log.info("WebSocket connected: sessionId={}, user={}", sessionId, user.getName());
        } else {
            log.info("WebSocket connected: sessionId={} (anonymous)", sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        Principal user = accessor.getUser();
        if (user != null) {
            log.info("WebSocket disconnected: sessionId={}, user={}", sessionId, user.getName());
        } else {
            log.info("WebSocket disconnected: sessionId={}", sessionId);
        }
    }
}
