package com.vibechat.websocket;

import com.vibechat.dto.SendMessagePayload;
import com.vibechat.dto.coordinator.MessageProcessResult;
import com.vibechat.service.coordinator.MessageCoordinatorService;
import com.vibechat.service.presence.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ChatWsController {

    private final MessageCoordinatorService messageCoordinatorService;
    private final PresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/rooms/{roomId}/send")
    public void sendMessage(@DestinationVariable Long roomId,
                            @Payload SendMessagePayload payload,
                            SimpMessageHeaderAccessor headerAccessor) {

        Map<String, Object> sessionAttrs = Objects.requireNonNull(headerAccessor.getSessionAttributes());
        Long userId = (Long) sessionAttrs.get("userId");
        String nickname = (String) sessionAttrs.get("nickname");

        if (userId == null || nickname == null) {
            messagingTemplate.convertAndSendToUser(headerAccessor.getSessionId(), "/queue/errors",
                    Map.of("type", "UNAUTHORIZED", "title", "Unauthorized", "detail", "User not authenticated."));
            return;
        }

        try {
            log.debug("Processing message from WebSocket: roomId={}, userId={}, type={}",
                roomId, userId, payload.getType());

            // MessageCoordinatorService를 통한 완전한 메시지 처리 (검증, 강화, 발행)
            MessageProcessResult result = messageCoordinatorService.processRoomMessage(roomId, userId, payload);

            // 클라이언트에게 ACK 응답
            if (result.isSuccess()) {
                messagingTemplate.convertAndSendToUser(headerAccessor.getSessionId(), "/queue/ack",
                    Map.of(
                        "clientTempId", result.getClientTempId(),
                        "messageId", result.getMessageId(),
                        "status", "SUCCESS",
                        "processingTimeMs", result.getProcessingTimeMs()
                    ));

                log.info("Message processed successfully: messageId={}, clientTempId={}",
                    result.getMessageId(), result.getClientTempId());

            } else {
                messagingTemplate.convertAndSendToUser(headerAccessor.getSessionId(), "/queue/errors",
                    Map.of(
                        "clientTempId", payload.getClientTempId(),
                        "type", "MESSAGE_PROCESSING_FAILED",
                        "title", "Message Processing Failed",
                        "detail", result.getErrorMessage()
                    ));

                log.warn("Message processing failed: error={}, clientTempId={}",
                    result.getErrorMessage(), payload.getClientTempId());
            }

        } catch (Exception e) {
            log.error("Unexpected error in WebSocket message handler: roomId={}, userId={}",
                roomId, userId, e);

            messagingTemplate.convertAndSendToUser(headerAccessor.getSessionId(), "/queue/errors",
                Map.of(
                    "clientTempId", payload.getClientTempId(),
                    "type", "INTERNAL_ERROR",
                    "title", "Internal Server Error",
                    "detail", "An unexpected error occurred. Please try again."
                ));
        }
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
