package com.vibechat.websocket;

import com.vibechat.service.presence.PresenceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class StompEventListener {

    private static final Logger logger = LoggerFactory.getLogger(StompEventListener.class);
    private final PresenceService presenceService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        logger.info("Received a new web socket connection: {}", event);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        logger.info("Socket disconnected: {}", sessionId);
        presenceService.userDisconnected(sessionId);
    }

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        logger.info("Received a new subscription to {}: {}", destination, event);

        if (destination != null && destination.matches("/topic/rooms/\\d+/messages")) {
            try {
                String[] parts = destination.split("/");
                Long roomId = Long.parseLong(parts[3]);
                String sessionId = headerAccessor.getSessionId();
                Long userId = (Long) Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("userId");
                String nickname = (String) Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("nickname");

                if (userId != null) {
                    presenceService.userConnected(sessionId, userId, roomId, nickname);
                } else {
                    logger.warn("Could not find userId in session attributes for subscription to {}", destination);
                }
            } catch (Exception e) {
                logger.error("Failed to handle subscription to destination: {}", destination, e);
            }
        }
    }
}
