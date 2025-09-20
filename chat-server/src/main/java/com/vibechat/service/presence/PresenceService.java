package com.vibechat.service.presence;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PresenceService {

    private static final Logger logger = LoggerFactory.getLogger(PresenceService.class);
    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String PRESENCE_SESSION_KEY_PREFIX = "presence:session:";
    private static final String PRESENCE_ROOM_KEY_PREFIX = "presence:room:";
    private static final String TYPING_KEY_PREFIX = "typing:room:";
    private static final String ROOM_NICKNAME_KEY_PREFIX = "nickname:room:";

    public void userConnected(String stompSessionId, Long userId, Long roomId, String nickname) {
        String sessionKey = PRESENCE_SESSION_KEY_PREFIX + stompSessionId;
        String roomSessionsKey = PRESENCE_ROOM_KEY_PREFIX + roomId + ":sessions";
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
        logger.info("User {} connected to room {}. Session: {}", userId, roomId, stompSessionId);
        broadcastPresence(roomId);
    }

    public void userDisconnected(String stompSessionId) {
        String sessionKey = PRESENCE_SESSION_KEY_PREFIX + stompSessionId;
        Map<Object, Object> sessionInfo = redisTemplate.opsForHash().entries(sessionKey);
        String roomIdStr = (String) sessionInfo.get("roomId");
        if (roomIdStr != null) {
            Long roomId = Long.parseLong(roomIdStr);
            String roomSessionsKey = PRESENCE_ROOM_KEY_PREFIX + roomId + ":sessions";
            redisTemplate.opsForSet().remove(roomSessionsKey, stompSessionId);
            logger.info("User disconnected from room {}. Session: {}", roomId, stompSessionId);
            broadcastPresence(roomId);
            String userIdStr = (String) sessionInfo.get("userId");
            String nickname = (String) sessionInfo.get("nickname");
            if (userIdStr != null) {
                try { updateTypingStatus(roomId, Long.parseLong(userIdStr), nickname, false); } catch (Exception ignored) {}
            }
            if (nickname != null) {
                String nicknameKey = ROOM_NICKNAME_KEY_PREFIX + roomId;
                redisTemplate.opsForSet().remove(nicknameKey, nickname);
            }
        }
        redisTemplate.delete(sessionKey);
    }

    private void broadcastPresence(Long roomId) {
        String roomSessionsKey = PRESENCE_ROOM_KEY_PREFIX + roomId + ":sessions";
        Long onlineCount = redisTemplate.opsForSet().size(roomSessionsKey);
        if (onlineCount == null) onlineCount = 0L;
        Map<String, Object> presenceEvent = Map.of("roomId", roomId, "count", onlineCount);
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/presence", presenceEvent);
        logger.info("Broadcasting presence for room {}: {} users online", roomId, onlineCount);
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
        Map<String, Object> typingEvent = Map.of(
            "roomId", roomId,
            "userId", userId,
            "nickname", nickname,
            "typing", isTyping,
            "ts", System.currentTimeMillis()
        );
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/typing", typingEvent);
        logger.info("Broadcasting typing status for room {}: user {} is {}typing", roomId, userId, isTyping ? "" : "not ");
    }
}


