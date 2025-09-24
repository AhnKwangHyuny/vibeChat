package com.vibechat.service.messagequeue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibechat.dto.StreamMessageDto;
import com.vibechat.event.MessageBroadcastEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Redis Streams 메시지를 수신하여, 다른 서비스가 처리할 수 있도록 내부 이벤트를 발행하는 프로세서.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreamMessageProcessor {

    private final ApplicationEventPublisher eventPublisher;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PRESENCE_ROOM_KEY_PREFIX = "presence:room:";

    /**
     * 스트림에서 받은 메시지를 처리하는 핵심 로직.
     * @param messageDto 처리할 메시지 DTO
     * @param messageId 스트림 내 메시지 ID
     */
    public void processMessage(StreamMessageDto messageDto, String messageId) {
        try {
            Long roomId = messageDto.getRoomId();
            if (roomId == null) {
                log.warn("Room ID is null in message: {}", messageId);
                return;
            }

            Map<String, Object> messageData = objectMapper.convertValue(messageDto, Map.class);

            processMessageByUserStatus(roomId, messageData, messageId);

        } catch (Exception e) {
            log.error("Failed to process message from Redis Stream: {}", e.getMessage(), e);
        }
    }

    private void processMessageByUserStatus(Long roomId, Map<String, Object> messageData, String messageId) {
        Set<String> onlineSessionIds = getOnlineSessionIds(roomId);

        if (!onlineSessionIds.isEmpty()) {
            broadcastToOnlineUsers(roomId, messageData);
            log.info("Broadcast event published for message {} to {} online users in room {}",
                    messageId, onlineSessionIds.size(), roomId);
        }

        handleOfflineUsers(roomId, messageData, messageId);
    }

    private Set<String> getOnlineSessionIds(Long roomId) {
        String roomSessionsKey = PRESENCE_ROOM_KEY_PREFIX + roomId + ":sessions";
        return redisTemplate.opsForSet().members(roomSessionsKey);
    }

    /**
     * 메시지 브로드캐스트가 필요하다는 내부 이벤트를 발행합니다.
     */
    private void broadcastToOnlineUsers(Long roomId, Map<String, Object> messageData) {
        try {
            String destination = "/topic/rooms/" + roomId + "/messages";
            // 직접 메시지를 보내는 대신, 이벤트를 발행합니다.
            eventPublisher.publishEvent(new MessageBroadcastEvent(this, destination, messageData));
            log.debug("Published MessageBroadcastEvent for destination: {}", destination);
        } catch (Exception e) {
            log.error("Failed to publish broadcast event for room {}: {}", roomId, e.getMessage(), e);
        }
    }

    private void handleOfflineUsers(Long roomId, Map<String, Object> messageData, String messageId) {
        // TODO: 오프라인 유저 처리를 위한 로직 (DLQ, Push Notification 등)
        log.debug("Message {} available in Consumer Group for offline users in room {}",
                 messageId, roomId);
    }
}