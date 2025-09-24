package com.vibechat.websocket;

import com.vibechat.service.presence.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.Objects;

/**
 * 방 입장/퇴장 처리를 위한 WebSocket 컨트롤러
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class RoomPresenceController {

    private final PresenceService presenceService;

    @MessageMapping("/rooms/{roomId}/join")
    public void joinRoom(@DestinationVariable Long roomId, SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttrs = Objects.requireNonNull(headerAccessor.getSessionAttributes());
        Long userId = (Long) sessionAttrs.get("userId");
        String nickname = (String) sessionAttrs.get("nickname");
        String sessionId = headerAccessor.getSessionId();

        if (userId != null && nickname != null && sessionId != null) {
            log.info("User {} joining room {} via WebSocket", userId, roomId);
            presenceService.userConnected(sessionId, userId, roomId, nickname);
        } else {
            log.warn("Invalid join request - userId: {}, nickname: {}, sessionId: {}", userId, nickname, sessionId);
        }
    }

    @MessageMapping("/rooms/{roomId}/leave")
    public void leaveRoom(@DestinationVariable Long roomId, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();

        if (sessionId != null) {
            log.info("User leaving room {} via WebSocket", roomId);
            presenceService.userDisconnected(sessionId);
        }
    }
}