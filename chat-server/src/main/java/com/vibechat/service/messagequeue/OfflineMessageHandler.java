package com.vibechat.service.messagequeue;

import com.vibechat.dto.StreamMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.Record;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 오프라인 유저를 위한 메시지 처리 (DLQ - Dead Letter Queue)
 *
 * 기능:
 * 1. 연결이 끊어진 유저들의 미처리 메시지 관리
 * 2. 유저 재연결 시 누락된 메시지 전달
 * 3. DLQ 메시지 만료 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfflineMessageHandler {

    private final RedisTemplate<String, Object> redisStreamsTemplate;
    private final MessageQueueService messageQueueService;

    // 오프라인 유저 추적 (userId -> roomId)
    private final Map<Long, Long> offlineUsers = new ConcurrentHashMap<>();

    private static final String OFFLINE_CONSUMER_GROUP = "offline-group";

    /**
     * 유저가 오프라인 상태가 될 때 호출
     * Consumer Group에 유저를 등록하여 메시지 수신 대기 상태로 만듦
     *
     * @param userId 사용자 ID
     * @param roomId 방 ID
     */
    public void markUserOffline(Long userId, Long roomId) {
        try {
            offlineUsers.put(userId, roomId);

            // 오프라인 유저를 위한 Consumer 등록
            String consumerName = "offline-user-" + userId;
            messageQueueService.createConsumerGroup(roomId, OFFLINE_CONSUMER_GROUP);

            log.info("Marked user {} as offline in room {} with consumer: {}", userId, roomId, consumerName);

        } catch (Exception e) {
            log.error("Failed to mark user {} as offline in room {}: {}", userId, roomId, e.getMessage(), e);
        }
    }

    /**
     * 유저가 온라인 상태가 될 때 호출
     * 누락된 메시지들을 조회하고 전달
     *
     * @param userId 사용자 ID
     * @return 전달된 메시지 수
     */
    @Async
    public int deliverPendingMessages(Long userId) {
        Long roomId = offlineUsers.remove(userId);
        if (roomId == null) {
            log.debug("No offline state found for user: {}", userId);
            return 0;
        }

        try {
            String consumerName = "offline-user-" + userId;
            List<ObjectRecord<String, StreamMessageDto>> pendingMessages =
                    messageQueueService.getPendingMessages(roomId, OFFLINE_CONSUMER_GROUP, consumerName);

            if (!pendingMessages.isEmpty()) {
                log.info("Delivering {} pending messages to user {} in room {}",
                        pendingMessages.size(), userId, roomId);

                // 각 메시지를 개별적으로 처리
                for (ObjectRecord<String, StreamMessageDto> message : pendingMessages) {
                    deliverMessageToUser(userId, roomId, message);
                    acknowledgeMessage(roomId, OFFLINE_CONSUMER_GROUP, message.getId().getValue());
                }

                return pendingMessages.size();
            }

        } catch (Exception e) {
            log.error("Failed to deliver pending messages to user {} in room {}: {}",
                     userId, roomId, e.getMessage(), e);
        }

        return 0;
    }

    /**
     * 개별 메시지를 특정 유저에게 전달
     */
    private void deliverMessageToUser(Long userId, Long roomId, ObjectRecord<String, StreamMessageDto> message) {
        try {
            // 실제 구현에서는 WebSocket 세션을 찾아서 개별 전송
            // 현재는 로그만 출력
            log.info("Delivering message {} to user {} in room {}",
                    message.getId().getValue(), userId, roomId);

            // TODO: WebSocket 세션 매니저를 통해 특정 유저에게만 메시지 전송
            // sessionManager.sendToUser(userId, message.getValue());

        } catch (Exception e) {
            log.error("Failed to deliver message {} to user {}: {}",
                     message.getId().getValue(), userId, e.getMessage(), e);
        }
    }

    /**
     * Consumer Group에서 메시지 처리 완료 표시 (XACK)
     */
    private void acknowledgeMessage(Long roomId, String consumerGroup, String messageId) {
        try {
            String streamKey = "room:" + roomId + ":messages";
            redisStreamsTemplate.opsForStream().acknowledge(streamKey, consumerGroup, messageId);
            log.debug("Acknowledged message {} in stream {} for group {}", messageId, streamKey, consumerGroup);

        } catch (Exception e) {
            log.error("Failed to acknowledge message {} in room {}: {}", messageId, roomId, e.getMessage(), e);
        }
    }

    /**
     * 오프라인 상태인 유저 목록 조회
     */
    public Map<Long, Long> getOfflineUsers() {
        return Map.copyOf(offlineUsers);
    }

    /**
     * 특정 유저의 오프라인 상태 확인
     */
    public boolean isUserOffline(Long userId) {
        return offlineUsers.containsKey(userId);
    }

    /**
     * 만료된 오프라인 메시지 정리 (스케줄러에서 주기적 호출)
     */
    @Async
    public void cleanupExpiredOfflineMessages() {
        // TODO: 일정 시간(예: 7일) 이상 된 오프라인 메시지 삭제
        // Redis Streams의 XTRIM 명령어 활용
        log.debug("Cleaning up expired offline messages...");
    }
}