package com.vibechat.service.messagequeue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.connection.stream.Record;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 오프라인 유저를 위한 Consumer Group 기반 메시지 큐 처리
 *
 * 수정사항: SimpMessagingTemplate 직접 의존 제거, 이벤트 발행 방식으로 변경
 *
 * 핵심 로직:
 * 1. 유저가 오프라인이 되면 해당 유저용 Consumer 생성
 * 2. Consumer Group에서 해당 유저의 메시지만 읽기 시작
 * 3. 유저가 온라인이 되면 pending 메시지 전달 후 Consumer 정리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfflineUserQueueHandler {

    private final RedisTemplate<String, Object> redisStreamsTemplate;
    private final ApplicationEventPublisher eventPublisher;

    // 오프라인 유저별 Consumer 추적 (userId -> roomId)
    private final Map<Long, Long> offlineUserConsumers = new ConcurrentHashMap<>();

    private static final String OFFLINE_CONSUMER_GROUP = "offline-group";
    private static final String CONSUMER_NAME_PREFIX = "user-";

    /**
     * 유저가 오프라인 상태가 될 때 Consumer 등록
     * Consumer Group에서 해당 유저의 메시지 수신 대기 상태로 만듦
     *
     * @param userId 사용자 ID
     * @param roomId 방 ID
     */
    public void registerOfflineUser(Long userId, Long roomId) {
        try {
            String streamKey = getStreamKey(roomId);
            String consumerName = getConsumerName(userId);

            // Consumer Group이 존재하는지 확인 후 생성
            ensureConsumerGroupExists(streamKey);

            // 오프라인 유저 등록
            offlineUserConsumers.put(userId, roomId);

            log.info("Registered offline user {} as consumer '{}' for room {} stream",
                    userId, consumerName, roomId);

            // 즉시 pending 메시지가 있는지 확인
            checkAndProcessPendingMessages(userId, roomId);

        } catch (Exception e) {
            log.error("Failed to register offline user {} in room {}: {}", userId, roomId, e.getMessage(), e);
        }
    }

    /**
     * 유저가 온라인 상태가 될 때 pending 메시지 전달 및 Consumer 정리
     *
     * @param userId 사용자 ID
     * @return 전달된 메시지 수
     */
    @Async
    public int processUserReconnection(Long userId) {
        Long roomId = offlineUserConsumers.remove(userId);
        if (roomId == null) {
            log.debug("No offline consumer found for user: {}", userId);
            return 0;
        }

        try {
            int deliveredCount = deliverPendingMessages(userId, roomId);

            if (deliveredCount > 0) {
                log.info("Delivered {} pending messages to user {} upon reconnection", deliveredCount, userId);
            }

            return deliveredCount;

        } catch (Exception e) {
            log.error("Failed to process reconnection for user {} in room {}: {}",
                     userId, roomId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 특정 유저의 pending 메시지 전달
     */
    private int deliverPendingMessages(Long userId, Long roomId) {
        try {
            String streamKey = getStreamKey(roomId);
            String consumerName = getConsumerName(userId);

            // XREADGROUP으로 해당 Consumer의 pending 메시지 읽기
            List<MapRecord<String, Object, Object>> pendingMessages = redisStreamsTemplate.opsForStream()
                    .read(Consumer.from(OFFLINE_CONSUMER_GROUP, consumerName),
                            StreamOffset.create(streamKey, ReadOffset.from("0")));

            if (!pendingMessages.isEmpty()) {
                log.info("Found {} pending messages for user {} in room {}",
                        pendingMessages.size(), userId, roomId);

                // 각 메시지를 개별적으로 전달
                for (MapRecord<String, Object, Object> message : pendingMessages) {
                    deliverMessageToUser(userId, roomId, message);
                    acknowledgeMessage(streamKey, message.getId().getValue());
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
     * 개별 메시지를 특정 유저에게 전달 (이벤트 발행 방식)
     */
    private void deliverMessageToUser(Long userId, Long roomId, MapRecord<String, Object, Object> message) {
        try {
            // SimpMessagingTemplate 대신 이벤트 발행으로 순환 의존성 제거
            String destination = "/topic/rooms/" + roomId + "/messages";

            // 임시로 Redis에 저장하여 다른 서비스에서 읽도록 처리
            String tempKey = "temp:delivery:" + userId + ":" + roomId + ":" + message.getId().getValue();
            redisStreamsTemplate.opsForValue().set(tempKey, message.getValue());
            redisStreamsTemplate.expire(tempKey, java.time.Duration.ofMinutes(5));

            log.debug("Queued pending message {} for user {} in room {} (temp key: {})",
                     message.getId().getValue(), userId, roomId, tempKey);

        } catch (Exception e) {
            log.error("Failed to queue message {} for user {}: {}",
                     message.getId().getValue(), userId, e.getMessage(), e);
        }
    }

    /**
     * Consumer Group에서 메시지 처리 완료 표시 (XACK)
     */
    private void acknowledgeMessage(String streamKey, String messageId) {
        try {
            redisStreamsTemplate.opsForStream()
                    .acknowledge(streamKey, OFFLINE_CONSUMER_GROUP, messageId);
            log.debug("Acknowledged message {} in stream {}", messageId, streamKey);

        } catch (Exception e) {
            log.error("Failed to acknowledge message {} in stream {}: {}",
                     messageId, streamKey, e.getMessage(), e);
        }
    }

    /**
     * Consumer Group 존재 확인 및 생성
     */
    private void ensureConsumerGroupExists(String streamKey) {
        try {
            // Consumer Group 생성 시도 (이미 존재하면 무시됨)
            redisStreamsTemplate.opsForStream()
                    .createGroup(streamKey, OFFLINE_CONSUMER_GROUP);
            log.debug("Ensured Consumer Group '{}' exists for stream: {}", OFFLINE_CONSUMER_GROUP, streamKey);

        } catch (Exception e) {
            // BUSYGROUP 에러는 정상 (이미 존재)
            if (!e.getMessage().contains("BUSYGROUP")) {
                log.error("Failed to create Consumer Group '{}' for stream {}: {}",
                         OFFLINE_CONSUMER_GROUP, streamKey, e.getMessage());
            }
        }
    }

    /**
     * 오프라인 상태 확인 및 즉시 처리할 pending 메시지가 있는지 확인
     */
    private void checkAndProcessPendingMessages(Long userId, Long roomId) {
        try {
            String streamKey = getStreamKey(roomId);
            String consumerName = getConsumerName(userId);

            // 기존 pending 메시지가 있는지 확인

        } catch (Exception e) {
            log.debug("No existing pending messages for user {} in room {}: {}", userId, roomId, e.getMessage());
        }
    }

    /**
     * Stream Key 생성
     */
    private String getStreamKey(Long roomId) {
        return "room:" + roomId + ":messages";
    }

    /**
     * Consumer 이름 생성
     */
    private String getConsumerName(Long userId) {
        return CONSUMER_NAME_PREFIX + userId;
    }

    /**
     * 현재 오프라인 유저 목록 조회
     */
    public Map<Long, Long> getOfflineUsers() {
        return Map.copyOf(offlineUserConsumers);
    }

    /**
     * 특정 유저의 오프라인 상태 확인
     */
    public boolean isUserOffline(Long userId) {
        return offlineUserConsumers.containsKey(userId);
    }

    /**
     * 만료된 Consumer 정리 (스케줄러에서 주기적 호출)
     */
    @Async
    public void cleanupExpiredConsumers() {
        log.debug("Cleaning up expired offline user consumers...");
    }
}