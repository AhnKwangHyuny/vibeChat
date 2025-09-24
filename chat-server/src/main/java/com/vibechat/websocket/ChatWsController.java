package com.vibechat.websocket;

import com.vibechat.dto.SendMessagePayload;
import com.vibechat.dto.StreamMessageDto;
import com.vibechat.service.RateLimitService;
import com.vibechat.service.messagequeue.MessageStreamProducer;
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

    private final MessageStreamProducer messageStreamProducer;
    private final PresenceService presenceService;
    private final RateLimitService rateLimitService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/rooms/{roomId}/send")
    public void sendMessage(@DestinationVariable Long roomId,
                            @Payload SendMessagePayload payload,
                            SimpMessageHeaderAccessor headerAccessor) {

        Map<String, Object> sessionAttrs = Objects.requireNonNull(headerAccessor.getSessionAttributes());
        Long userId = (Long) sessionAttrs.get("userId");
        String nickname = (String) sessionAttrs.get("nickname");
        String avatarUrl = (String) sessionAttrs.get("avatarUrl");

        if (userId == null || nickname == null) {
            messagingTemplate.convertAndSendToUser(headerAccessor.getSessionId(), "/queue/errors",
                    Map.of("type", "UNAUTHORIZED", "title", "Unauthorized", "detail", "User not authenticated."));
            return;
        }

        if (!rateLimitService.tryConsume(userId, roomId)) {
            long waitNanos = rateLimitService.nanosToWait(userId, roomId);
            long waitSeconds = Math.max(1, waitNanos / 1_000_000_000);
            messagingTemplate.convertAndSendToUser(headerAccessor.getSessionId(), "/queue/errors",
                    Map.of("type", "RATE_LIMIT", "title", "Rate limit exceeded",
                            "detail", "Too many messages. Please retry after a short delay.", "retryAfterSeconds", waitSeconds));
            return;
        }

        // 1. Stream에 발행할 DTO 생성
        StreamMessageDto streamDto = StreamMessageDto.builder()
                .clientTempId(payload.getClientTempId())
                .roomId(roomId)
                .userId(userId)
                .nickname(nickname)
                .avatarUrl(avatarUrl)
                .type(payload.getType())
                .contentText(payload.getContentText())
                .mediaUrl(payload.getMediaUrl())
                .build();

        // 2. MessageStreamProducer를 통해 Redis Stream에 메시지 발행
        messageStreamProducer.publishMessage(streamDto);
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
