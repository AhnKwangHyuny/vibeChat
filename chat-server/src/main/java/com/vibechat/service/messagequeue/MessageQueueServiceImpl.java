package com.vibechat.service.messagequeue;

import com.vibechat.dto.StreamMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis Streams 기반 메시지 큐 서비스 구현
 *
 * Stream 네이밍 패턴:
 * - Messages: room:{roomId}:messages
 * - Consumer Groups: room:{roomId}:broadcast-group, room:{roomId}:offline-group
 */
@Slf4j
@Service
public class MessageQueueServiceImpl implements MessageQueueService {

    private final RedisTemplate<String, Object> redisStreamsTemplate;
    private final StreamMessageListenerContainer<String, ObjectRecord<String, StreamMessageDto>> listenerContainer;
    private final StreamMessageProcessor streamMessageProcessor;

    // Room별 활성 리스너 추적
    private final Map<Long, Subscription> activeListeners =
            new ConcurrentHashMap<>();

    public MessageQueueServiceImpl(RedisTemplate<String, Object> redisStreamsTemplate,
                                 StreamMessageListenerContainer<String, ObjectRecord<String, StreamMessageDto>> listenerContainer,
                                 StreamMessageProcessor streamMessageProcessor) {
        this.redisStreamsTemplate = redisStreamsTemplate;
        this.listenerContainer = listenerContainer;
        this.streamMessageProcessor = streamMessageProcessor;
    }

    @Override
    public String addMessage(Long roomId, Object messageData) {
        String streamKey = getStreamKey(roomId);

        try {
            // Redis Streams에 메시지 추가 (XADD)
            Map<String, Object> messageMap;
            if (messageData instanceof Map) {
                messageMap = (Map<String, Object>) messageData;
            } else {
                messageMap = Map.of("data", messageData);
            }

            RecordId recordId = redisStreamsTemplate.opsForStream()
                    .add(streamKey, messageMap);

            log.info("Message added to stream {} with ID: {}", streamKey, recordId.getValue());
            return recordId.getValue();

        } catch (Exception e) {
            log.error("Failed to add message to stream {}: {}", streamKey, e.getMessage(), e);
            throw new RuntimeException("Failed to add message to stream", e);
        }
    }

    @Override
    public void initializeRoomStream(Long roomId) {
        String streamKey = getStreamKey(roomId);

        try {
            // Stream이 존재하는지 확인
            if (!streamExists(streamKey)) {
                // 빈 메시지로 Stream 초기화
                Map<String, Object> initMessage = Map.of("init", "true");
                redisStreamsTemplate.opsForStream().add(streamKey, initMessage);
                log.info("Initialized new stream: {}", streamKey);
            }

            // Consumer Groups 생성
            createConsumerGroup(roomId, "broadcast-group");
            createConsumerGroup(roomId, "offline-group");

        } catch (Exception e) {
            log.error("Failed to initialize stream {}: {}", streamKey, e.getMessage(), e);
        }
    }

    @Override
    public void createConsumerGroup(Long roomId, String groupName) {
        String streamKey = getStreamKey(roomId);

        try {
            // Consumer Group이 이미 존재하는지 확인하지 않고 생성 시도
            // 이미 존재하면 무시됨
            redisStreamsTemplate.opsForStream()
                    .createGroup(streamKey, groupName);
            log.info("Created consumer group '{}' for stream: {}", groupName, streamKey);

        } catch (Exception e) {
            // Consumer Group이 이미 존재하는 경우는 정상
            if (e.getMessage() != null && !e.getMessage().contains("BUSYGROUP")) {
                log.error("Failed to create consumer group '{}' for stream {}: {}",
                         groupName, streamKey, e.getMessage());
            }
        }
    }

    @Override
    public void startStreamListener(Long roomId) {
        String streamKey = getStreamKey(roomId);
        if (activeListeners.containsKey(roomId)) {
            log.debug("Stream listener for room {} is already active.", roomId);
            return;
        }

        try {
            Subscription subscription = listenerContainer.receive(
                    StreamOffset.latest(streamKey),
                    record -> {
                        log.debug("Received stream message for room {}: {}", roomId, record.getId().getValue());
                        streamMessageProcessor.processMessage(record.getValue(), record.getId().getValue());
                    }
            );
            activeListeners.put(roomId, subscription);
            log.info("Started individual stream listener for room: {}", roomId);
        } catch (Exception e) {
            log.error("Failed to start stream listener for room {}: {}", roomId, e.getMessage(), e);
        }
    }

    @Override
    public void stopStreamListener(Long roomId) {
        Subscription subscription = activeListeners.remove(roomId);
        if (subscription != null) {
            subscription.cancel();
            log.info("Stopped stream listener for room: {}", roomId);
        }
    }

    @Override
    public List<ObjectRecord<String, StreamMessageDto>> getPendingMessages(Long roomId, String consumerGroup, String consumerName) {
        String streamKey = getStreamKey(roomId);
        try {
            // 메시지 수를 미리 확인하는 대신 바로 read를 시도
            // 읽을 메시지가 없으면 read 메소드는 예외 없이 빈 리스트를 반환하므로 이 코드는 안전
            List<ObjectRecord<String, StreamMessageDto>> records = redisStreamsTemplate.opsForStream()
                    .read(StreamMessageDto.class, Consumer.from(consumerGroup, consumerName),
                          StreamOffset.create(streamKey, ReadOffset.from("0")));

            if (records != null && !records.isEmpty()) {
                log.info("Retrieved {} pending messages for consumer {} in group {}",
                        records.size(), consumerName, consumerGroup);
                return records;
            }
        } catch (Exception e) {
            // 스트림이 아직 존재하지 않는 등의 예외는 오류가 아닐 수 있으므로 WARN 레벨로 로깅합니다.
            log.warn("Could not get pending messages for room {}: {}", roomId, e.getMessage());
        }
        return List.of();
    }

    /**
     * Room ID로부터 Redis Stream Key 생성
     */
    private String getStreamKey(Long roomId) {
        return "room:" + roomId + ":messages";
    }

    /**
     * Stream 존재 여부 확인
     */
    private boolean streamExists(String streamKey) {
        try {
            StreamInfo.XInfoStream info = redisStreamsTemplate.opsForStream().info(streamKey);
            return info != null;
        } catch (Exception e) {
            return false;
        }
    }
}