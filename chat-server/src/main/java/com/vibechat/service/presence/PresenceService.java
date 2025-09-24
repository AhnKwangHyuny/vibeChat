package com.vibechat.service.presence;

import com.vibechat.service.messagequeue.MessageQueueService;
import com.vibechat.service.messagequeue.OfflineUserQueueHandler;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PresenceService {

    private static final Logger logger = LoggerFactory.getLogger(PresenceService.class);
    private final RedisTemplate<String, String> redisTemplate;
    private final MessageQueueService messageQueueService;
    private final OfflineUserQueueHandler offlineUserQueueHandler;

    private static final String PRESENCE_SESSION_KEY_PREFIX = "presence:session:";
    private static final String PRESENCE_ROOM_KEY_PREFIX = "presence:room:";
    private static final String TYPING_KEY_PREFIX = "typing:room:";
    private static final String ROOM_NICKNAME_KEY_PREFIX = "nickname:room:";
    private static final String PRESENCE_EVENT_KEY_PREFIX = "presence:event:";
    private static final String TYPING_EVENT_KEY_PREFIX = "typing:event:";

    public void userConnected(String stompSessionId, Long userId, Long roomId, String nickname) {
        String sessionKey = PRESENCE_SESSION_KEY_PREFIX + stompSessionId;
        String roomSessionsKey = PRESENCE_ROOM_KEY_PREFIX + roomId + ":sessions";

        // Redis Presence 정보 저장
        if (nickname != null) {
            redisTemplate.opsForHash().putAll(sessionKey, Map.of(
                "userId", String.valueOf(userId),
                "roomId", String.valueOf(roomId),
                "nickname", nickname
            ));
        } else {
            redisTemplate.opsForHash().putAll(sessionKey, Map.of(
                "userId", String.valueOf(userId),
                "roomId", String.valueOf(roomId)
            ));
        }
        redisTemplate.opsForSet().add(roomSessionsKey, stompSessionId);
        redisTemplate.expire(sessionKey, java.time.Duration.ofHours(1));

        // Redis Streams 메시지 큐 초기화
        messageQueueService.initializeRoomStream(roomId);
        messageQueueService.startStreamListener(roomId);

        // 오프라인 상태였다면 누락된 메시지 전달
        if (offlineUserQueueHandler.isUserOffline(userId)) {
            int deliveredMessages = offlineUserQueueHandler.processUserReconnection(userId);
            logger.info("Delivered {} pending messages to user {} upon reconnection", deliveredMessages, userId);
        }

        logger.info("User {} connected to room {} with Redis Streams support. Session: {}", userId, roomId, stompSessionId);
        broadcastPresence(roomId);
    }

    public void userDisconnected(String stompSessionId) {
        String sessionKey = PRESENCE_SESSION_KEY_PREFIX + stompSessionId;
        Map<Object, Object> sessionInfo = redisTemplate.opsForHash().entries(sessionKey);
        String roomIdStr = (String) sessionInfo.get("roomId");
        String userIdStr = (String) sessionInfo.get("userId");

        if (roomIdStr != null) {
            Long roomId = Long.parseLong(roomIdStr);
            String roomSessionsKey = PRESENCE_ROOM_KEY_PREFIX + roomId + ":sessions";
            redisTemplate.opsForSet().remove(roomSessionsKey, stompSessionId);

            // 유저를 오프라인 상태로 마킹 (Consumer Group DLQ 처리)
            if (userIdStr != null) {
                Long userId = Long.parseLong(userIdStr);
                offlineUserQueueHandler.registerOfflineUser(userId, roomId);

                // 타이핑 상태 정리
                String nickname = (String) sessionInfo.get("nickname");
                try {
                    updateTypingStatus(roomId, userId, nickname, false);
                } catch (Exception ignored) {}

                // 닉네임 정리 (방에서 완전히 나간 경우만)
                if (nickname != null) {
                    Long activeSessionsCount = redisTemplate.opsForSet().size(roomSessionsKey);
                    if (activeSessionsCount != null && activeSessionsCount == 0) {
                        // 방에 아무도 없으면 Stream 리스너 정지
                        messageQueueService.stopStreamListener(roomId);
                    }
                    String nicknameKey = ROOM_NICKNAME_KEY_PREFIX + roomId;
                    redisTemplate.opsForSet().remove(nicknameKey, nickname);
                }
            }

            logger.info("User disconnected from room {} and marked as offline. Session: {}", roomId, stompSessionId);
            broadcastPresence(roomId);
        }
        redisTemplate.delete(sessionKey);
    }

    private void broadcastPresence(Long roomId) {
        String roomSessionsKey = PRESENCE_ROOM_KEY_PREFIX + roomId + ":sessions";
        Long onlineCount = redisTemplate.opsForSet().size(roomSessionsKey);
        if (onlineCount == null) onlineCount = 0L;

        // Redis에 presence 이벤트 저장 (메시지 큐에서 읽어갈 수 있도록)
        String presenceEventKey = PRESENCE_EVENT_KEY_PREFIX + roomId;
        String eventData = String.format("{\"roomId\":%d,\"count\":%d,\"timestamp\":%d}",
            roomId, onlineCount, System.currentTimeMillis());

        redisTemplate.opsForValue().set(presenceEventKey, eventData);
        redisTemplate.expire(presenceEventKey, 5, TimeUnit.MINUTES); // 5분 후 자동 삭제

        logger.info("Published presence event for room {}: {} users online", roomId, onlineCount);
    }

    public void updateTypingStatus(Long roomId, Long userId, String nickname, boolean isTyping) {
        String typingKey = TYPING_KEY_PREFIX + roomId;
        String userTypingKey = userId.toString();
        if (isTyping) {
            redisTemplate.opsForSet().add(typingKey, userTypingKey);
            redisTemplate.expire(typingKey, 3, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForSet().remove(typingKey, userTypingKey);
        }
        broadcastTypingStatus(roomId, userId, nickname, isTyping);
    }

    private void broadcastTypingStatus(Long roomId, Long userId, String nickname, boolean isTyping) {
        // Redis에 typing 이벤트 저장 (메시지 큐에서 읽어갈 수 있도록)
        String typingEventKey = TYPING_EVENT_KEY_PREFIX + roomId + ":" + userId;
        String eventData = String.format("{\"roomId\":%d,\"userId\":%d,\"nickname\":\"%s\",\"typing\":%s,\"timestamp\":%d}",
            roomId, userId, nickname, isTyping, System.currentTimeMillis());

        if (isTyping) {
            redisTemplate.opsForValue().set(typingEventKey, eventData);
            redisTemplate.expire(typingEventKey, 5, TimeUnit.SECONDS); // 5초 후 자동 삭제
        } else {
            // 타이핑 중지 시 즉시 삭제
            redisTemplate.delete(typingEventKey);
        }

        logger.info("Published typing event for room {}: user {} is {}typing", roomId, userId, isTyping ? "" : "not ");
    }
}


