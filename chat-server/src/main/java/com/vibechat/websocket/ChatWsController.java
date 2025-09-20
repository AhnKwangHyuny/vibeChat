package com.vibechat.websocket;

import com.vibechat.dto.SendMessagePayload;
import com.vibechat.service.message.MessageService;
import com.vibechat.service.RateLimitService;
import com.vibechat.service.presence.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final MessageService messageService;
    private final PresenceService presenceService;
    private final RateLimitService rateLimitService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/rooms/{roomId}/send")
    public void sendMessage(@DestinationVariable Long roomId, 
                            @Payload SendMessagePayload payload, 
                            SimpMessageHeaderAccessor headerAccessor) {
        
        Long userId = (Long) Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("userId");
        if (userId == null) {
            messagingTemplate.convertAndSendToUser(headerAccessor.getSessionId(), "/queue/errors",
                    java.util.Map.of(
                            "type", "UNAUTHORIZED",
                            "title", "Unauthorized",
                            "detail", "User not authenticated."
                    ));
            return;
        }

        // 사용자+방 단위 레이트 제한 검사
        if (!rateLimitService.tryConsume(userId, roomId)) {
            long waitNanos = rateLimitService.nanosToWait(userId, roomId);
            long waitSeconds = Math.max(1, waitNanos / 1_000_000_000);
            messagingTemplate.convertAndSendToUser(headerAccessor.getSessionId(), "/queue/errors",
                    java.util.Map.of(
                            "type", "RATE_LIMIT",
                            "title", "Rate limit exceeded",
                            "detail", "Too many messages. Please retry after a short delay.",
                            "retryAfterSeconds", waitSeconds
                    ));
            return;
        }

        messageService.saveAndBroadcastMessage(roomId, userId, payload);
    }

    @MessageMapping("/rooms/{roomId}/typing")
    public void handleTypingEvent(@DestinationVariable Long roomId,
                                  @Payload Map<String, Object> payload,
                                  SimpMessageHeaderAccessor headerAccessor) {
        Long userId = (Long) Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("userId");
        String nickname = (String) Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("nickname");
        Boolean isTyping = (Boolean) payload.get("typing");

        if (userId != null && nickname != null && isTyping != null) {
            presenceService.updateTypingStatus(roomId, userId, nickname, isTyping);
        }
    }
}
